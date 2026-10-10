param([switch]$Demo, [int]$Port = 8080)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot
if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
  throw 'No se encontro Java. Instala Java 17 o 21 y vuelve a abrir la terminal.'
}
$jar = Join-Path $projectRoot 'runtime/el-tumi-erp.jar'
if (-not (Test-Path $jar)) { throw 'Falta runtime/el-tumi-erp.jar. Ejecuta scripts/build.ps1 para compilar el proyecto.' }
$appArgs = @('-Duser.timezone=America/Lima', '-jar', $jar, "--server.port=$Port")
if ($Demo) {
  $appArgs += '--spring.profiles.active=demo'
  Write-Host 'Modo demostracion: datos H2 en ./data. No utiliza MySQL.'
} else {
  if (-not (Test-Path 'config/application.properties')) {
    Copy-Item 'config/application.properties.example' 'config/application.properties'
    Write-Host 'Se creo config/application.properties. Puedes ajustar ahi tu conexion MySQL.'
  }
  $appArgs += '--spring.config.additional-location=optional:file:./config/'
}
Write-Host "El Tumi ERP: http://localhost:$Port"
Write-Host 'Espera el mensaje Started TumiApplication antes de abrir la pagina. Ctrl+C para detener.'
& java @appArgs
if ($LASTEXITCODE -ne 0) { throw "El servidor termino con codigo $LASTEXITCODE. Revisa el error de conexion o el puerto indicado arriba." }
