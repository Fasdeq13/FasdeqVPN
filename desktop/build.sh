#!/usr/bin/env bash
set -Eeuo pipefail

SELF="$(readlink -f "${BASH_SOURCE[0]}")"
SRC="$(dirname "$SELF")"
OUT="${FASDEQ_OUT:-$PWD/dist}"
TARGETS=("$@")
if [ "${#TARGETS[@]}" -eq 0 ]; then
  TARGETS=(linux windows)
fi

pm_install() {
  if command -v pacman >/dev/null 2>&1; then
    as_root pacman -S --needed --noconfirm "$@" >/dev/null
  else
    as_root apt-get update -qq
    as_root apt-get install -y -qq --no-install-recommends "$@" >/dev/null
  fi
}

is_arch() {
  command -v pacman >/dev/null 2>&1
}

as_root() {
  if [ "$(id -u)" -eq 0 ]; then
    "$@"
  else
    sudo "$@"
  fi
}

ensure_rust() {
  if ! command -v cargo >/dev/null 2>&1 && [ -f "$HOME/.cargo/env" ]; then
    . "$HOME/.cargo/env"
  fi
  if ! command -v cargo >/dev/null 2>&1; then
    echo ">> Устанавливаю Rust"
    curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y --profile minimal
    . "$HOME/.cargo/env"
  fi
}

build_linux() {
  echo ">> Linux: зависимости"
  if is_arch; then
    pm_install base-devel pkgconf curl ca-certificates gtk3 xdotool libayatana-appindicator libxkbcommon wayland libx11 libxcursor libxrandr libxi
  else
    pm_install build-essential pkg-config curl ca-certificates \
      libgtk-3-dev libxdo-dev libayatana-appindicator3-dev \
      libxkbcommon-dev libwayland-dev libx11-dev libxcursor-dev libxrandr-dev libxi-dev
  fi
  echo ">> Linux: сборка"
  (cd "$SRC" && cargo build --release)
  mkdir -p "$OUT/linux"
  cp "$SRC/target/release/fasdeqvpn" "$OUT/linux/FasdeqVPN"
  cp "$SRC/assets/app.svg" "$OUT/linux/FasdeqVPN.svg"
  cat > "$OUT/linux/FasdeqVPN.desktop" <<D
[Desktop Entry]
Type=Application
Name=FasdeqVPN
Exec=FasdeqVPN
Icon=FasdeqVPN
Categories=Network;
Terminal=false
D
  echo ">> Linux: $OUT/linux/FasdeqVPN"
}

build_windows() {
  echo ">> Windows: зависимости"
  if is_arch; then
    pm_install mingw-w64-gcc base-devel curl ca-certificates
  else
    pm_install mingw-w64 build-essential curl ca-certificates
  fi
  rustup target add x86_64-pc-windows-gnu
  echo ">> Windows: сборка"
  (cd "$SRC" && \
    CARGO_TARGET_X86_64_PC_WINDOWS_GNU_LINKER=x86_64-w64-mingw32-gcc \
    CC_x86_64_pc_windows_gnu=x86_64-w64-mingw32-gcc \
    cargo build --release --target x86_64-pc-windows-gnu)
  mkdir -p "$OUT/windows"
  cp "$SRC/target/x86_64-pc-windows-gnu/release/fasdeqvpn.exe" "$OUT/windows/FasdeqVPN.exe"
  echo ">> Windows: $OUT/windows/FasdeqVPN.exe"
}

ensure_rust

for t in "${TARGETS[@]}"; do
  case "$t" in
    linux) build_linux ;;
    windows) build_windows ;;
    *) echo "Неизвестная цель: $t" >&2; exit 1 ;;
  esac
done

echo ">> Готово: $OUT"
