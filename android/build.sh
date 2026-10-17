#!/usr/bin/env bash
set -Eeuo pipefail
export LC_ALL=C.UTF-8
export LANG=C.UTF-8
export JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8"
export DEBIAN_FRONTEND=noninteractive

GRADLE_VER="8.10.2"
CMDLINE_TOOLS="11076708"
NDK_VER="27.2.12479018"
PLATFORM="android-35"
BUILD_TOOLS="35.0.0"
XRAY_LIB_VER="v26.9.9"
KS_PASS="fasdeqvpn"
KS_ALIAS="fasdeq"
APP_ID="${FASDEQ_APP_ID:-app.fasdeq.vpn}"
APP_LABEL="${FASDEQ_LABEL:-FasdeqVPN}"

SELF="$(readlink -f "${BASH_SOURCE[0]}")"
SRC="$(dirname "$SELF")"
OUT="$PWD"

if [ "$(id -u)" -ne 0 ]; then
  exec sudo -E bash "$SELF" "$@"
fi

OWNER="${SUDO_USER:-root}"
WORK="$(mktemp -d /var/tmp/fasdeqvpn.XXXXXX)"
BEFORE="$WORK/pkgs.before"

if command -v pacman >/dev/null 2>&1; then
  PM=pacman
else
  PM=apt
fi

pkg_list() {
  if [ "$PM" = pacman ]; then
    pacman -Qq | sort -u
  else
    dpkg-query -W -f='${binary:Package}\n' | sort -u
  fi
}

pkg_purge() {
  if [ "$PM" = pacman ]; then
    xargs -a "$1" pacman -Rns --noconfirm >/dev/null 2>&1
  else
    xargs -a "$1" apt-get purge -y >/dev/null 2>&1
  fi
}

pkg_list > "$BEFORE"

cleanup() {
  local rc=$?
  set +e
  cd /
  pkill -f "$WORK" >/dev/null 2>&1
  sleep 1
  pkg_list > "$WORK/pkgs.after"
  comm -13 "$BEFORE" "$WORK/pkgs.after" > "$WORK/pkgs.new"
  if [ -s "$WORK/pkgs.new" ]; then
    echo ">> Удаляю установленные пакеты"
    pkg_purge "$WORK/pkgs.new"
  fi
  if [ "$PM" = pacman ]; then
    pacman -Scc --noconfirm >/dev/null 2>&1 < <(yes)
  else
    apt-get clean >/dev/null 2>&1
  fi
  rm -rf "$WORK"
  if [ "$rc" -eq 0 ]; then
    echo ">> Готово. Временные файлы и пакеты удалены."
  else
    echo ">> Сборка завершилась с ошибкой (код $rc). Временные файлы и пакеты удалены."
  fi
  exit "$rc"
}
trap cleanup EXIT

echo ">> Устанавливаю зависимости"
if [ "$PM" = pacman ]; then
  pacman -S --needed --noconfirm jdk17-openjdk curl unzip ca-certificates >/dev/null 2>&1 \
    || { pacman -Sy --noconfirm >/dev/null 2>&1; pacman -S --needed --noconfirm jdk17-openjdk curl unzip ca-certificates >/dev/null; }
  export JAVA_HOME="/usr/lib/jvm/java-17-openjdk"
  if [ ! -x "$JAVA_HOME/bin/javac" ]; then
    JAVA_HOME="$(dirname "$(dirname "$(readlink -f "$(command -v javac)")")")"
    export JAVA_HOME
  fi
  export PATH="$JAVA_HOME/bin:$PATH"
else
  apt-get update -qq
  JDK=""
  for v in 17 21; do
    if apt-cache show "openjdk-$v-jdk-headless" >/dev/null 2>&1; then
      JDK="openjdk-$v-jdk-headless"
      break
    fi
  done
  if [ -z "$JDK" ]; then
    echo "Не найден openjdk 17 или 21 в репозиториях" >&2
    exit 1
  fi
  apt-get install -y -qq --no-install-recommends "$JDK" curl unzip ca-certificates >/dev/null

  JAVAC="$(dpkg -L "$JDK" | grep '/bin/javac$' | head -n1)"
  export JAVA_HOME="$(dirname "$(dirname "$(readlink -f "$JAVAC")")")"
  export PATH="$JAVA_HOME/bin:$PATH"
fi

export HOME="$WORK/home"
export GRADLE_USER_HOME="$WORK/gradle-home"
export ANDROID_USER_HOME="$WORK/android-home"
export ANDROID_HOME="$WORK/sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
mkdir -p "$HOME" "$GRADLE_USER_HOME" "$ANDROID_USER_HOME" "$ANDROID_HOME/cmdline-tools"

cd "$WORK"

echo ">> Скачиваю Gradle $GRADLE_VER"
GRADLE_URL="https://services.gradle.org/distributions/gradle-$GRADLE_VER-bin.zip"
curl -fsSL -o gradle.zip "$GRADLE_URL"
echo "$(curl -fsSL "$GRADLE_URL.sha256")  gradle.zip" | sha256sum -c - >/dev/null
unzip -q gradle.zip

echo ">> Скачиваю Android SDK"
curl -fsSL -o cmdline.zip "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS}_latest.zip"
unzip -q cmdline.zip -d "$ANDROID_HOME/cmdline-tools"
mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
SDKM="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
"$SDKM" --sdk_root="$ANDROID_HOME" --licenses < <(yes) >/dev/null 2>&1 || true
"$SDKM" --sdk_root="$ANDROID_HOME" "platforms;$PLATFORM" "build-tools;$BUILD_TOOLS" "ndk;$NDK_VER" >/dev/null

echo ">> Копирую проект"
mkdir -p "$WORK/proj"
tar -C "$SRC" --exclude=./build.sh --exclude=./.gradle --exclude=./build --exclude=./app/build --exclude='*.apk' --exclude='*.jks' -cf - . | tar -C "$WORK/proj" -xf -
echo "sdk.dir=$ANDROID_HOME" > "$WORK/proj/local.properties"

echo ">> Собираю нативное ядро на Rust"
export RUSTUP_HOME="$WORK/rustup"
export CARGO_HOME="$WORK/cargo"
if [ "$PM" = pacman ]; then
  pacman -S --needed --noconfirm base-devel >/dev/null 2>&1
else
  apt-get install -y -qq --no-install-recommends build-essential pkg-config >/dev/null
fi
curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y --profile minimal --no-modify-path >/dev/null 2>&1
export PATH="$CARGO_HOME/bin:$PATH"
rustup target add aarch64-linux-android armv7-linux-androideabi >/dev/null
NDKBIN="$ANDROID_HOME/ndk/$NDK_VER/toolchains/llvm/prebuilt/linux-x86_64/bin"
NATIVE_SRC="$(dirname "$SRC")/android-native"
CORE_SRC="$(dirname "$SRC")/core"
mkdir -p "$WORK/rs/android-native" "$WORK/rs/core"
tar -C "$NATIVE_SRC" --exclude=./target -cf - . | tar -C "$WORK/rs/android-native" -xf -
tar -C "$CORE_SRC" --exclude=./target -cf - . | tar -C "$WORK/rs/core" -xf -
build_native() {
  local triple="$1" abi="$2" clang="$3"
  local upper
  upper="$(echo "$triple" | tr 'a-z-' 'A-Z_')"
  export "CARGO_TARGET_${upper}_LINKER=$NDKBIN/$clang"
  export "CC_${triple//-/_}=$NDKBIN/$clang"
  export "AR_${triple//-/_}=$NDKBIN/llvm-ar"
  (cd "$WORK/rs/android-native" && cargo build --release --target "$triple" --target-dir "$WORK/rs/target")
  mkdir -p "$WORK/proj/app/src/main/jniLibs/$abi"
  cp "$WORK/rs/target/$triple/release/libfasdeq_native.so" "$WORK/proj/app/src/main/jniLibs/$abi/"
}
build_native aarch64-linux-android arm64-v8a aarch64-linux-android26-clang
build_native armv7-linux-androideabi armeabi-v7a armv7a-linux-androideabi26-clang

echo ">> Скачиваю ядро"
mkdir -p "$WORK/proj/app/libs"
curl -fL --retry 3 -o "$WORK/proj/app/libs/libv2ray.aar" "https://github.com/2dust/AndroidLibXrayLite/releases/download/$XRAY_LIB_VER/libv2ray.aar"
if [ "$(stat -c %s "$WORK/proj/app/libs/libv2ray.aar")" -lt 10000000 ]; then
  echo "Ядро скачалось некорректно" >&2
  exit 1
fi

OWNER_HOME="$(getent passwd "$OWNER" | cut -d: -f6)"
KS_DIR="${OWNER_HOME:-$OUT}/.fasdeqvpn"
KS_OUT="$KS_DIR/release.jks"
KS="$WORK/release.jks"
mkdir -p "$KS_DIR"
chown "$OWNER" "$KS_DIR"
if [ ! -f "$KS_OUT" ] && [ -f "$OUT/fasdeqvpn-release.jks" ]; then
  cp "$OUT/fasdeqvpn-release.jks" "$KS_OUT"
  chown "$OWNER" "$KS_OUT"
fi
if [ -f "$KS_OUT" ]; then
  cp "$KS_OUT" "$KS"
else
  echo ">> Создаю ключ подписи"
  keytool -genkeypair -alias "$KS_ALIAS" -keyalg RSA -keysize 2048 -validity 36500 \
    -keystore "$KS" -storepass "$KS_PASS" -keypass "$KS_PASS" \
    -dname "CN=FasdeqVPN, O=Fasdeq, C=RU"
  cp "$KS" "$KS_OUT"
  chown "$OWNER" "$KS_OUT"
fi

echo ">> Собираю APK"
cd "$WORK/proj"
"$WORK/gradle-$GRADLE_VER/bin/gradle" --no-daemon --console=plain assembleRelease \
  -Pks.path="$KS" -Pks.pass="$KS_PASS" -Pks.alias="$KS_ALIAS" \
  -Papp.id="$APP_ID" -Papp.label="$APP_LABEL" -Papp.vcode="$(( $(date +%s) / 60 ))"

APK="$WORK/proj/app/build/outputs/apk/release/app-release.apk"
if [ ! -f "$APK" ]; then
  echo "APK не найден" >&2
  exit 1
fi
cp "$APK" "$OUT/FasdeqVPN.apk"
chown "$OWNER" "$OUT/FasdeqVPN.apk"
echo ">> APK: $OUT/FasdeqVPN.apk"
