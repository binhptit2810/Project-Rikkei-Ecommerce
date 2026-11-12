# =============================================================================
# scripts/benchmark-best-sellers.ps1
# Đo tốc độ query Best Seller thực tế trên MySQL để kiểm tra tiêu chí nghiệm thu DoD (< 30ms)
#
# Người phụ trách: Đặng Thanh Bình (DTB)
# DoD: API Best Seller trả về đúng thứ tự sản phẩm bán nhiều nhất trong tháng, tốc độ truy vấn < 30ms
# =============================================================================

[CmdletBinding()]
param(
    [int]$Month = (Get-Date).Month,
    [int]$Year = (Get-Date).Year
)

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir

$DbName = "ecommerce_db"
$DbUser = "ecommerce_user"
$DbPass = "ecommerce_pass_123"
$ContainerName = "ecommerce-mysql"

$EnvFile = Join-Path $ProjectRoot ".env"
if (Test-Path $EnvFile) {
    Get-Content $EnvFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#")) {
            $parts = $line -split "=", 2
            if ($parts.Length -eq 2) {
                switch ($parts[0].Trim()) {
                    "MYSQL_DATABASE" { $DbName = $parts[1].Trim() }
                    "MYSQL_USER"     { $DbUser = $parts[1].Trim() }
                    "MYSQL_PASSWORD" { $DbPass = $parts[1].Trim() }
                }
            }
        }
    }
}

Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "    BENCHMARK TỐC ĐỘ QUERY BEST SELLER THEO THÁNG (DoD < 30ms)   " -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "Tháng truy vấn: $Month / $Year" -ForegroundColor Yellow
Write-Host "Mục tiêu DoD  : Tốc độ query < 30ms & đúng thứ tự totalSold DESC`n" -ForegroundColor Yellow

$StartDate = "$Year-" + $Month.ToString("00") + "-01 00:00:00"
$NextMonthYear = if ($Month -eq 12) { $Year + 1 } else { $Year }
$NextMonth = if ($Month -eq 12) { 1 } else { $Month + 1 }
$EndDate = "$NextMonthYear-" + $NextMonth.ToString("00") + "-01 00:00:00"

$QuerySql = @"
SET profiling = 1;

-- 1. Query danh sách Best Seller chính (Native SQL có phân trang & sắp xếp totalSold DESC)
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
  AND o.created_at >= '$StartDate'
  AND o.created_at < '$EndDate'
GROUP BY p.id, c.name
ORDER BY totalSold DESC, p.id ASC
LIMIT 10;

-- 2. Query đếm tổng số sản phẩm bán được (countQuery)
SELECT COUNT(DISTINCT oi.product_id) AS total_count
FROM order_items oi
INNER JOIN orders o ON oi.order_id = o.id
WHERE o.order_status != 'CANCELLED'
  AND o.created_at >= '$StartDate'
  AND o.created_at < '$EndDate';

SHOW PROFILES;
"@

$rawOutput = docker exec -i $ContainerName mysql "-u$DbUser" "-p$DbPass" -e $QuerySql $DbName 2>$null

Write-Host "Kết quả thực thi từ MySQL Container:`n" -ForegroundColor Cyan
$lines = $rawOutput -split "`r?`n"
$profileStarted = $false
$allPassed = $true

foreach ($line in $lines) {
    if ($line -match "Query_ID\s+Duration\s+Query") {
        $profileStarted = $true
        Write-Host "`n--------------------------------------------------------------------------------" -ForegroundColor DarkGray
        Write-Host ("{0,-8} | {1,-15} | {2}" -f "QueryID", "Thời gian (ms)", "Nội dung câu lệnh") -ForegroundColor Yellow
        Write-Host ("-" * 80)
        continue
    }
    
    if ($profileStarted -and $line.Trim()) {
        $cols = $line -split "`t"
        if ($cols.Length -ge 3) {
            $qid = $cols[0].Trim()
            $durSec = [double]$cols[1].Trim()
            $durMs = [math]::Round($durSec * 1000, 3)
            $queryText = $cols[2].Trim()
            
            $status = if ($durMs -lt 30.0) { "[ĐẠT DoD < 30ms]" } else { "[KHÔNG ĐẠT]" }
            $color = if ($durMs -lt 30.0) { "Green" } else { "Red" }
            if ($durMs -ge 30.0) { $allPassed = $false }
            
            $shortQuery = $queryText.Substring(0, [math]::Min(50, $queryText.Length)) + "..."
            Write-Host ("{0,-8} | {1,-15} | {2,-55} {3}" -f $qid, "$durMs ms", $shortQuery, $status) -ForegroundColor $color
        }
    } elseif (-not $profileStarted -and $line.Trim()) {
        Write-Host $line
    }
}

Write-Host "`n================================================================" -ForegroundColor Cyan
if ($allPassed) {
    Write-Host "[KẾT LUẬN] TRUY VẤN BEST SELLER HOÀN TOÀN ĐẠT TIÊU CHÍ DoD (< 30ms)!" -ForegroundColor Green
} else {
    Write-Host "[KẾT LUẬN] Cần kiểm tra lại hiệu năng câu lệnh." -ForegroundColor Red
}
Write-Host "================================================================" -ForegroundColor Cyan
