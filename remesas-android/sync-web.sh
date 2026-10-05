#!/usr/bin/env bash
# Descarga la última versión de la web de PeterServices y la copia dentro de la APK.
set -euo pipefail
cd "$(dirname "$0")"
REPO="${WEB_REPO:-https://github.com/momosad26/PeterServices}"
rm -rf web-src app/src/main/assets/www
git clone --depth 1 "$REPO" web-src
mkdir -p app/src/main/assets/www
(cd web-src && tar --exclude=.git --exclude=.github -cf - .) | (cd app/src/main/assets/www && tar -xf -)
if [ -f web-src/icons/icon-512.png ]; then
  cp web-src/icons/icon-512.png app/src/main/res/mipmap-xxxhdpi/ic_launcher.png
fi
echo "Web copiada (commit $(git -C web-src rev-parse --short HEAD))"
