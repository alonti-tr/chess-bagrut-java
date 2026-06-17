$ErrorActionPreference = "Stop"

$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$configDir = Join-Path $root "config"
New-Item -ItemType Directory -Path $configDir -Force | Out-Null

$keystore = Join-Path $configDir "server-keystore.jks"
$truststore = Join-Path $configDir "client-truststore.jks"
$cert = Join-Path $configDir "server-cert.cer"
$password = "changeit"

Write-Host "Generating server keystore..."
keytool -genkeypair -alias chess-server -keyalg RSA -keysize 2048 -validity 365 `
    -keystore $keystore -storepass $password -keypass $password `
    -dname "CN=localhost, OU=Chess, O=Bagrut, L=Local, ST=Local, C=IL"

Write-Host "Exporting server certificate..."
keytool -exportcert -alias chess-server -keystore $keystore `
    -storepass $password -file $cert

Write-Host "Creating client truststore..."
if (Test-Path $truststore) { Remove-Item $truststore }
keytool -importcert -alias chess-server -file $cert `
    -keystore $truststore -storepass $password -noprompt

Write-Host "Done."
Write-Host "  Keystore:   $keystore"
Write-Host "  Truststore: $truststore"
Write-Host "Set chess.tls.enabled=true in config/server.properties and config/client.properties"
