#!/usr/bin/env bash
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"
if [[ ! -f runtime/el-tumi-erp.jar ]]; then
  echo 'Falta el ejecutable. Compila con ./scripts/build.sh.' >&2
  exit 1
fi
tumi_args=()
if [[ "${1:-}" == '--demo' ]]; then
  tumi_args+=(--spring.profiles.active=demo)
  shift
else
  if [[ ! -f config/application.properties ]]; then
    cp config/application.properties.example config/application.properties
  fi
  tumi_args+=(--spring.config.additional-location=optional:file:./config/)
fi
echo 'El Tumi ERP: http://localhost:8080. Espera el mensaje Started TumiApplication.'
exec java -Duser.timezone=America/Lima -jar runtime/el-tumi-erp.jar "${tumi_args[@]}" "$@"
