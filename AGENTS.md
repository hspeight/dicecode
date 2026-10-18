# Master Dice

Kotlin Multiplatform / Compose Multiplatform Mastermind-style dice deduction
game (Android + iOS + Desktop JVM). Spec: `docs/PRD/Master Dice PRD.md`.

## Layout

- `shared/` — all game code: `com.masterdice.game` (pure-Kotlin rules engine,
  ViewModel), `com.masterdice.data` (GameStore, okio file persistence),
  `com.masterdice.ui` (single Compose `GameScreen`), platform actuals in
  `androidMain`/`iosMain`/`desktopMain`
- `androidApp/` — thin Android shell (Activity, manifest, launcher icon)
- `desktopApp/` — Compose Desktop entry point (`com.masterdice.desktop.MainKt`)
- `iosApp/` — SwiftUI shell + Xcode project embedding the `MasterDiceShared`
  framework; `iosApp/Configuration/Config.xcconfig` holds TEAM_ID/BUNDLE_ID
  (Mac-local)

## Build & test

```bash
# Engine + store + ViewModel unit tests (JVM host tests)
./gradlew :shared:testAndroidHostTest

# Desktop unit + Compose UI tests
./gradlew :shared:desktopTest

# Android debug APK
./gradlew :androidApp:assembleDebug

# Run the desktop app (normal count-only gameplay)
./gradlew :desktopApp:run

# Desktop debug tools: add -Dmasterdice.debug=true to the application JVM.
# Positional feedback remains off until enabled in Settings.

# iOS Kotlin/Native klibs (compilation only; Xcode packaging needs macOS)
./gradlew :shared:compileKotlinIosArm64 :shared:compileKotlinIosSimulatorArm64
```

On Windows (this project is built from WSL too — `kotlin.incremental=false`
is set because the two hosts poison each other's incremental caches), run
Gradle with Android Studio's JBR:

```
cmd.exe /c "set \"JAVA_HOME=C:\Program Files\Android\Android Studio\jbr\" && cd /d C:\AndroidStudioProjects\DiceCode && gradlew.bat <tasks>"
```

Requires JDK 17+ and a local Android SDK (`local.properties` → `sdk.dir=`).
iOS test execution and `.framework` linking require macOS; on Windows/Linux
the iOS targets compile klibs only. `iosApp` builds via
`./gradlew :shared:embedAndSignAppleFrameworkForXcode` from Xcode.
