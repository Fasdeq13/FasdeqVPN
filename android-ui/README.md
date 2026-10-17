Прототип интерфейса FasdeqVPN на чистом Rust (eframe/egui + android-native-activity), без Java в UI.

Проверка на ПК: cargo run --bin fasdeq-ui-preview

Сборка под Android: cargo install cargo-apk, затем cargo apk build --release (нужны Android SDK/NDK).
Кроме UI нужен маленький VpnService на Java/Kotlin: Android не позволяет написать его на нативном коде.
Известные пробелы: буфер обмена и экранная клавиатура в egui на Android требуют JNI-моста.
