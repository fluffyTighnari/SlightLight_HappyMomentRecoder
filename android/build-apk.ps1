# Builds the release APK and copies it to android/dist/ with a friendly name.
# Usage:  .\build-apk.ps1

$root = $PSScriptRoot
Push-Location $root
try {
    .\gradlew.bat assembleRelease
    if ($LASTEXITCODE -ne 0) { throw "Gradle build failed (exit $LASTEXITCODE)" }

    $gradleFile = Join-Path $root 'app\build.gradle.kts'
    $version = (Select-String -Path $gradleFile -Pattern 'versionName\s*=\s*"([^"]+)"').Matches[0].Groups[1].Value

    $apk = Join-Path $root 'app\build\outputs\apk\release\app-release.apk'
    if (-not (Test-Path $apk)) { throw "APK not found: $apk" }

    $dist = Join-Path $root 'dist'
    New-Item -ItemType Directory -Force -Path $dist | Out-Null
    $target = Join-Path $dist "weiguang-$version.apk"
    Copy-Item $apk $target -Force

    $size = [math]::Round((Get-Item $target).Length / 1KB, 1)
    Write-Host ""
    Write-Host "APK: $target ($size KB)" -ForegroundColor Green
}
finally {
    Pop-Location
}
