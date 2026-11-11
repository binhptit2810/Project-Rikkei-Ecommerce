# =============================================================================
# scripts/benchmark-queries.ps1
# Đo tốc độ query CSDL thực tế để kiểm tra tiêu chí nghiệm thu DoD (< 50ms)
#
# Người phụ trách: Đặng Thanh Bình (DTB)
# Người phối hợp: Cao Chí Thiện (CCT)
# =============================================================================

[CmdletBinding()]
param()

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
Write-Host "   BENCHMARK TỐC ĐỘ TRUY VẤN CSDL (ĐÁNH INDEX THEO TIÊU CHÍ DoD)  " -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "Mục tiêu DoD: Tốc độ query < 50ms`n" -ForegroundColor Yellow

$BenchmarkSql = @"
SET profiling = 1;
SELECT id, order_code, coupon_code, total_amount FROM orders WHERE coupon_code = 'DISCOUNT10' LIMIT 10;
SELECT id, code, discount_type, discount_value FROM coupons WHERE code = 'DISCOUNT10';
SELECT id, product_id, rating, comment, created_at FROM reviews WHERE product_id = 1 ORDER BY created_at DESC LIMIT 10;
SELECT COUNT(*) AS total, AVG(rating) AS avg_rating FROM reviews WHERE product_id = 1;
SELECT id, order_id, transaction_code, vnp_txn_ref, amount, status FROM payments WHERE vnp_txn_ref = 'PAY-20261111-0001';
SHOW PROFILES;
"@

$output = docker exec -i $ContainerName mysql "-u$DbUser" "-p$DbPass" -e $BenchmarkSql $DbName 2>$null

Write-Host "Kết quả MySQL Profiling chi tiết:`n" -ForegroundColor Cyan
$lines = $output -split "`r?`n"
$profileStarted = $false
$allPassed = $true

foreach ($line in $lines) {
    if ($line -match "Query_ID\s+Duration\s+Query") {
        $profileStarted = $true
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
            
            $status = if ($durMs -lt 50.0) { "[ĐẠT DoD <50ms]" } else { "[KHÔNG ĐẠT]" }
            $color = if ($durMs -lt 50.0) { "Green" } else { "Red" }
            if ($durMs -ge 50.0) { $allPassed = $false }
            
            Write-Host ("{0,-8} | {1,-15} | {2} {3}" -f $qid, "$durMs ms", $queryText, $status) -ForegroundColor $color
        }
    }
}

Write-Host "`n================================================================" -ForegroundColor Cyan
if ($allPassed) {
    Write-Host "[KẾT LUẬN] 100% TRUY VẤN ĐỀU ĐẠT TIÊU CHÍ DoD (< 50ms) - HIỆU NĂNG VƯỢT TRỘI!" -ForegroundColor Green
} else {
    Write-Host "[KẾT LUẬN] Cần kiểm tra lại các query chưa đạt." -ForegroundColor Red
}
Write-Host "================================================================" -ForegroundColor Cyan
