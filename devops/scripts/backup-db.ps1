# =============================================================================
# backup-db.ps1 — Backup MySQL database (Windows PowerShell)
# Cách dùng: .\backup-db.ps1
# Yêu cầu: MySQL client (mysqldump) phải có trong PATH
# =============================================================================

param(
    [string]$ContainerName = "ecommerce-mysql",
    [string]$Database = "ecommerce_db",
    [string]$BackupDir = "..\..\database\backup"
)

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$backupFile = "$BackupDir\backup_${Database}_${timestamp}.sql"

# Tạo thư mục backup nếu chưa có
New-Item -ItemType Directory -Force -Path $BackupDir | Out-Null

Write-Host "Đang backup database '$Database' từ container '$ContainerName'..."
Write-Host "File output: $backupFile"

# Đọc password từ biến môi trường hoặc .env
$envFile = "..\..\..env"
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        if ($_ -match "^MYSQL_ROOT_PASSWORD=(.+)$") {
            $env:MYSQL_ROOT_PASSWORD = $matches[1]
        }
    }
}

$rootPwd = $env:MYSQL_ROOT_PASSWORD
if (-not $rootPwd) {
    Write-Error "Biến môi trường MYSQL_ROOT_PASSWORD chưa được thiết lập."
    exit 1
}

# Thực thi mysqldump trong container
docker exec $ContainerName mysqldump -u root -p"$rootPwd" $Database | Out-File -Encoding UTF8 $backupFile

if ($LASTEXITCODE -eq 0) {
    $size = (Get-Item $backupFile).Length / 1KB
    Write-Host "✅ Backup thành công! File: $backupFile ($([math]::Round($size, 1)) KB)"
} else {
    Write-Error "❌ Backup thất bại!"
    exit 1
}
