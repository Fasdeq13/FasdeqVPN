#!/usr/bin/env bash
set -Eeuo pipefail

FLAVOR="${1:?wireguard-go или amneziawg}"
VERSION="${2:?версия, например 1.0.20250601}"
NDK="${ANDROID_NDK_HOME:?задайте ANDROID_NDK_HOME}"
API="${ANDROID_API:-26}"
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUT="${OUT:-$PWD/dist}"
mkdir -p "$OUT"

case "$FLAVOR" in
  wireguard-go) TAGS=""; NAME="WireGuard-Go"; MOD="golang.zx2c4.com/wireguard"; LIB="libfq_wg.so" ;;
  amneziawg) TAGS="awg"; NAME="AmneziaWG"; MOD="github.com/amnezia-vpn/amneziawg-go"; LIB="libfq_awg.so" ;;
  *) echo "неизвестный движок" >&2; exit 1 ;;
esac

TOOLCHAIN="$NDK/toolchains/llvm/prebuilt/linux-x86_64/bin"
WORK="$(mktemp -d)"
cp "$HERE"/*.go "$HERE/go.mod" "$WORK/"
cd "$WORK"
go get "$MOD@${UPSTREAM:-latest}"
go mod tidy

CGO_ENABLED=1 GOOS=android GOARCH=arm64 CC="$TOOLCHAIN/aarch64-linux-android${API}-clang" \
  go build -buildmode=c-shared -trimpath -ldflags "-s -w -X main.version=$VERSION" ${TAGS:+-tags "$TAGS"} -o "$LIB" .

cat > plugin.json <<JSON
{
  "id": "$FLAVOR",
  "family": "$FLAVOR",
  "name": "$NAME",
  "version": "$VERSION",
  "author": "FasdeqVPN",
  "description": "$NAME как движок WireGuard-конфигураций",
  "kinds": ["protocol"],
  "protocol": {
    "label": "$NAME",
    "lib": "$LIB",
    "handlesWireGuard": true,
    "match": {$( [ "$FLAVOR" = amneziawg ] && echo '"contains": ["[interface]", "jc"]' )}
  }
}
JSON
zip -j "$OUT/fq-$FLAVOR-$VERSION-arm64.zip" plugin.json "$LIB"
echo ">> $OUT/fq-$FLAVOR-$VERSION-arm64.zip"
