$ErrorActionPreference = "Stop"

$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
$env:GRADLE_USER_HOME = "D:\gradle-cache"
$env:ANDROID_HOME = "D:\Android\Sdk"
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME

Set-Location "D:\Perkuliahan\semester 7\menu"
& .\gradlew.bat :app:installDebug
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
