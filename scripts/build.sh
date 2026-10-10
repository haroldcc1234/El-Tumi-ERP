#!/usr/bin/env bash
set -euo pipefail
tumi_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$tumi_root/frontend"
npm ci
npm run build
rm -rf -- "$tumi_root/backend/src/main/resources/static"
mkdir -p "$tumi_root/backend/src/main/resources/static"
cp -R dist/. "$tumi_root/backend/src/main/resources/static/"
cd "$tumi_root/backend"
mvn clean package
mkdir -p "$tumi_root/runtime"
cp target/el-tumi-erp-1.0.0.jar "$tumi_root/runtime/el-tumi-erp.jar"
echo 'Compilacion lista. Ejecuta ./iniciar.sh.'
