$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
foreach ($tool in @('java', 'mvn', 'npm')) {
  if (-not (Get-Command $tool -ErrorAction SilentlyContinue)) { throw "Falta $tool. Para compilar necesitas JDK 17 o 21, Maven 3.9 y Node 22.12 o superior." }
}
Push-Location (Join-Path $projectRoot 'frontend')
try {
  & npm.cmd ci
  if ($LASTEXITCODE -ne 0) { throw 'No se pudieron instalar las dependencias de React.' }
  & npm.cmd run build
  if ($LASTEXITCODE -ne 0) { throw 'Fallo la compilacion del frontend.' }
} finally { Pop-Location }
$static = Join-Path $projectRoot 'backend/src/main/resources/static'
if (Test-Path $static) { Remove-Item $static -Recurse -Force }
New-Item $static -ItemType Directory -Force | Out-Null
Copy-Item (Join-Path $projectRoot 'frontend/dist/*') $static -Recurse
Push-Location (Join-Path $projectRoot 'backend')
try {
  & mvn.cmd clean package
  if ($LASTEXITCODE -ne 0) { throw 'Fallo la compilacion o las pruebas del backend.' }
} finally { Pop-Location }
New-Item (Join-Path $projectRoot 'runtime') -ItemType Directory -Force | Out-Null
Copy-Item (Join-Path $projectRoot 'backend/target/el-tumi-erp-1.0.0.jar') (Join-Path $projectRoot 'runtime/el-tumi-erp.jar') -Force
Write-Host 'Compilacion lista. Ejecuta iniciar.cmd.'
