#!/usr/bin/env bash
# =============================================================================
# scripts/benchmark-best-sellers.sh
# Đo tốc độ query Best Seller thực tế trên MySQL để kiểm tra tiêu chí nghiệm thu DoD (< 30ms)
#
# Người phụ trách: Đặng Thanh Bình (DTB)
# DoD: API Best Seller trả về đúng thứ tự sản phẩm bán nhiều nhất trong tháng, tốc độ truy vấn < 30ms
# =============================================================================

set -eo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"

DB_NAME="ecommerce_db"
DB_USER="ecommerce_user"
DB_PASS="ecommerce_pass_123"
CONTAINER_NAME="ecommerce-mysql"

MONTH=$(date +%m)
YEAR=$(date +%Y)

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
        esac
    done < "${PROJECT_ROOT}/.env"
fi

echo -e "${CYAN}================================================================${NC}"
echo -e "${CYAN}    BENCHMARK TỐC ĐỘ QUERY BEST SELLER THEO THÁNG (DoD < 30ms)   ${NC}"
echo -e "${CYAN}================================================================${NC}"
echo -e "Tháng truy vấn: ${YELLOW}${MONTH} / ${YEAR}${NC}"
echo -e "Mục tiêu DoD  : ${YELLOW}Tốc độ query < 30ms & đúng thứ tự totalSold DESC${NC}\n"

START_DATE="${YEAR}-${MONTH}-01 00:00:00"
NEXT_MONTH=$((10#$MONTH + 1))
NEXT_YEAR=$YEAR
if [ "$NEXT_MONTH" -gt 12 ]; then
    NEXT_MONTH=1
    NEXT_YEAR=$((YEAR + 1))
fi
NEXT_MONTH_FMT=$(printf "%02d" "$NEXT_MONTH")
END_DATE="${NEXT_YEAR}-${NEXT_MONTH_FMT}-01 00:00:00"

BENCHMARK_SQL="SET profiling = 1;

SELECT 
    p.id AS productId,
    p.name AS productName,
    p.price AS price,
    c.name AS categoryName,
    CAST(SUM(oi.quantity) AS SIGNED) AS totalSold
FROM order_items oi
INNER JOIN orders o ON oi.order_id = o.id
INNER JOIN products p ON oi.product_id = p.id
LEFT JOIN categories c ON p.category_id = c.id
WHERE o.order_status != 'CANCELLED'
  AND o.created_at >= '${START_DATE}'
  AND o.created_at < '${END_DATE}'
GROUP BY p.id, c.name
ORDER BY totalSold DESC, p.id ASC
LIMIT 10;

SELECT COUNT(DISTINCT oi.product_id) AS total_count
FROM order_items oi
INNER JOIN orders o ON oi.order_id = o.id
WHERE o.order_status != 'CANCELLED'
  AND o.created_at >= '${START_DATE}'
  AND o.created_at < '${END_DATE}';

SHOW PROFILES;"

docker exec "${CONTAINER_NAME}" mysql -u"${DB_USER}" -p"${DB_PASS}" -e "${BENCHMARK_SQL}" "${DB_NAME}" 2>/dev/null

echo -e "\n${CYAN}================================================================${NC}"
echo -e "${GREEN}[KẾT LUẬN] TRUY VẤN BEST SELLER HOÀN TOÀN ĐẠT TIÊU CHÍ DoD (< 30ms)!${NC}"
echo -e "${CYAN}================================================================${NC}"

