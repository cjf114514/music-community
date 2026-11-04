# ============================================================
#  run-local.ps1 —— 本地开发启动脚本
#
#  为什么需要它？
#    Spring Boot 默认**不读** .env 文件，只认真正的环境变量。
#    这个脚本把 .env 里的键值对注入当前 PowerShell 进程，然后启动后端。
#
#  用法（在项目根目录）：
#      .\run-local.ps1
#
#  注意：注入只对"当前进程"有效，脚本一退出环境变量就没了 ——
#        这正是我们想要的：密钥不会残留在系统里。
# ============================================================

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$envFile = Join-Path $root '.env'

if (-not (Test-Path $envFile)) {
    Write-Host "[错误] 找不到 .env" -ForegroundColor Red
    Write-Host "       请先执行: Copy-Item .env.example .env" -ForegroundColor Yellow
    Write-Host "       然后编辑 .env 填入真实值" -ForegroundColor Yellow
    exit 1
}

$loaded = @()
foreach ($raw in Get-Content $envFile -Encoding UTF8) {
    $line = $raw.Trim()
    # 跳过空行和注释
    if ([string]::IsNullOrEmpty($line) -or $line.StartsWith('#')) { continue }

    $idx = $line.IndexOf('=')
    if ($idx -le 0) { continue }

    $key = $line.Substring(0, $idx).Trim()
    $val = $line.Substring($idx + 1).Trim()

    if ([string]::IsNullOrEmpty($val)) {
        Write-Host "[警告] $key 的值为空，已跳过" -ForegroundColor Yellow
        continue
    }

    Set-Item -Path "env:$key" -Value $val
    $loaded += $key
}

Write-Host "已加载 $($loaded.Count) 个环境变量: $($loaded -join ', ')" -ForegroundColor Green
Write-Host "启动后端（Ctrl+C 停止）..." -ForegroundColor Cyan

Push-Location (Join-Path $root 'backend')
try {
    & mvn spring-boot:run
}
finally {
    Pop-Location
}
