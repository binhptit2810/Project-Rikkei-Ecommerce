#!/usr/bin/env bash
# =============================================================================
# scripts/restore-db.sh
# Script khôi phục Cơ sở dữ liệu MySQL từ bản sao lưu (Dự phòng khẩn cấp ngày bảo vệ)
#
# Người phụ trách: Đặng Thanh Bình (DTB)
# Người phối hợp: Cao Chí Thiện (CCT)
# Tiêu chí nghiệm thu (DoD): Script sao lưu và khôi phục hoạt động ổn định
#
# Cách sử dụng:
#   ./scripts/restore-db.sh                          # Khôi phục từ bản backup mới nhất
#   ./scripts/restore-db.sh <đường_dẫn_file_backup>  # Khôi phục từ file chỉ định
#   ./scripts/restore-db.sh --yes                    # Không cần gõ xác nhận (non-interactive)
# =============================================================================

set -eo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
BACKUP_DIR="${PROJECT_ROOT}/database/backup"

DB_NAME="${MYSQL_DATABASE:-ecommerce_db}"
DB_USER="${MYSQL_USER:-ecommerce_user}"
DB_PASS="${MYSQL_PASSWORD:-password}"
DB_HOST="${MYSQL_HOST:-127.0.0.1}"
DB_PORT="${MYSQL_PORT:-3306}"
CONTAINER_NAME="ecommerce-mysql"
AUTO_CONFIRM=false
TARGET_FILE=""

# Đọc .env nếu có
if [ -f "${PROJECT_ROOT}/.env" ]; then
    while IFS='=' read -r key val || [ -n "$key" ]; do
        [[ "$key" =~ ^#.*$ ]] && continue
        [[ -z "$key" ]] && continue
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

# Parse tham số
while [[ $# -gt 0 ]]; do
    case "$1" in
        -y|--yes)
            AUTO_CONFIRM=true
            shift
            ;;
        -h|--help)
            echo "Hướng dẫn sử dụng scripts/restore-db.sh:"
            echo "  ./scripts/restore-db.sh [file_path] [-y|--yes]"
            echo "  -y, --yes: Bỏ qua bước gõ xác nhận"
            exit 0
            ;;
        *)
            if [ -z "$TARGET_FILE" ]; then
                TARGET_FILE="$1"
            fi
            shift
            ;;
    esac
done

# Nếu không truyền file, tự động tìm file backup mới nhất trong database/backup
if [ -z "$TARGET_FILE" ]; then
    LATEST_BACKUP=$(find "${BACKUP_DIR}" -maxdepth 1 \( -name "*.sql" -o -name "*.sql.gz" \) -type f -printf '%T@ %p\n' 2>/dev/null | sort -nr | head -n 1 | cut -d' ' -f2-)
    if [ -z "$LATEST_BACKUP" ]; then
        # Thử lệnh ls tương thích macOS / BSD
        LATEST_BACKUP=$(ls -t "${BACKUP_DIR}"/*.sql* 2>/dev/null | head -n 1 || true)
    fi
    TARGET_FILE="${LATEST_BACKUP}"
fi

if [ -z "$TARGET_FILE" ] || [ ! -f "$TARGET_FILE" ]; then
    echo -e "${RED}[LỖI] Không tìm thấy file sao lưu hợp lệ để khôi phục!${NC}"
    echo -e "Thư mục kiểm tra: ${BACKUP_DIR}"
    exit 1
fi

FILE_SIZE=$(du -h "${TARGET_FILE}" | cut -f1)

echo -e "${CYAN}================================================================${NC}"
echo -e "${CYAN}     HỆ THỐNG KHÔI PHỤC CƠ SỞ DỮ LIỆU DỰ PHÒNG - E-COMMERCE     ${NC}"
echo -e "${CYAN}================================================================${NC}"
echo -e "Người phụ trách: ${YELLOW}Đặng Thanh Bình${NC} | Phối hợp: ${YELLOW}Cao Chí Thiện${NC}"
echo -e "Cơ sở dữ liệu đích: ${BLUE}${DB_NAME}${NC}"
echo -e "File khôi phục    : ${CYAN}${TARGET_FILE}${NC} (${FILE_SIZE})"

# Kiểm tra Docker container hoặc local mysql
USE_DOCKER=false
if command -v docker &> /dev/null && docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    USE_DOCKER=true
    echo -e "Phương thức       : ${GREEN}Docker Container (${CONTAINER_NAME})${NC}"
elif command -v mysql &> /dev/null; then
    USE_DOCKER=false
    echo -e "Phương thức       : ${YELLOW}Local mysql binary (${DB_HOST}:${DB_PORT})${NC}"
else
    echo -e "${RED}[LỖI] Không tìm thấy Docker container '${CONTAINER_NAME}' đang chạy và máy không có mysql CLI.${NC}"
    echo -e "${YELLOW}Vui lòng chạy 'docker compose up -d mysql' trước khi khôi phục.${NC}"
    exit 1
fi

# Cảnh báo và yêu cầu xác nhận
if [ "$AUTO_CONFIRM" = false ]; then
    echo -e "\n${RED}========================== CẢNH BÁO ============================${NC}"
    echo -e "${YELLOW}Thao tác khôi phục sẽ GHI ĐÈ toàn bộ dữ liệu hiện tại trong '${DB_NAME}'.${NC}"
    echo -e "${YELLOW}Đây là quy trình khôi phục dự phòng cho ngày bảo vệ đồ án.${NC}"
    echo -e "${RED}================================================================${NC}"
    read -p "Bạn có chắc chắn muốn khôi phục không? (Nhập 'YES' để tiếp tục): " CONFIRM_INPUT
    if [ "$CONFIRM_INPUT" != "YES" ]; then
        echo -e "${YELLOW}Đã hủy thao tác khôi phục CSDL.${NC}"
        exit 0
    fi
fi

START_TIME=$(date +%s)
echo -e "\n${BLUE}>>> Đang thực hiện khôi phục dữ liệu vào ${DB_NAME}...${NC}"

AUTH_USER="${DB_USER}"
AUTH_PASS="${DB_PASS}"
if [ -n "${DB_ROOT_PASS:-}" ]; then
    AUTH_USER="root"
    AUTH_PASS="${DB_ROOT_PASS}"
fi

# Xử lý luồng nén hoặc không nén
if [[ "$TARGET_FILE" == *.gz ]]; then
    if [ "$USE_DOCKER" = true ]; then
        gzip -dc "$TARGET_FILE" | docker exec -i "${CONTAINER_NAME}" mysql -u"${AUTH_USER}" -p"${AUTH_PASS}" --default-character-set=utf8mb4 "${DB_NAME}"
    else
        gzip -dc "$TARGET_FILE" | mysql -h"${DB_HOST}" -P"${DB_PORT}" -u"${DB_USER}" -p"${DB_PASS}" --default-character-set=utf8mb4 "${DB_NAME}"
    fi
else
    if [ "$USE_DOCKER" = true ]; then
        docker exec -i "${CONTAINER_NAME}" mysql -u"${AUTH_USER}" -p"${AUTH_PASS}" --default-character-set=utf8mb4 "${DB_NAME}" < "$TARGET_FILE"
    else
        mysql -h"${DB_HOST}" -P"${DB_PORT}" -u"${DB_USER}" -p"${DB_PASS}" --default-character-set=utf8mb4 "${DB_NAME}" < "$TARGET_FILE"
    fi
fi

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

echo -e "\n${GREEN}================================================================${NC}"
echo -e "${GREEN}[THÀNH CÔNG] ĐÃ KHÔI PHỤC DỮ LIỆU CSDL THÀNH CÔNG TRONG ${DURATION} GIÂY!${NC}"
echo -e "${GREEN}================================================================${NC}"

# Kiểm tra nhanh số lượng bản ghi sau khi restore
echo -e "\n${BLUE}>>> Kiểm tra tính toàn vẹn dữ liệu sau khi khôi phục:${NC}"
CHECK_SQL="SELECT 'products' AS tab, COUNT(*) AS total FROM products UNION ALL SELECT 'users', COUNT(*) FROM users UNION ALL SELECT 'orders', COUNT(*) FROM orders UNION ALL SELECT 'reviews', COUNT(*) FROM reviews UNION ALL SELECT 'payments', COUNT(*) FROM payments;"

if [ "$USE_DOCKER" = true ]; then
    docker exec -i "${CONTAINER_NAME}" mysql -u"${AUTH_USER}" -p"${AUTH_PASS}" -e "${CHECK_SQL}" "${DB_NAME}"
else
    mysql -h"${DB_HOST}" -P"${DB_PORT}" -u"${DB_USER}" -p"${DB_PASS}" -e "${CHECK_SQL}" "${DB_NAME}"
fi

echo -e "\n${GREEN}Hệ thống đã sẵn sàng cho buổi bảo vệ đồ án!${NC}"
exit 0
