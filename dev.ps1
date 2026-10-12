# Dev helper for working in VS Code with the Android emulator.
#   .\dev.ps1 run       start the emulator if needed, build, install and open the app
#   .\dev.ps1 emulator  only start the emulator
#   .\dev.ps1 slips     copy images from .\samples into the emulator's gallery
#   .\dev.ps1 log       show the app's log (useful when it crashes)
#   .\dev.ps1 test      run the parser unit tests
#   .\dev.ps1 release   build TeenJod.apk for a real phone
param(
    [ValidateSet('run', 'emulator', 'slips', 'log', 'test', 'release')]
    [string]$Command = 'run'
)
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot

$env:JAVA_HOME = "$env:ProgramFiles\Android\Android Studio\jbr"
$sdk = "$env:LOCALAPPDATA\Android\Sdk"
$adb = "$sdk\platform-tools\adb.exe"
# First virtual device created in Android Studio's Device Manager.
$avd = (& "$sdk\emulator\emulator.exe" -list-avds | Where-Object { $_ -and $_ -notmatch '^(INFO|WARNING|ERROR)' } | Select-Object -First 1)
$pkg = 'com.tarit.slipledger'

function Get-EmulatorSerial {
    (& $adb devices) | ForEach-Object { if ($_ -match '^(emulator-\d+)\s+device$') { $Matches[1] } } | Select-Object -First 1
}

function Start-Emulator {
    $serial = Get-EmulatorSerial
    if (-not $serial) {
        if (-not $avd) { throw 'No emulator found. Create one in Android Studio: Device Manager > + > Create Virtual Device.' }
        Write-Host "Starting emulator $avd ..."
        Start-Process "$sdk\emulator\emulator.exe" -ArgumentList '-avd', $avd
        while (-not ($serial = Get-EmulatorSerial)) { Start-Sleep -Seconds 2 }
    }
    while ((& $adb -s $serial shell getprop sys.boot_completed).Trim() -ne '1') {
        Write-Host 'Waiting for Android to finish booting ...'
        Start-Sleep -Seconds 3
    }
    # Gradle and adb then target only the emulator, even if a phone is plugged in too.
    $env:ANDROID_SERIAL = $serial
    $serial
}

function Invoke-Gradle {
    .\gradlew.bat @args --console=plain
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

switch ($Command) {
    'emulator' { Start-Emulator | Out-Null }
    'run' {
        Start-Emulator | Out-Null
        Invoke-Gradle installDebug
        & $adb shell am start -n "$pkg/.MainActivity" | Out-Null
        Write-Host 'App is running on the emulator.'
    }
    'slips' {
        Start-Emulator | Out-Null
        $files = Get-ChildItem samples -File -Include *.jpg, *.jpeg, *.png -Recurse
        foreach ($f in $files) { & $adb push $f.FullName "/sdcard/Pictures/$($f.Name)" | Out-Null }
        # Make the gallery (MediaStore) index the new files.
        & $adb shell content call --uri content://media/ --method scan_volume --arg external_primary | Out-Null
        Write-Host "Copied $($files.Count) images to the emulator gallery."
    }
    'log' {
        Start-Emulator | Out-Null
        $appPid = (& $adb shell pidof $pkg)
        if ($appPid) { & $adb logcat -v color --pid=$($appPid.Trim()) '*:I' 'SlipScanner:D' }
        else { Write-Host 'App is not running; showing recent crashes:'; & $adb logcat -d -b crash }
    }
    'test' { Invoke-Gradle testDebugUnitTest }
    'release' {
        Invoke-Gradle assembleRelease
        Copy-Item app\build\outputs\apk\release\app-release.apk TeenJod.apk -Force
        Write-Host "Built $PSScriptRoot\TeenJod.apk"
    }
}
