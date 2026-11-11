# =============================================================================
# scripts/restore-db.ps1
# PowerShell script khôi phục Cơ sở dữ liệu MySQL từ bản sao lưu (Dự phòng ngày bảo vệ)
#
# Người phụ trách: Đặng Thanh Bình (DTB)
# Người phối hợp: Cao Chí Thiện (CCT)
# Tiêu chí nghiệm thu (DoD): Script sao lưu và khôi phục hoạt động ổn định
#
# Cách sử dụng:
#   .\scripts\restore-db.ps1                          # Khôi phục từ bản backup mới nhất
#   .\scripts\restore-db.ps1 -BackupFile <file_path>  # Khôi phục từ file chỉ định
#   .\scripts\restore-db.ps1 -Force                   # Bỏ qua xác nhận (non-interactive)
# =============================================================================

[CmdletBinding()]
param(
    [string]$BackupFile,
    [switch]$Force,
    [switch]$Yes
)

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir
$BackupDir = Join-Path $ProjectRoot "database\backup"

$DbName = "ecommerce_db"
$DbUser = "ecommerce_user"
$DbPass = "password"
$DbHost = "127.0.0.1"
$DbPort = "3306"
$DbRootPass = "rootpassword"
$ContainerName = "ecommerce-mysql"

$EnvFile = Join-Path $ProjectRoot ".env"
if (Test-Path $EnvFile) {
    Get-Content $EnvFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -and -not $line.StartsWith("#")) {
            $parts = $line -split "=", 2
            if ($parts.Length -eq 2) {
                $k = $parts[0].Trim()
                $v = $parts[1].Trim().Trim('"').Trim("'")
                switch ($k) {
                    "MYSQL_DATABASE" { $DbName = $v }
                    "MYSQL_USER"     { $DbUser = $v }
                    "MYSQL_PASSWORD" { $DbPass = $v }
                    "MYSQL_PORT"     { $DbPort = $v }
                    "MYSQL_ROOT_PASSWORD" { $DbRootPass = $v }
                }
            }
        }
    }
}

# Tự động chọn file backup mới nhất nếu không chỉ định
if (-not $BackupFile) {
    $latest = Get-ChildItem -Path $BackupDir -File | Where-Object { $_.Name -like "*.sql*" } | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if ($latest) {
        $BackupFile = $latest.FullName
    }
}

if (-not $BackupFile -or -not (Test-Path $BackupFile)) {
    Write-Host "[LỖI] Không tìm thấy file sao lưu hợp lệ để khôi phục: $BackupFile" -ForegroundColor Red
    Write-Host "Thư mục kiểm tra: $BackupDir"
    exit 1
}

$FileSize = "{0:N2} MB" -f ((Get-Item $BackupFile).Length / 1MB)

Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "     HỆ THỐNG KHÔI PHỤC CƠ SỞ DỮ LIỆU DỰ PHÒNG - E-COMMERCE     " -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "Người phụ trách: Đặng Thanh Bình | Phối hợp: Cao Chí Thiện" -ForegroundColor Yellow
Write-Host "Cơ sở dữ liệu đích: $DbName" -ForegroundColor Blue
Write-Host "File khôi phục    : $BackupFile ($FileSize)" -ForegroundColor Cyan

# Kiểm tra Docker container hoặc local mysql CLI
$UseDocker = $false
try {
    $dockerCheck = docker ps --filter "name=$ContainerName" --format "{{.Names}}" 2>$null
    if ($dockerCheck -match $ContainerName) {
        $UseDocker = $true
    }
} catch {
    $UseDocker = $false
}

if ($UseDocker) {
    Write-Host "Phương thức       : Docker Container ($ContainerName)" -ForegroundColor Green
} elseif (Get-Command mysql -ErrorAction SilentlyContinue) {
    Write-Host "Phương thức       : Local mysql binary (${DbHost}:${DbPort})" -ForegroundColor Yellow
} else {
    Write-Host "[LỖI] Không tìm thấy Docker container '$ContainerName' đang chạy và máy chưa cài mysql CLI." -ForegroundColor Red
    Write-Host "Vui lòng chạy 'docker compose up -d mysql' trước khi khôi phục." -ForegroundColor Yellow
    exit 1
}

# Xác nhận trước khi ghi đè dữ liệu
$SkipPrompt = $Force -or $Yes
if (-not $SkipPrompt) {
    Write-Host "`n========================== CẢNH BÁO ============================" -ForegroundColor Red
    Write-Host "Thao tác khôi phục sẽ GHI ĐÈ toàn bộ dữ liệu hiện tại trong '$DbName'." -ForegroundColor Yellow
    Write-Host "Đây là quy trình khôi phục dữ liệu dự phòng cho ngày bảo vệ đồ án." -ForegroundColor Yellow
    Write-Host "================================================================" -ForegroundColor Red
    
    $confirm = Read-Host "Bạn có chắc chắn muốn khôi phục không? (Nhập 'YES' để tiếp tục)"
    if ($confirm -ne "YES") {
        Write-Host "Đã hủy thao tác khôi phục CSDL." -ForegroundColor Yellow
        exit 0
    }
}

$Stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
Write-Host "`n>>> Đang chuẩn bị dữ liệu và nạp vào CSDL $DbName..." -ForegroundColor Blue

$AuthUser = if ($DbRootPass) { "root" } else { $DbUser }
$AuthPass = if ($DbRootPass) { $DbRootPass } else { $DbPass }

# Nếu là file nén .gz, giải nén ra file tạm
$SqlRestoreTarget = $BackupFile
$IsTemp = $false
if ($BackupFile.EndsWith(".gz", [System.StringComparison]::OrdinalIgnoreCase)) {
    Write-Host ">>> Đang giải nén dữ liệu từ gzip..." -ForegroundColor Blue
    $TempSql = [System.IO.Path]::GetTempFileName() + ".sql"
    
    $InputStream = [System.IO.File]::OpenRead($BackupFile)
    $GzipStream = New-Object System.IO.Compression.GZipStream($InputStream, [System.IO.Compression.CompressionMode]::Decompress)
    $OutputStream = [System.IO.File]::Create($TempSql)
    
    $GzipStream.CopyTo($OutputStream)
    
    $OutputStream.Close()
    $GzipStream.Close()
    $InputStream.Close()
    
    $SqlRestoreTarget = $TempSql
    $IsTemp = $true
}

try {
    if ($UseDocker) {
        $cmd = "docker exec -i $ContainerName mysql -u$AuthUser -p$AuthPass --default-character-set=utf8mb4 $DbName"
        cmd /c "$cmd < `"$SqlRestoreTarget`""
    } else {
        $cmd = "mysql -h$DbHost -P$DbPort -u$DbUser -p$DbPass --default-character-set=utf8mb4 $DbName"
        cmd /c "$cmd < `"$SqlRestoreTarget`""
    }
} finally {
    if ($IsTemp -and (Test-Path $SqlRestoreTarget)) {
        Remove-Item $SqlRestoreTarget -Force
    }
}

$Stopwatch.Stop()
$Duration = [math]::Round($Stopwatch.Elapsed.TotalSeconds, 2)

Write-Host "`n================================================================" -ForegroundColor Green
Write-Host "[THÀNH CÔNG] ĐÃ KHÔI PHỤC DỮ LIỆU CSDL THÀNH CÔNG TRONG $Duration GIÂY!" -ForegroundColor Green
Write-Host "================================================================" -ForegroundColor Green

# Kiểm tra dữ liệu sau restore
Write-Host "`n>>> Kiểm tra tính toàn vẹn dữ liệu sau khi khôi phục:" -ForegroundColor Blue
$CheckSql = "SELECT 'products' AS tab, COUNT(*) AS total FROM products UNION ALL SELECT 'users', COUNT(*) FROM users UNION ALL SELECT 'orders', COUNT(*) FROM orders UNION ALL SELECT 'reviews', COUNT(*) FROM reviews UNION ALL SELECT 'payments', COUNT(*) FROM payments;"

if ($UseDocker) {
    docker exec -i $ContainerName mysql "-u$AuthUser" "-p$AuthPass" -e $CheckSql $DbName
} else {
    mysql "-h$DbHost" "-P$DbPort" "-u$DbUser" "-p$DbPass" -e $CheckSql $DbName
}

Write-Host "`nHệ thống đã sẵn sàng cho buổi bảo vệ đồ án!" -ForegroundColor Green
