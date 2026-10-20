param([switch]$DeviceTests)
$ErrorActionPreference = 'Stop'
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    & .\gradlew.bat assembleDebug testDebugUnitTest lintDebug --console plain
    if ($LASTEXITCODE -ne 0) { throw '빌드 또는 정적 검증에 실패했습니다.' }
    if ($DeviceTests) {
        & .\gradlew.bat connectedDebugAndroidTest --console plain
        if ($LASTEXITCODE -ne 0) { throw '기기 테스트에 실패했습니다.' }
    }
} finally { Pop-Location }
