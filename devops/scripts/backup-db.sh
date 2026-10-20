#!/bin/bash
# =============================================================================
# backup-db.sh — Backup MySQL database (Linux/macOS)
# Cách dùng: chmod +x backup-db.sh && ./backup-db.sh
# =============================================================================

CONTAINER_NAME="${CONTAINER_NAME:-ecommerce-mysql}"
DATABASE="${MYSQL_DATABASE:-ecommerce_db}"
BACKUP_DIR="../../database/backup"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="${BACKUP_DIR}/backup_${DATABASE}_${TIMESTAMP}.sql"

mkdir -p "$BACKUP_DIR"

# Đọc password từ biến môi trường
ROOT_PWD="${MYSQL_ROOT_PASSWORD}"
if [ -z "$ROOT_PWD" ]; then
    echo "❌ Lỗi: Biến môi trường MYSQL_ROOT_PASSWORD chưa được thiết lập."
    echo "   Chạy: export MYSQL_ROOT_PASSWORD=your_password"
    exit 1
fi

echo "Đang backup database '$DATABASE' từ container '$CONTAINER_NAME'..."
docker exec "$CONTAINER_NAME" mysqldump -u root -p"$ROOT_PWD" "$DATABASE" > "$BACKUP_FILE"

if [ $? -eq 0 ]; then
    SIZE=$(du -sh "$BACKUP_FILE" | cut -f1)
    echo "✅ Backup thành công! File: $BACKUP_FILE ($SIZE)"
else
    echo "❌ Backup thất bại!"
    rm -f "$BACKUP_FILE"
    exit 1
fi
