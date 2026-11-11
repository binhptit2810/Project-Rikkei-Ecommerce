#!/usr/bin/env bash
# =============================================================================
# scripts/backup-db.sh
# Script tự động sao lưu Cơ sở dữ liệu MySQL dự phòng cho ngày bảo vệ đồ án
#
# Người phụ trách: Đặng Thanh Bình (DTB)
# Người phối hợp: Cao Chí Thiện (CCT)
# Tiêu chí nghiệm thu (DoD): Script sao lưu và khôi phục hoạt động ổn định
#
# Cách sử dụng:
#   ./scripts/backup-db.sh              # Sao lưu thông thường có nén gzip
#   ./scripts/backup-db.sh --no-compress# Sao lưu file .sql thuần không nén
#   ./scripts/backup-db.sh --defense    # Sao lưu mốc chuẩn bảo vệ (không bị xóa theo retention)
#   ./scripts/backup-db.sh --retention 14 # Giữ lại bản sao lưu 14 ngày gần nhất
# =============================================================================

set -eo pipefail

# Màu hiển thị terminal
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Thư mục gốc dự án
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
BACKUP_DIR="${PROJECT_ROOT}/database/backup"

# Mặc định cấu hình
DB_NAME="${MYSQL_DATABASE:-ecommerce_db}"
DB_USER="${MYSQL_USER:-ecommerce_user}"
DB_PASS="${MYSQL_PASSWORD:-password}"
DB_HOST="${MYSQL_HOST:-127.0.0.1}"
DB_PORT="${MYSQL_PORT:-3306}"
CONTAINER_NAME="ecommerce-mysql"
RETENTION_DAYS=7
COMPRESS=true
IS_DEFENSE=false

# Đọc cấu hình từ file .env nếu có
if [ -f "${PROJECT_ROOT}/.env" ]; then
    # Lấy các biến cấu hình từ .env mà không xuất biến rác
    while IFS='=' read -r key val || [ -n "$key" ]; do
        # Bỏ qua comment hoặc dòng rỗng
        [[ "$key" =~ ^#.*$ ]] && continue
        [[ -z "$key" ]] && continue
        # Bỏ ký tự carriage return nếu có (Windows CRLF)
        key=$(echo "$key" | tr -d '\r' | xargs)
        val=$(echo "$val" | tr -d '\r' | xargs)
        case "$key" in
            MYSQL_DATABASE) DB_NAME="$val" ;;
            MYSQL_USER) DB_USER="$val" ;;
            MYSQL_PASSWORD) DB_PASS="$val" ;;
            MYSQL_PORT) DB_PORT="$val" ;;
            MYSQL_ROOT_PASSWORD) DB_ROOT_PASS="$val" ;;
        esac
    done < "${PROJECT_ROOT}/.env"
fi

# Parse tham số dòng lệnh
while [[ $# -gt 0 ]]; do
    case "$1" in
        --no-compress)
            COMPRESS=false
            shift
            ;;
        --defense)
            IS_DEFENSE=true
            shift
            ;;
        --retention)
            RETENTION_DAYS="$2"
            shift 2
            ;;
        -h|--help)
            echo "Hướng dẫn sử dụng scripts/backup-db.sh:"
            echo "  --no-compress   Tạo file .sql thuần không nén gzip"
            echo "  --defense       Tạo bản sao lưu mốc chuẩn cho ngày bảo vệ (không bị xóa định kỳ)"
            echo "  --retention N   Số ngày lưu trữ các bản backup cũ (mặc định: 7)"
            echo "  -h, --help      Hiển thị trợ giúp này"
            exit 0
            ;;
        *)
            echo -e "${RED}[LỖI] Tham số không hợp lệ: $1${NC}"
            exit 1
            ;;
    esac
done

mkdir -p "${BACKUP_DIR}"

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
if [ "$IS_DEFENSE" = true ]; then
    BASE_FILENAME="ecommerce_db_DEFENSE_BACKUP_${TIMESTAMP}"
    TAG_NOTE="[BẢN SAO LƯU CHUẨN CHO NGÀY BẢO VỆ]"
else
    BASE_FILENAME="ecommerce_db_backup_${TIMESTAMP}"
    TAG_NOTE="[BẢN SAO LƯU ĐỊNH KỲ]"
fi

SQL_FILE="${BACKUP_DIR}/${BASE_FILENAME}.sql"
FINAL_FILE="${SQL_FILE}"

echo -e "${CYAN}================================================================${NC}"
echo -e "${CYAN}       HỆ THỐNG SAO LƯU CƠ SỞ DỮ LIỆU TỰ ĐỘNG - E-COMMERCE       ${NC}"
echo -e "${CYAN}================================================================${NC}"
echo -e "Người phụ trách: ${YELLOW}Đặng Thanh Bình${NC} | Phối hợp: ${YELLOW}Cao Chí Thiện${NC}"
echo -e "Thời điểm bắt đầu : $(date '+%Y-%m-%d %H:%M:%S')"
echo -e "Loại sao lưu      : ${GREEN}${TAG_NOTE}${NC}"
echo -e "Cơ sở dữ liệu     : ${BLUE}${DB_NAME}${NC}"
echo -e "Thư mục lưu trữ   : ${BACKUP_DIR}"

# Kiểm tra phương thức chạy: Docker Container hay Native mysqldump
USE_DOCKER=false
if command -v docker &> /dev/null && docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    USE_DOCKER=true
    echo -e "Phương thức       : ${GREEN}Docker Container (${CONTAINER_NAME})${NC}"
elif command -v mysqldump &> /dev/null; then
    USE_DOCKER=false
    echo -e "Phương thức       : ${YELLOW}Local mysqldump binary (${DB_HOST}:${DB_PORT})${NC}"
else
    echo -e "${RED}[LỖI] Không tìm thấy Docker container '${CONTAINER_NAME}' đang chạy và máy không có sẵn mysqldump.${NC}"
    echo -e "${YELLOW}Vui lòng chạy 'docker compose up -d mysql' trước khi thực hiện sao lưu.${NC}"
    exit 1
fi

START_TIME=$(date +%s)
echo -e "\n${BLUE}>>> Đang trích xuất dữ liệu CSDL (mysqldump)...${NC}"

# Tùy chọn mysqldump tối ưu nhất cho InnoDB (không khóa bảng, đầy đủ triggers, utf8mb4)
DUMP_OPTS="--single-transaction --quick --routines --triggers --events --default-character-set=utf8mb4 --set-gtid-purged=OFF"

if [ "$USE_DOCKER" = true ]; then
    # Dùng tài khoản root hoặc ecommerce_user trong container
    AUTH_USER="${DB_USER}"
    AUTH_PASS="${DB_PASS}"
    if [ -n "${DB_ROOT_PASS:-}" ]; then
        AUTH_USER="root"
        AUTH_PASS="${DB_ROOT_PASS}"
    fi

    docker exec "${CONTAINER_NAME}" mysqldump -u"${AUTH_USER}" -p"${AUTH_PASS}" ${DUMP_OPTS} "${DB_NAME}" > "${SQL_FILE}"
else
    mysqldump -h"${DB_HOST}" -P"${DB_PORT}" -u"${DB_USER}" -p"${DB_PASS}" ${DUMP_OPTS} "${DB_NAME}" > "${SQL_FILE}"
fi

# Kiểm tra file sinh ra
if [ ! -s "${SQL_FILE}" ]; then
    echo -e "${RED}[LỖI] Quá trình sao lưu thất bại, file rỗng: ${SQL_FILE}${NC}"
    rm -f "${SQL_FILE}"
    exit 1
fi

# Kiểm tra dòng kết thúc của mysqldump
if ! tail -n 10 "${SQL_FILE}" | grep -q "Dump completed"; then
    echo -e "${YELLOW}[CẢNH BÁO] File sao lưu có thể chưa hoàn tất trọn vẹn (thiếu 'Dump completed'). Vui lòng kiểm tra lại.${NC}"
fi

RAW_SIZE=$(du -h "${SQL_FILE}" | cut -f1)

# Nén file nếu được yêu cầu
if [ "$COMPRESS" = true ]; then
    if command -v gzip &> /dev/null; then
        echo -e "${BLUE}>>> Đang nén file với gzip...${NC}"
        gzip -f "${SQL_FILE}"
        FINAL_FILE="${SQL_FILE}.gz"
    else
        echo -e "${YELLOW}[CẢNH BÁO] Không tìm thấy công cụ gzip trên máy, giữ nguyên file .sql thô.${NC}"
    fi
fi

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))
FINAL_SIZE=$(du -h "${FINAL_FILE}" | cut -f1)

echo -e "\n${GREEN}================================================================${NC}"
echo -e "${GREEN}[THÀNH CÔNG] ĐÃ HOÀN TẤT SAO LƯU CƠ SỞ DỮ LIỆU!${NC}"
echo -e "File sao lưu      : ${CYAN}${FINAL_FILE}${NC}"
echo -e "Dung lượng        : ${FINAL_SIZE} (Dung lượng thô: ${RAW_SIZE})"
echo -e "Thời gian thực thi: ${DURATION} giây"
echo -e "${GREEN}================================================================${NC}"

# Dọn dẹp các bản backup cũ theo RETENTION_DAYS (trừ các bản DEFENSE_BACKUP)
if [ "$IS_DEFENSE" = false ] && [ "$RETENTION_DAYS" -gt 0 ]; then
    echo -e "\n${BLUE}>>> Đang dọn dẹp các bản sao lưu tự động cũ hơn ${RETENTION_DAYS} ngày...${NC}"
    DELETED_COUNT=0
    while IFS= read -r old_file; do
        if [ -n "$old_file" ]; then
            rm -f "$old_file"
            echo -e "  - Đã xóa bản cũ: $(basename "$old_file")"
            DELETED_COUNT=$((DELETED_COUNT + 1))
        fi
    done < <(find "${BACKUP_DIR}" -maxdepth 1 -name "ecommerce_db_backup_*" \( -name "*.sql" -o -name "*.sql.gz" \) -type f -mtime +"${RETENTION_DAYS}")

    if [ "$DELETED_COUNT" -eq 0 ]; then
        echo -e "  Không có bản sao lưu cũ nào vượt quá hạn ${RETENTION_DAYS} ngày."
    else
        echo -e "  Đã dọn dẹp ${DELETED_COUNT} file cũ."
    fi
fi

echo -e "\n${GREEN}Sẵn sàng phục vụ bảo vệ đồ án! Hãy giữ file này cẩn thận.${NC}"
exit 0
