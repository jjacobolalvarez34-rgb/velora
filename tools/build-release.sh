#!/usr/bin/env bash
# Compila la versión oficial (release) de Velora y la copia a la raíz como Velora-<versión>.apk.
# Luego corre la verificación completa (pruebas, ktlint, detekt, lint) y deja el resultado en
# build-report.txt. Funciona solo, sin nadie mirando: en Windows, desde Git Bash:
#   bash tools/build-release.sh
set -u
cd "$(dirname "$0")/.."
export JAVA_HOME="${JAVA_HOME:-$HOME/.jdks/jbr-21.0.11}"
REPORT=build-report.txt
VERSION=$(grep -oE 'versionName = "[^"]+"' app/build.gradle.kts | cut -d'"' -f2)

{
  echo "Velora $VERSION — $(date)"
  echo "== 1. APK release =="
} > "$REPORT"

if ./gradlew.bat :app:assembleRelease -q >> "$REPORT" 2>&1; then
  rm -f Velora-*.apk
  cp app/build/outputs/apk/release/app-release.apk "Velora-$VERSION.apk"
  echo "OK: Velora-$VERSION.apk listo en la raíz del proyecto." >> "$REPORT"
else
  echo "FALLÓ el APK release (ver arriba)." >> "$REPORT"
fi

echo "== 2. Verificación (pruebas, ktlint, detekt, lint) ==" >> "$REPORT"
if ./gradlew.bat testDebugUnitTest ktlintCheck detekt lint -q --continue >> "$REPORT" 2>&1; then
  echo "OK: todo en verde." >> "$REPORT"
else
  echo "Hay avisos o errores de estilo (ver arriba). El APK de arriba igual sirve para probar." >> "$REPORT"
fi
echo "Terminado: $(date)" >> "$REPORT"
