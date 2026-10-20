#!/bin/bash
# =============================================================================
# restore-db.sh — Restore MySQL database từ file backup
# Cách dùng: ./restore-db.sh ../../database/backup/backup_ecommerce_db_20241015.sql
# =============================================================================

BACKUP_FILE="$1"
CONTAINER_NAME="${CONTAINER_NAME:-ecommerce-mysql}"
DATABASE="${MYSQL_DATABASE:-ecommerce_db}"

if [ -z "$BACKUP_FILE" ]; then
    echo "❌ Lỗi: Chưa chỉ định file backup."
    echo "   Cách dùng: $0 <đường_dẫn_file_backup.sql>"
    echo "   Ví dụ:     $0 ../../database/backup/backup_ecommerce_db_20241015.sql"
    exit 1
fi

if [ ! -f "$BACKUP_FILE" ]; then
    echo "❌ Lỗi: File không tồn tại: $BACKUP_FILE"
    exit 1
fi

ROOT_PWD="${MYSQL_ROOT_PASSWORD}"
if [ -z "$ROOT_PWD" ]; then
    echo "❌ Lỗi: Biến môi trường MYSQL_ROOT_PASSWORD chưa được thiết lập."
    exit 1
fi

echo "⚠️  CẢNH BÁO: Thao tác này sẽ ghi đè dữ liệu trong database '$DATABASE'!"
echo "   File restore: $BACKUP_FILE"
read -p "   Bạn có chắc chắn muốn tiếp tục? (yes/no): " confirm

if [ "$confirm" != "yes" ]; then
    echo "Đã hủy."
    exit 0
fi

echo "Đang restore database '$DATABASE'..."
cat "$BACKUP_FILE" | docker exec -i "$CONTAINER_NAME" mysql -u root -p"$ROOT_PWD" "$DATABASE"

if [ $? -eq 0 ]; then
    echo "✅ Restore thành công!"
else
    echo "❌ Restore thất bại!"
    exit 1
fi
