# =============================================================================
# restore-db.ps1 — Restore MySQL database từ file backup (Windows PowerShell)
# Cách dùng: .\restore-db.ps1 -BackupFile ..\..\database\backup\backup_file.sql
# =============================================================================

param(
    [Parameter(Mandatory=$true)]
    [string]$BackupFile,
    [string]$ContainerName = "ecommerce-mysql",
    [string]$Database = "ecommerce_db"
)

if (-not (Test-Path $BackupFile)) {
    Write-Error "❌ Lỗi: File không tồn tại: $BackupFile"
    exit 1
}

# Đọc password từ .env nếu chưa có trong môi trường
$envFile = "$PSScriptRoot\..\..\.env"
if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        if ($_ -match "^MYSQL_ROOT_PASSWORD=(.+)$") {
            $env:MYSQL_ROOT_PASSWORD = $matches[1].Trim()
        }
        if ($_ -match "^MYSQL_DATABASE=(.+)$") {
            $Database = $matches[1].Trim()
        }
    }
}

$rootPwd = $env:MYSQL_ROOT_PASSWORD
if (-not $rootPwd) {
    Write-Error "❌ Lỗi: Biến môi trường MYSQL_ROOT_PASSWORD chưa được thiết lập."
    exit 1
}

Write-Warning "CẢNH BÁO: Thao tác này sẽ ghi đè dữ liệu trong database '$Database'!"
Write-Host "File restore: $BackupFile"

$confirmation = Read-Host "Bạn có chắc chắn muốn tiếp tục? (yes/no)"
if ($confirmation -ne "yes") {
    Write-Host "Thao tác restore đã bị hủy."
    exit 0
}

Write-Host "Đang restore database '$Database' từ file '$BackupFile'..."
Get-Content -Path $BackupFile -Encoding UTF8 | docker exec -i $ContainerName mysql -u root -p"$rootPwd" $Database

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Restore thành công!" -ForegroundColor Green
} else {
    Write-Error "❌ Restore thất bại!"
    exit 1
}
