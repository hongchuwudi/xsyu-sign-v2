# Environment Switch Script (Monorepo)
# Usage: .\switch-env.ps1 [dev|test|prod|prods]
# If no parameter provided, interactive mode will be used
# 作用：
#   1. 切换后端 xsyu-sign-server 的 application.yml active profile
#   2. 切换旧前端(static/js)的 API_BASE（过渡期）
#   3. 写入新前端 xsyu-sign-web/.env.production 的 VITE_API_BASE

param(
    [string]$env = ""
)

# Monorepo paths
$root = $PSScriptRoot
$envFile = "$root\.env"
$applicationYml = "$root\xsyu-sign-server\src\main\resources\application.yml"
$apiJs = "$root\xsyu-sign-server\src\main\resources\static\js\utils\api.js"
$webEnv = "$root\xsyu-sign-web\.env.production"

# Check if .env file exists
if (-not (Test-Path $envFile)) {
    Write-Host "Error: .env file not found at $envFile" -ForegroundColor Red
    exit 1
}

# Load configuration from .env file
$config = @{}
Get-Content $envFile | ForEach-Object {
    if ($_ -match "^(\w+)=(.*)$") {
        $key = $matches[1]
        $value = $matches[2]
        $config[$key] = $value
    }
}

# If no parameter provided, show interactive menu
if (-not $env) {
    Write-Host "==========================================" -ForegroundColor Cyan
    Write-Host "    Select Environment to Switch" -ForegroundColor Cyan
    Write-Host "==========================================" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "  [1] dev   - Development (localhost)" -ForegroundColor Green
    Write-Host "  [2] test  - Test (49.232.16.204)" -ForegroundColor Yellow
    Write-Host "  [3] prod  - Production (http)" -ForegroundColor Magenta
    Write-Host "  [4] prods - Production (https)" -ForegroundColor Blue
    Write-Host ""
    $choice = Read-Host "Enter your choice (1-4)"

    switch ($choice) {
        "1" { $env = "dev" }
        "2" { $env = "test" }
        "3" { $env = "prod" }
        "4" { $env = "prods" }
        default {
            Write-Host "Invalid choice. Exiting..." -ForegroundColor Red
            exit 1
        }
    }
}

# Validate parameter
if ($env -ne "dev" -and $env -ne "test" -and $env -ne "prod" -and $env -ne "prods") {
    Write-Host "Error: Environment must be 'dev', 'test', 'prod', or 'prods'" -ForegroundColor Red
    exit 1
}

# Get configuration for selected environment
$envUpper = $env.ToUpper()
$backendActive = $config["${envUpper}_BACKEND_ACTIVE"]
$apiBase = $config["${envUpper}_API_BASE"]

if (-not $backendActive -or -not $apiBase) {
    Write-Host "Error: Configuration not found for environment '$env' in .env file" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "Switching to [$env] environment..." -ForegroundColor Cyan
Write-Host "Backend active: $backendActive" -ForegroundColor Cyan
Write-Host "API base: $apiBase" -ForegroundColor Cyan
Write-Host ""

# ==================== Modify application.yml ====================
if (Test-Path $applicationYml) {
    $content = Get-Content $applicationYml -Raw -Encoding UTF8

    # Set the correct active profile (match 'active:' at the beginning of a line with optional whitespace)
    $content = $content -replace "(?m)^([ \t]*)active:.*$", "`$1active: $backendActive"

    # Save file
    $content | Set-Content $applicationYml -Encoding UTF8 -NoNewline
    Write-Host "  [OK] application.yml updated" -ForegroundColor Green
} else {
    Write-Host "  [FAIL] File not found: $applicationYml" -ForegroundColor Red
}

# ==================== Modify old frontend api.js (transition period) ====================
if (Test-Path $apiJs) {
    $content = Get-Content $apiJs -Raw -Encoding UTF8

    # Delete any existing const API_BASE line and add the new one at the beginning
    $lines = $content -split "`n"
    $newLines = @()
    $apiBaseAdded = $false

    foreach ($line in $lines) {
        # Skip any existing const API_BASE line (commented or not)
        if ($line -match "^\s*(//\s*)?const API_BASE\s*=") {
            # Skip this line - we'll add the new one at the beginning
            continue
        }
        $newLines += $line
    }

    # Add the new API_BASE at the beginning (after any comments at the top)
    $finalLines = @()
    $added = $false
    foreach ($line in $newLines) {
        if (-not $added -and -not ($line -match "^\s*//") -and $line -match "\S") {
            # Add API_BASE before the first non-comment, non-empty line
            $finalLines += "const API_BASE = '$apiBase'"
            $added = $true
        }
        $finalLines += $line
    }

    # If not added yet (file might be empty or all comments), add at the end
    if (-not $added) {
        $finalLines = @("const API_BASE = '$apiBase'") + $finalLines
    }

    $content = $finalLines -join "`n"

    # Save file
    $content | Set-Content $apiJs -Encoding UTF8 -NoNewline
    Write-Host "  [OK] old frontend api.js updated" -ForegroundColor Green
} else {
    Write-Host "  [SKIP] old frontend api.js not found (已退役可忽略)" -ForegroundColor DarkGray
}

# ==================== Write new frontend .env.production ====================
if (Test-Path "$root\xsyu-sign-web") {
    "VITE_API_BASE=$apiBase" | Set-Content $webEnv -Encoding UTF8 -NoNewline
    Write-Host "  [OK] xsyu-sign-web/.env.production updated" -ForegroundColor Green
} else {
    Write-Host "  [FAIL] xsyu-sign-web not found" -ForegroundColor Red
}

Write-Host ""
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host "Environment switch completed!" -ForegroundColor Green
Write-Host "Current environment: [$env]" -ForegroundColor Yellow
Write-Host "==========================================" -ForegroundColor Cyan
Write-Host ""

# Show current configuration
Write-Host "Current Configuration:" -ForegroundColor Yellow
Write-Host "------------------------------------------"

# Show active profile from application.yml
$appContent = Get-Content $applicationYml -Raw
if ($appContent -match "(?m)^[ \t]*active:\s*(\w+)") {
    $currentProfile = $matches[1]
    $color = if ($currentProfile -eq $backendActive) { "Green" } else { "Red" }
    Write-Host "  application.yml : active = $currentProfile" -ForegroundColor $color
}

# Show API_BASE from old api.js
if (Test-Path $apiJs) {
    $apiContent = Get-Content $apiJs -Raw
    if ($apiContent -match "(?m)^\s*const API_BASE\s*=\s*'([^']+)'") {
        $currentApi = $matches[1]
        $color = if ($currentApi -eq $apiBase) { "Green" } else { "Red" }
        Write-Host "  old api.js      : API_BASE = $currentApi" -ForegroundColor $color
    }
}

# Show VITE_API_BASE from new frontend .env.production
if (Test-Path $webEnv) {
    $webContent = Get-Content $webEnv -Raw
    if ($webContent -match "VITE_API_BASE=(\S+)") {
        $currentWebApi = $matches[1]
        $color = if ($currentWebApi -eq $apiBase) { "Green" } else { "Red" }
        Write-Host "  web .env.prod   : VITE_API_BASE = $currentWebApi" -ForegroundColor $color
    }
}

Write-Host "------------------------------------------"
Write-Host ""
Write-Host "Note: Please rebuild and redeploy to apply changes" -ForegroundColor Yellow
Write-Host ""
