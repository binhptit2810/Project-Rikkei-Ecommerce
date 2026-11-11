# =============================================================================
# scripts/backup-db.ps1
# PowerShell script tự động sao lưu Cơ sở dữ liệu MySQL dự phòng cho ngày bảo vệ
#
# Người phụ trách: Đặng Thanh Bình (DTB)
# Người phối hợp: Cao Chí Thiện (CCT)
# Tiêu chí nghiệm thu (DoD): Script sao lưu và khôi phục hoạt động ổn định
#
# Cách sử dụng:
#   .\scripts\backup-db.ps1              # Sao lưu thông thường có nén gzip
#   .\scripts\backup-db.ps1 -NoCompress  # Sao lưu file .sql thuần không nén
#   .\scripts\backup-db.ps1 -Defense     # Sao lưu mốc chuẩn bảo vệ (không bị xóa định kỳ)
#   .\scripts\backup-db.ps1 -RetentionDays 14 # Giữ lại các bản trong 14 ngày gần nhất
# =============================================================================

[CmdletBinding()]
param(
    [switch]$NoCompress,
    [switch]$Defense,
    [int]$RetentionDays = 7
)

$ErrorActionPreference = "Stop"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ProjectRoot = Split-Path -Parent $ScriptDir
$BackupDir = Join-Path $ProjectRoot "database\backup"

if (-not (Test-Path $BackupDir)) {
    New-Item -ItemType Directory -Path $BackupDir -Force | Out-Null
}

# Cấu hình mặc định
$DbName = "ecommerce_db"
$DbUser = "ecommerce_user"
$DbPass = "password"
$DbHost = "127.0.0.1"
$DbPort = "3306"
$DbRootPass = "rootpassword"
$ContainerName = "ecommerce-mysql"

# Đọc cấu hình từ file .env nếu có
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

$Timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
if ($Defense) {
    $BaseFilename = "ecommerce_db_DEFENSE_BACKUP_$Timestamp"
    $TagNote = "[BẢN SAO LƯU CHUẨN CHO NGÀY BẢO VỆ]"
} else {
    $BaseFilename = "ecommerce_db_backup_$Timestamp"
    $TagNote = "[BẢN SAO LƯU ĐỊNH KỲ]"
}

$SqlFile = Join-Path $BackupDir "$BaseFilename.sql"
$FinalFile = $SqlFile

Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "       HỆ THỐNG SAO LƯU CƠ SỞ DỮ LIỆU TỰ ĐỘNG - E-COMMERCE       " -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "Người phụ trách: Đặng Thanh Bình | Phối hợp: Cao Chí Thiện" -ForegroundColor Yellow
Write-Host "Thời điểm bắt đầu : $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
Write-Host "Loại sao lưu      : $TagNote" -ForegroundColor Green
Write-Host "Cơ sở dữ liệu     : $DbName" -ForegroundColor Blue
Write-Host "Thư mục lưu trữ   : $BackupDir"

# Kiểm tra Docker container hoặc mysqldump local
$UseDocker = $false
$DockerRunning = $false
try {
    $dockerCheck = docker ps --filter "name=$ContainerName" --format "{{.Names}}" 2>$null
    if ($dockerCheck -match $ContainerName) {
        $UseDocker = $true
        $DockerRunning = $true
    }
} catch {
    $DockerRunning = $false
}

if ($UseDocker) {
    Write-Host "Phương thức       : Docker Container ($ContainerName)" -ForegroundColor Green
} elseif (Get-Command mysqldump -ErrorAction SilentlyContinue) {
    Write-Host "Phương thức       : Local mysqldump binary (${DbHost}:${DbPort})" -ForegroundColor Yellow
} else {
    Write-Host "[LỖI] Không tìm thấy Docker container '$ContainerName' đang chạy và máy tính chưa cài mysqldump." -ForegroundColor Red
    Write-Host "Vui lòng chạy 'docker compose up -d mysql' trước khi sao lưu." -ForegroundColor Yellow
    exit 1
}

$Stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
Write-Host "`n>>> Đang trích xuất dữ liệu CSDL (mysqldump)..." -ForegroundColor Blue

$AuthUser = if ($DbRootPass) { "root" } else { $DbUser }
$AuthPass = if ($DbRootPass) { $DbRootPass } else { $DbPass }

if ($UseDocker) {
    # Chạy mysqldump bên trong Docker container và xuất ra file
    $cmd = "docker exec $ContainerName mysqldump -u$AuthUser -p$AuthPass --single-transaction --quick --routines --triggers --events --default-character-set=utf8mb4 --set-gtid-purged=OFF $DbName"
    cmd /c "$cmd > `"$SqlFile`""
} else {
    $cmd = "mysqldump -h$DbHost -P$DbPort -u$DbUser -p$DbPass --single-transaction --quick --routines --triggers --events --default-character-set=utf8mb4 --set-gtid-purged=OFF $DbName"
    cmd /c "$cmd > `"$SqlFile`""
}

if (-not (Test-Path $SqlFile) -or (Get-Item $SqlFile).Length -eq 0) {
    Write-Host "[LỖI] Quá trình sao lưu thất bại, file rỗng: $SqlFile" -ForegroundColor Red
    if (Test-Path $SqlFile) { Remove-Item $SqlFile -Force }
    exit 1
}

$RawSize = "{0:N2} MB" -f ((Get-Item $SqlFile).Length / 1MB)

# Nén file với .NET GZipStream (chuẩn gzip đa nền tảng)
if (-not $NoCompress) {
    Write-Host ">>> Đang nén file với GZip..." -ForegroundColor Blue
    $GzFile = "$SqlFile.gz"
    
    $InputStream = [System.IO.File]::OpenRead($SqlFile)
    $OutputStream = [System.IO.File]::Create($GzFile)
    $GzipStream = New-Object System.IO.Compression.GZipStream($OutputStream, [System.IO.Compression.CompressionMode]::Compress)
    
    $InputStream.CopyTo($GzipStream)
    
    $GzipStream.Close()
    $OutputStream.Close()
    $InputStream.Close()
    
    Remove-Item $SqlFile -Force
    $FinalFile = $GzFile
}

$Stopwatch.Stop()
$Duration = [math]::Round($Stopwatch.Elapsed.TotalSeconds, 2)
$FinalSize = "{0:N2} MB" -f ((Get-Item $FinalFile).Length / 1MB)

Write-Host "`n================================================================" -ForegroundColor Green
Write-Host "[THÀNH CÔNG] ĐÃ HOÀN TẤT SAO LƯU CƠ SỞ DỮ LIỆU!" -ForegroundColor Green
Write-Host "File sao lưu      : $FinalFile" -ForegroundColor Cyan
Write-Host "Dung lượng        : $FinalSize (Dung lượng thô ban đầu: $RawSize)"
Write-Host "Thời gian thực thi: $Duration giây"
Write-Host "================================================================" -ForegroundColor Green

# Dọn dẹp retention cho các bản backup thường
if (-not $Defense -and $RetentionDays -gt 0) {
    Write-Host "`n>>> Đang kiểm tra và dọn dẹp các bản backup cũ hơn $RetentionDays ngày..." -ForegroundColor Blue
    $CutoffDate = (Get-Date).AddDays(-$RetentionDays)
    $OldFiles = Get-ChildItem -Path $BackupDir -File | Where-Object {
        $_.Name -like "ecommerce_db_backup_*" -and $_.LastWriteTime -lt $CutoffDate
    }
    
    $DeletedCount = 0
    foreach ($f in $OldFiles) {
        Remove-Item $f.FullName -Force
        Write-Host "  - Đã xóa bản cũ: $($f.Name)" -ForegroundColor DarkGray
        $DeletedCount++
    }
    
    if ($DeletedCount -eq 0) {
        Write-Host "  Không có bản backup cũ nào vượt quá $RetentionDays ngày."
    } else {
        Write-Host "  Đã xóa $DeletedCount bản sao lưu cũ."
    }
}

Write-Host "`nSẵn sàng phục vụ bảo vệ đồ án! Hãy giữ file backup này an toàn." -ForegroundColor Green
