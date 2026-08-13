# Android Foundation Implementation Plan (Plan 1 of 5)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Stand up a running native Android app — themed, navigable, bilingual, Firebase-connected — and get a signed AAB onto a Play Console closed track so the mandatory 14-day test clock starts.

**Architecture:** Single-Activity Jetpack Compose app under `android/` in the existing monorepo. MVVM with `ViewModel` + `StateFlow`. Design tokens ported verbatim from `AppColors.swift`/`AppFonts.swift`. Language is an explicit in-app vi/en toggle persisted in DataStore and applied via `AppCompatDelegate.setApplicationLocales` — not system-locale-following.

**Tech Stack:** Kotlin, Jetpack Compose (Material3), Navigation Compose, DataStore Preferences, Firebase Firestore, AppCompat (per-app locales), JUnit4 + Robolectric, Compose UI test.

## Global Constraints

- Package name / `applicationId` / `namespace`: **`vn.hocdantranh.guzheng`** — permanent, never change after publishing.
- `minSdk = 24`. `compileSdk`/`targetSdk` = current Play requirement (see Task 1, Step 2).
- Gradle JDK: **Android Studio's bundled JBR 17** at `/Applications/Android Studio.app/Contents/jbr/Contents/Home`. The system JDK is 20, which AGP does not officially support — never rely on it.
- All Android code lives under `android/`. **Do not modify anything under `HocDanTranh-Guzheng-DanThanh/`** (the iOS app) in this plan.
- Firebase project is the existing **`apphocdantranh`** — register a *new Android app* inside it, do not create a new project.
- Vietnamese is the default locale (`values/`); English is `values-en/`.
- Tab order must match iOS exactly: **Lessons → Video → Tuner → Sheet → Settings**.
- Never commit `google-services.json` contents to a public remote without a deliberate decision; it is added to git in Task 6 by explicit choice.
- Colors are copied hex-exact from `AppColors.swift`. Do not re-derive or "improve" them.

---

### Task 1: Gradle project scaffold that builds

**Files:**
- Create: `android/settings.gradle.kts`
- Create: `android/build.gradle.kts`
- Create: `android/gradle.properties`
- Create: `android/gradle/libs.versions.toml`
- Create: `android/gradle/wrapper/gradle-wrapper.properties`
- Create: `android/app/build.gradle.kts`
- Create: `android/app/src/main/AndroidManifest.xml`
- Create: `android/app/src/main/java/vn/hocdantranh/guzheng/MainActivity.kt`
- Create: `android/local.properties` (git-ignored)
- Modify: `.gitignore`

**Interfaces:**
- Consumes: nothing.
- Produces: a Gradle project rooted at `android/` whose `:app` module builds a debug APK. Applies plugins `com.android.application`, `org.jetbrains.kotlin.android`, `org.jetbrains.kotlin.plugin.compose`. Later tasks add dependencies to `android/app/build.gradle.kts` and source files under `android/app/src/main/java/vn/hocdantranh/guzheng/`.

- [ ] **Step 1: Create the Gradle wrapper from Android Studio's JDK**

Android Studio is installed but `gradle` is not on PATH. Generate the wrapper using the bundled JBR:

```bash
cd /Users/kelvin/Code/AppHocDanTranh
mkdir -p android/app/src/main/java/vn/hocdantranh/guzheng
mkdir -p android/gradle/wrapper
```

Create `android/gradle/wrapper/gradle-wrapper.properties`:

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.11.1-bin.zip
networkTimeout=10000
validateDistributionUrl=true
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

The wrapper JAR and `gradlew` script are fetched in Step 4.

- [ ] **Step 2: Determine the required target API level**

Google Play enforces a minimum `targetSdk` for new apps, raised annually (API 35 as of Aug 2025). Check the current requirement at
https://developer.android.com/google/play/requirements/target-sdk
then install that platform:

```bash
~/Library/Android/sdk/cmdline-tools/latest/bin/sdkmanager --list_installed
~/Library/Android/sdk/cmdline-tools/latest/bin/sdkmanager "platforms;android-36" "build-tools;36.0.0"
```

Only `android-34` is currently installed, which is below the Play requirement — this step is mandatory, not optional.
Record the installed level; use it for `compileSdk` and `targetSdk` everywhere below (this plan writes `36`).

- [ ] **Step 3: Write the build configuration**

`android/settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "DanThanhGuzheng"
include(":app")
```

`android/gradle/libs.versions.toml` — these are verified-stable floors. If dependency resolution fails in Step 5, bump to the latest stable shown by Android Studio and record what you changed:

```toml
[versions]
agp = "8.9.1"
kotlin = "2.1.0"
composeBom = "2025.01.00"
coreKtx = "1.15.0"
lifecycle = "2.8.7"
activityCompose = "1.9.3"
navigationCompose = "2.8.5"
appcompat = "1.7.0"
datastore = "1.1.1"
junit = "4.13.2"
robolectric = "4.14.1"
androidxTestExt = "1.2.1"
espresso = "3.6.1"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-appcompat = { group = "androidx.appcompat", name = "appcompat", version.ref = "appcompat" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
robolectric = { group = "org.robolectric", name = "robolectric", version.ref = "robolectric" }
androidx-test-ext-junit = { group = "androidx.test.ext", name = "junit", version.ref = "androidxTestExt" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espresso" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
```

`android/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
```

`android/gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

`android/app/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "vn.hocdantranh.guzheng"
    compileSdk = 36

    defaultConfig {
        applicationId = "vn.hocdantranh.guzheng"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        resourceConfigurations += listOf("vi", "en")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
}
```

Create `android/app/proguard-rules.pro` as an empty file (referenced above, must exist):

```bash
touch /Users/kelvin/Code/AppHocDanTranh/android/app/proguard-rules.pro
```

`android/local.properties` (machine-specific, git-ignored):

```properties
sdk.dir=/Users/kelvin/Library/Android/sdk
```

`android/app/src/main/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:name=".DanThanhApp"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="false"
        android:theme="@style/Theme.DanThanh">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:configChanges="orientation|screenSize|keyboardHidden|uiMode|locale|layoutDirection"
            android:theme="@style/Theme.DanThanh">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

`DanThanhApp` is created in Task 6, `@string/app_name` and `Theme.DanThanh` in Tasks 4 and 2. To keep this task independently buildable, create the three placeholders now — they are replaced, not extended, by later tasks:

`android/app/src/main/res/values/strings.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Đàn Tranh</string>
</resources>
```

`android/app/src/main/res/values/themes.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.DanThanh" parent="Theme.AppCompat.DayNight.NoActionBar" />
</resources>
```

`android/app/src/main/java/vn/hocdantranh/guzheng/DanThanhApp.kt`:

```kotlin
package vn.hocdantranh.guzheng

import android.app.Application

class DanThanhApp : Application()
```

`android/app/src/main/java/vn/hocdantranh/guzheng/MainActivity.kt`:

```kotlin
package vn.hocdantranh.guzheng

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.Text

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Text("Đàn Tranh")
        }
    }
}
```

Launcher icons: generate placeholder `mipmap` resources now so the manifest resolves. Copy any square PNG (e.g. `MatTruoc.jpg` converted) or use Android Studio's Image Asset tool. Minimum to build:

```bash
cd /Users/kelvin/Code/AppHocDanTranh/android/app/src/main/res
mkdir -p mipmap-mdpi mipmap-hdpi mipmap-xhdpi mipmap-xxhdpi mipmap-xxxhdpi
sips -s format png -z 48 48   ../../../../../MatTruoc.jpg --out mipmap-mdpi/ic_launcher.png
sips -s format png -z 72 72   ../../../../../MatTruoc.jpg --out mipmap-hdpi/ic_launcher.png
sips -s format png -z 96 96   ../../../../../MatTruoc.jpg --out mipmap-xhdpi/ic_launcher.png
sips -s format png -z 144 144 ../../../../../MatTruoc.jpg --out mipmap-xxhdpi/ic_launcher.png
sips -s format png -z 192 192 ../../../../../MatTruoc.jpg --out mipmap-xxxhdpi/ic_launcher.png
```

Real adaptive icons are produced in Plan 5 before the production release.

- [ ] **Step 4: Fetch the Gradle wrapper**

```bash
cd /Users/kelvin/Code/AppHocDanTranh/android
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
curl -sL https://services.gradle.org/distributions/gradle-8.11.1-bin.zip -o /tmp/gradle.zip \
  && unzip -q -o /tmp/gradle.zip -d /tmp/gradle-dist \
  && /tmp/gradle-dist/gradle-8.11.1/bin/gradle wrapper --gradle-version 8.11.1
```

Expected: `gradlew`, `gradlew.bat`, and `gradle/wrapper/gradle-wrapper.jar` created.

- [ ] **Step 5: Build and verify it fails or passes explicitly**

```bash
cd /Users/kelvin/Code/AppHocDanTranh/android
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:assembleDebug --stacktrace
```

Expected: `BUILD SUCCESSFUL`, and `android/app/build/outputs/apk/debug/app-debug.apk` exists.
If dependency resolution fails, bump the failing version in `libs.versions.toml` to the latest stable and re-run. Record any version you changed in the commit message.

Verify the artifact and its package name:

```bash
ls -la app/build/outputs/apk/debug/app-debug.apk
~/Library/Android/sdk/build-tools/36.0.0/aapt2 dump packagename app/build/outputs/apk/debug/app-debug.apk
```

Expected output: `vn.hocdantranh.guzheng`

- [ ] **Step 6: Ignore Android build artifacts**

Append to `/Users/kelvin/Code/AppHocDanTranh/.gitignore`:

```gitignore
# Android
android/.gradle/
android/build/
android/app/build/
android/local.properties
android/.idea/
android/captures/
*.apk
*.aab
*.keystore
!android/gradle/wrapper/gradle-wrapper.jar
```

- [ ] **Step 7: Commit**

```bash
cd /Users/kelvin/Code/AppHocDanTranh
git add .gitignore android/
git status --short
git commit -m "feat(android): scaffold Gradle project for vn.hocdantranh.guzheng"
```

Check `git status --short` output before committing — confirm no build outputs or `local.properties` are staged.

---

### Task 2: Design system ported from iOS

**Files:**
- Create: `android/app/src/main/java/vn/hocdantranh/guzheng/ui/theme/Color.kt`
- Create: `android/app/src/main/java/vn/hocdantranh/guzheng/ui/theme/Type.kt`
- Create: `android/app/src/main/java/vn/hocdantranh/guzheng/ui/theme/Theme.kt`
- Test: `android/app/src/test/java/vn/hocdantranh/guzheng/ui/theme/ColorTest.kt`
- Reference (read-only): `HocDanTranh-Guzheng-DanThanh/HocDanTranh-Guzheng-DanThanh/Utilities/AppColors.swift`

**Interfaces:**
- Consumes: Task 1's Gradle project.
- Produces:
  - `object VintageColors` with `val paper, paperWarm, paperDeep, inkDark, inkSoft, inkMuted, sage, sageDark, sageSoft, wood, woodDark, woodSoft, plum, plumSoft, vintageGold, sealRed: Color`
  - `val DanThanhTypography: Typography`
  - `@Composable fun DanThanhTheme(content: @Composable () -> Unit)`

  Every later task wraps its UI in `DanThanhTheme { }` and reads colors from `VintageColors`.

- [ ] **Step 1: Write the failing test**

The iOS palette is the source of truth. This test locks the exact ARGB values so a later "cleanup" cannot silently shift the brand colors.

`android/app/src/test/java/vn/hocdantranh/guzheng/ui/theme/ColorTest.kt`:

```kotlin
package vn.hocdantranh.guzheng.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

class ColorTest {

    private fun assertHex(expected: String, actual: Color) {
        val expectedColor = Color(android.graphics.Color.parseColor("#$expected"))
        assertEquals(expectedColor.value, actual.value)
    }

    @Test
    fun `paper tones match iOS AppColors`() {
        assertHex("F2E8D5", VintageColors.paper)
        assertHex("FFF8EB", VintageColors.paperWarm)
        assertHex("E8DCC0", VintageColors.paperDeep)
    }

    @Test
    fun `ink tones match iOS AppColors`() {
        assertHex("3A3228", VintageColors.inkDark)
        assertHex("5C4E3D", VintageColors.inkSoft)
        assertHex("8A7B66", VintageColors.inkMuted)
    }

    @Test
    fun `sage tones match iOS AppColors`() {
        assertHex("8A9A7B", VintageColors.sage)
        assertHex("6B7C5E", VintageColors.sageDark)
        assertHex("B5C2A6", VintageColors.sageSoft)
    }

    @Test
    fun `wood tones match iOS AppColors`() {
        assertHex("B8956A", VintageColors.wood)
        assertHex("8B6F4E", VintageColors.woodDark)
        assertHex("D4B896", VintageColors.woodSoft)
    }

    @Test
    fun `accent tones match iOS AppColors`() {
        assertHex("A65F5F", VintageColors.plum)
        assertHex("C48888", VintageColors.plumSoft)
        assertHex("C9A961", VintageColors.vintageGold)
        assertHex("B84A3E", VintageColors.sealRed)
    }
}
```

This test uses `android.graphics.Color`, so it needs Robolectric. Add the runner annotation at the class level:

```kotlin
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
class ColorTest {
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd /Users/kelvin/Code/AppHocDanTranh/android
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:testDebugUnitTest --tests "*ColorTest*"
```

Expected: FAIL — compilation error, `Unresolved reference: VintageColors`.

- [ ] **Step 3: Write the color tokens**

`ui/theme/Color.kt` — hex values copied verbatim from `AppColors.swift`:

```kotlin
package vn.hocdantranh.guzheng.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Vintage paper + sage + wood + plum palette.
 * Ported hex-exact from iOS AppColors.swift — do not re-derive these values.
 */
object VintageColors {
    // Paper tones
    val paper = Color(0xFFF2E8D5)
    val paperWarm = Color(0xFFFFF8EB)
    val paperDeep = Color(0xFFE8DCC0)

    // Ink (text)
    val inkDark = Color(0xFF3A3228)
    val inkSoft = Color(0xFF5C4E3D)
    val inkMuted = Color(0xFF8A7B66)

    // Sage green
    val sage = Color(0xFF8A9A7B)
    val sageDark = Color(0xFF6B7C5E)
    val sageSoft = Color(0xFFB5C2A6)

    // Wood
    val wood = Color(0xFFB8956A)
    val woodDark = Color(0xFF8B6F4E)
    val woodSoft = Color(0xFFD4B896)

    // Plum / accent
    val plum = Color(0xFFA65F5F)
    val plumSoft = Color(0xFFC48888)

    // Special
    val vintageGold = Color(0xFFC9A961)
    val sealRed = Color(0xFFB84A3E)
}
```

- [ ] **Step 4: Run test to verify it passes**

```bash
./gradlew :app:testDebugUnitTest --tests "*ColorTest*"
```

Expected: PASS, 5 tests.

- [ ] **Step 5: Write the typography**

iOS uses `design: .serif` for titles and `.default` for body. Android's closest built-in equivalents are `FontFamily.Serif` and `FontFamily.SansSerif`. No custom font files are shipped in this plan — a licensed serif can be swapped in later by changing this one file.

`ui/theme/Type.kt`:

```kotlin
package vn.hocdantranh.guzheng.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Mirrors iOS AppFonts.swift:
 *   serif for titles (traditional feel), sans-serif for body (readability).
 */
val DanThanhTypography = Typography(
    // heroTitle — Font.system(.largeTitle, design: .serif, weight: .bold)
    displaySmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 41.sp
    ),
    // sectionTitle — Font.system(.title, design: .serif, weight: .semibold)
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp
    ),
    // cardTitle — Font.system(.title3, design: .serif, weight: .semibold)
    titleLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 25.sp
    ),
    // subtitle — Font.system(.headline, design: .serif, weight: .medium)
    titleMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        lineHeight = 22.sp
    ),
    // bodyText — Font.system(.body, design: .default)
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 22.sp
    ),
    // caption — Font.system(.caption, design: .default)
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    // metadata — Font.system(.caption2, design: .default)
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 13.sp
    ),
    // buttonText — Font.system(.body, design: .default, weight: .semibold)
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp
    )
)
```

- [ ] **Step 6: Write the theme**

The iOS app is light-only (a warm paper aesthetic). Do not add a dark scheme — a dark variant would invert the entire design language. Force light.

`ui/theme/Theme.kt`:

```kotlin
package vn.hocdantranh.guzheng.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DanThanhColorScheme = lightColorScheme(
    primary = VintageColors.sageDark,
    onPrimary = VintageColors.paperWarm,
    primaryContainer = VintageColors.sageSoft,
    onPrimaryContainer = VintageColors.inkDark,
    secondary = VintageColors.wood,
    onSecondary = VintageColors.paperWarm,
    secondaryContainer = VintageColors.woodSoft,
    onSecondaryContainer = VintageColors.inkDark,
    tertiary = VintageColors.plum,
    onTertiary = VintageColors.paperWarm,
    background = VintageColors.paper,
    onBackground = VintageColors.inkDark,
    surface = VintageColors.paperWarm,
    onSurface = VintageColors.inkDark,
    surfaceVariant = VintageColors.paperDeep,
    onSurfaceVariant = VintageColors.inkSoft,
    outline = VintageColors.inkMuted,
    error = VintageColors.sealRed,
    onError = VintageColors.paperWarm
)

@Composable
fun DanThanhTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DanThanhColorScheme,
        typography = DanThanhTypography,
        content = content
    )
}
```

- [ ] **Step 7: Run all tests and build**

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Expected: `BUILD SUCCESSFUL`, all tests pass.

- [ ] **Step 8: Commit**

```bash
cd /Users/kelvin/Code/AppHocDanTranh
git add android/app/src/main/java/vn/hocdantranh/guzheng/ui/ android/app/src/test/
git commit -m "feat(android): port vintage color palette and typography from iOS"
```

---

### Task 3: Language preference persisted in DataStore

**Files:**
- Create: `android/app/src/main/java/vn/hocdantranh/guzheng/data/prefs/AppPrefs.kt`
- Test: `android/app/src/test/java/vn/hocdantranh/guzheng/data/prefs/AppPrefsTest.kt`
- Modify: `android/app/build.gradle.kts` (add coroutines-test)
- Modify: `android/gradle/libs.versions.toml`

**Interfaces:**
- Consumes: Task 1's Gradle project.
- Produces:
  - `class AppPrefs(private val context: Context)`
  - `val language: Flow<String>` — emits `"vi"` or `"en"`, defaulting to `"vi"`
  - `suspend fun setLanguage(code: String)`
  - `companion object { const val VI = "vi"; const val EN = "en"; const val DEFAULT = VI }`

  Task 4 consumes `AppPrefs.language` to drive locale switching. Later plans add more keys to this same class.

- [ ] **Step 1: Add the coroutines test dependency**

In `libs.versions.toml`, under `[versions]` add:

```toml
coroutines = "1.9.0"
```

under `[libraries]` add:

```toml
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }
```

In `android/app/build.gradle.kts`, add to `dependencies`:

```kotlin
    testImplementation(libs.kotlinx.coroutines.test)
```

- [ ] **Step 2: Write the failing test**

`android/app/src/test/java/vn/hocdantranh/guzheng/data/prefs/AppPrefsTest.kt`:

```kotlin
package vn.hocdantranh.guzheng.data.prefs

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class AppPrefsTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val prefs = AppPrefs(context)

    @After
    fun tearDown() {
        File(context.filesDir, "datastore").deleteRecursively()
    }

    @Test
    fun `defaults to Vietnamese`() = runTest {
        assertEquals("vi", prefs.language.first())
    }

    @Test
    fun `persists English selection`() = runTest {
        prefs.setLanguage(AppPrefs.EN)
        assertEquals("en", prefs.language.first())
    }

    @Test
    fun `switches back to Vietnamese`() = runTest {
        prefs.setLanguage(AppPrefs.EN)
        prefs.setLanguage(AppPrefs.VI)
        assertEquals("vi", prefs.language.first())
    }

    @Test
    fun `rejects unknown language codes and keeps current value`() = runTest {
        prefs.setLanguage(AppPrefs.EN)
        prefs.setLanguage("fr")
        assertEquals("en", prefs.language.first())
    }
}
```

Add to `dependencies` in `android/app/build.gradle.kts` so `ApplicationProvider` resolves in unit tests:

```kotlin
    testImplementation(libs.androidx.test.ext.junit)
```

- [ ] **Step 3: Run test to verify it fails**

```bash
cd /Users/kelvin/Code/AppHocDanTranh/android
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:testDebugUnitTest --tests "*AppPrefsTest*"
```

Expected: FAIL — `Unresolved reference: AppPrefs`.

- [ ] **Step 4: Write the implementation**

`data/prefs/AppPrefs.kt`:

```kotlin
package vn.hocdantranh.guzheng.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "app_prefs")

/**
 * App-wide user preferences.
 * Replaces iOS @AppStorage — the "appLanguage" key becomes [language].
 */
class AppPrefs(private val context: Context) {

    val language: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[LANGUAGE_KEY] ?: DEFAULT
    }

    /** No-ops on codes the app does not ship, so a bad value can never brick the UI. */
    suspend fun setLanguage(code: String) {
        if (code !in SUPPORTED) return
        context.dataStore.edit { prefs -> prefs[LANGUAGE_KEY] = code }
    }

    companion object {
        const val VI = "vi"
        const val EN = "en"
        const val DEFAULT = VI

        private val SUPPORTED = setOf(VI, EN)
        private val LANGUAGE_KEY = stringPreferencesKey("appLanguage")
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

```bash
./gradlew :app:testDebugUnitTest --tests "*AppPrefsTest*"
```

Expected: PASS, 4 tests.

- [ ] **Step 6: Commit**

```bash
cd /Users/kelvin/Code/AppHocDanTranh
git add android/
git commit -m "feat(android): persist vi/en language preference in DataStore"
```

---

### Task 4: Bilingual strings and in-app locale switching

**Files:**
- Modify: `android/app/src/main/res/values/strings.xml` (replace placeholder from Task 1)
- Create: `android/app/src/main/res/values-en/strings.xml`
- Create: `android/app/src/main/java/vn/hocdantranh/guzheng/ui/LocaleController.kt`
- Test: `android/app/src/test/java/vn/hocdantranh/guzheng/ui/LocaleControllerTest.kt`
- Reference (read-only): `HocDanTranh-Guzheng-DanThanh/HocDanTranh-Guzheng-DanThanh/Resources/vi.lproj/`, `en.lproj/`

**Interfaces:**
- Consumes: `AppPrefs` from Task 3.
- Produces:
  - `object LocaleController` with `fun apply(code: String)` — calls `AppCompatDelegate.setApplicationLocales`
  - `fun currentLanguage(): String` — reads back the applied locale, returns `AppPrefs.DEFAULT` if unset
  - String resources `R.string.tab_lessons`, `tab_video`, `tab_tuner`, `tab_sheet`, `tab_settings`, `app_name`

  Task 5 uses these string resources for tab labels.

- [ ] **Step 1: Write the Vietnamese strings (default locale)**

Replace `android/app/src/main/res/values/strings.xml` entirely:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Đàn Tranh</string>

    <!-- Tab bar — order must match iOS MainTabView -->
    <string name="tab_lessons">Khóa học</string>
    <string name="tab_video">Video</string>
    <string name="tab_tuner">Lên dây</string>
    <string name="tab_sheet">Sheet</string>
    <string name="tab_settings">Cài đặt</string>

    <!-- Placeholder screen copy, replaced in Plan 2 -->
    <string name="screen_coming_soon">Đang phát triển</string>
</resources>
```

- [ ] **Step 2: Write the English strings**

`android/app/src/main/res/values-en/strings.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Dan Thanh Guzheng</string>

    <string name="tab_lessons">Lessons</string>
    <string name="tab_video">Video</string>
    <string name="tab_tuner">Tuner</string>
    <string name="tab_sheet">Sheet</string>
    <string name="tab_settings">Settings</string>

    <string name="screen_coming_soon">Coming soon</string>
</resources>
```

Cross-check these against the iOS `.lproj` files so wording matches the shipped app:

```bash
cat "/Users/kelvin/Code/AppHocDanTranh/HocDanTranh-Guzheng-DanThanh/HocDanTranh-Guzheng-DanThanh/Resources/vi.lproj/Localizable.strings"
cat "/Users/kelvin/Code/AppHocDanTranh/HocDanTranh-Guzheng-DanThanh/HocDanTranh-Guzheng-DanThanh/Resources/en.lproj/Localizable.strings"
```

If the iOS wording differs from the above, the iOS wording wins — users must see identical copy on both platforms.

- [ ] **Step 3: Write the failing test**

`android/app/src/test/java/vn/hocdantranh/guzheng/ui/LocaleControllerTest.kt`:

```kotlin
package vn.hocdantranh.guzheng.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import vn.hocdantranh.guzheng.data.prefs.AppPrefs

@RunWith(RobolectricTestRunner::class)
class LocaleControllerTest {

    @Test
    fun `unset locale reports the Vietnamese default`() {
        LocaleController.apply("")
        assertEquals(AppPrefs.DEFAULT, LocaleController.currentLanguage())
    }

    @Test
    fun `applying English is readable back`() {
        LocaleController.apply(AppPrefs.EN)
        assertEquals("en", LocaleController.currentLanguage())
    }

    @Test
    fun `applying Vietnamese is readable back`() {
        LocaleController.apply(AppPrefs.EN)
        LocaleController.apply(AppPrefs.VI)
        assertEquals("vi", LocaleController.currentLanguage())
    }
}
```

- [ ] **Step 4: Run test to verify it fails**

```bash
./gradlew :app:testDebugUnitTest --tests "*LocaleControllerTest*"
```

Expected: FAIL — `Unresolved reference: LocaleController`.

- [ ] **Step 5: Write the implementation**

`ui/LocaleController.kt`:

```kotlin
package vn.hocdantranh.guzheng.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import vn.hocdantranh.guzheng.data.prefs.AppPrefs

/**
 * Applies the in-app language choice.
 *
 * The app does NOT follow system locale — iOS ships an explicit vi/en switch in
 * Settings and Android must behave identically. AppCompat's per-app locale API
 * is backported to API 21, so this works on the full minSdk 24 range.
 */
object LocaleController {

    fun apply(code: String) {
        val locales = if (code.isBlank()) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(code)
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }

    fun currentLanguage(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (locales.isEmpty) return AppPrefs.DEFAULT
        return locales[0]?.language ?: AppPrefs.DEFAULT
    }
}
```

- [ ] **Step 6: Run test to verify it passes**

```bash
./gradlew :app:testDebugUnitTest --tests "*LocaleControllerTest*"
```

Expected: PASS, 3 tests.

- [ ] **Step 7: Commit**

```bash
cd /Users/kelvin/Code/AppHocDanTranh
git add android/
git commit -m "feat(android): add vi/en string resources and in-app locale switching"
```

---

### Task 5: Five-tab navigation shell

**Files:**
- Create: `android/app/src/main/java/vn/hocdantranh/guzheng/ui/MainScaffold.kt`
- Create: `android/app/src/main/java/vn/hocdantranh/guzheng/ui/Destinations.kt`
- Create: `android/app/src/main/java/vn/hocdantranh/guzheng/ui/PlaceholderScreen.kt`
- Modify: `android/app/src/main/java/vn/hocdantranh/guzheng/MainActivity.kt`
- Test: `android/app/src/androidTest/java/vn/hocdantranh/guzheng/ui/MainScaffoldTest.kt`

**Interfaces:**
- Consumes: `DanThanhTheme` (Task 2), string resources (Task 4), `AppPrefs` + `LocaleController` (Tasks 3–4).
- Produces:
  - `enum class Destination(val route: String, val labelRes: Int, val icon: ImageVector)` with entries `LESSONS, VIDEO, TUNER, SHEET, SETTINGS` in that order
  - `@Composable fun MainScaffold()`
  - `@Composable fun PlaceholderScreen(title: String)`

  Plan 2 replaces each `PlaceholderScreen` call with a real screen composable, keeping `Destination` unchanged.

- [ ] **Step 1: Write the destinations**

`ui/Destinations.kt`:

```kotlin
package vn.hocdantranh.guzheng.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import vn.hocdantranh.guzheng.R

/**
 * Tab order mirrors iOS MainTabView exactly:
 * Lessons -> Video -> Tuner -> Sheet -> Settings.
 * Do not reorder — users switching platforms rely on muscle memory.
 */
enum class Destination(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector
) {
    LESSONS("lessons", R.string.tab_lessons, Icons.AutoMirrored.Filled.MenuBook),
    VIDEO("video", R.string.tab_video, Icons.Filled.PlayCircle),
    TUNER("tuner", R.string.tab_tuner, Icons.Filled.GraphicEq),
    SHEET("sheet", R.string.tab_sheet, Icons.Filled.Description),
    SETTINGS("settings", R.string.tab_settings, Icons.Filled.Settings)
}
```

- [ ] **Step 2: Write the placeholder screen**

`ui/PlaceholderScreen.kt`:

```kotlin
package vn.hocdantranh.guzheng.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import vn.hocdantranh.guzheng.R

/** Temporary tab body. Plan 2 replaces every usage with the real screen. */
@Composable
fun PlaceholderScreen(title: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium)
        Text(
            text = stringResource(R.string.screen_coming_soon),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
```

- [ ] **Step 3: Write the failing UI test**

`android/app/src/androidTest/java/vn/hocdantranh/guzheng/ui/MainScaffoldTest.kt`:

```kotlin
package vn.hocdantranh.guzheng.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import org.junit.Rule
import org.junit.Test
import vn.hocdantranh.guzheng.R
import vn.hocdantranh.guzheng.ui.theme.DanThanhTheme

class MainScaffoldTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    /**
     * Labels are resolved from resources, never hardcoded. The emulator's system
     * locale decides whether values/ (vi) or values-en/ wins, and a US-locale
     * emulator would fail hardcoded Vietnamese assertions.
     */
    private fun label(resId: Int): String = context.getString(resId)

    private fun launch() {
        composeRule.setContent {
            DanThanhTheme { MainScaffold() }
        }
    }

    @Test
    fun showsAllFiveTabsInIosOrder() {
        launch()
        composeRule.onNodeWithText(label(R.string.tab_lessons)).assertIsDisplayed()
        composeRule.onNodeWithText(label(R.string.tab_video)).assertIsDisplayed()
        composeRule.onNodeWithText(label(R.string.tab_tuner)).assertIsDisplayed()
        composeRule.onNodeWithText(label(R.string.tab_sheet)).assertIsDisplayed()
        composeRule.onNodeWithText(label(R.string.tab_settings)).assertIsDisplayed()
    }

    @Test
    fun lessonsTabIsSelectedOnLaunch() {
        launch()
        composeRule.onNodeWithText(label(R.string.tab_lessons)).assertIsSelected()
    }

    @Test
    fun tappingTunerSelectsTunerTab() {
        launch()
        composeRule.onNodeWithText(label(R.string.tab_tuner)).performClick()
        composeRule.onNodeWithText(label(R.string.tab_tuner)).assertIsSelected()
    }

    @Test
    fun vietnameseResourcesCarryTheIosWording() {
        val vi = context.createConfigurationContext(
            android.content.res.Configuration(context.resources.configuration).apply {
                setLocale(java.util.Locale("vi"))
            }
        )
        org.junit.Assert.assertEquals("Khóa học", vi.getString(R.string.tab_lessons))
        org.junit.Assert.assertEquals("Lên dây", vi.getString(R.string.tab_tuner))
        org.junit.Assert.assertEquals("Cài đặt", vi.getString(R.string.tab_settings))
    }
}
```

- [ ] **Step 4: Start an emulator, then run the test to verify it fails**

No device is currently attached. Create and boot one first:

```bash
export ANDROID_HOME=~/Library/Android/sdk
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager "system-images;android-36;google_apis;arm64-v8a"
$ANDROID_HOME/cmdline-tools/latest/bin/avdmanager create avd -n dantranh_test \
  -k "system-images;android-36;google_apis;arm64-v8a" --device "pixel_6"
$ANDROID_HOME/emulator/emulator -avd dantranh_test -no-snapshot -no-audio &
$ANDROID_HOME/platform-tools/adb wait-for-device
$ANDROID_HOME/platform-tools/adb devices
```

Expected: one device listed as `device` (not `offline`).

```bash
cd /Users/kelvin/Code/AppHocDanTranh/android
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:connectedDebugAndroidTest --tests "*MainScaffoldTest*"
```

Expected: FAIL — `Unresolved reference: MainScaffold`.

- [ ] **Step 5: Write the scaffold**

`ui/MainScaffold.kt`:

```kotlin
package vn.hocdantranh.guzheng.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import vn.hocdantranh.guzheng.ui.theme.VintageColors

@Composable
fun MainScaffold() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: Destination.LESSONS.route

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = VintageColors.paperWarm) {
                Destination.entries.forEach { destination ->
                    val label = stringResource(destination.labelRes)
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            if (currentRoute != destination.route) {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = label) },
                        label = { Text(label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = VintageColors.sageDark,
                            selectedTextColor = VintageColors.sageDark,
                            indicatorColor = VintageColors.sageSoft,
                            unselectedIconColor = VintageColors.inkMuted,
                            unselectedTextColor = VintageColors.inkMuted
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.LESSONS.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            Destination.entries.forEach { destination ->
                composable(destination.route) {
                    PlaceholderScreen(title = stringResource(destination.labelRes))
                }
            }
        }
    }
}
```

- [ ] **Step 6: Run the test to verify it passes**

```bash
./gradlew :app:connectedDebugAndroidTest --tests "*MainScaffoldTest*"
```

Expected: PASS, 4 tests.

- [ ] **Step 7: Wire the scaffold into MainActivity with language applied at startup**

Replace `MainActivity.kt` entirely:

```kotlin
package vn.hocdantranh.guzheng

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import vn.hocdantranh.guzheng.data.prefs.AppPrefs
import vn.hocdantranh.guzheng.ui.LocaleController
import vn.hocdantranh.guzheng.ui.MainScaffold
import vn.hocdantranh.guzheng.ui.theme.DanThanhTheme

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Apply the stored language unconditionally.
        //
        // This must NOT be conditional on the current locale. With no per-app locale
        // set, Android follows the SYSTEM locale — so an English phone would show
        // English even though the stored preference is "vi". iOS defaults to
        // Vietnamese regardless of device language, and Android must match.
        // AppCompat no-ops internally when the locale is already correct.
        lifecycleScope.launch {
            val stored = AppPrefs(applicationContext).language.first()
            LocaleController.apply(stored)
        }

        setContent {
            DanThanhTheme {
                MainScaffold()
            }
        }
    }
}
```

- [ ] **Step 8: Install and verify on the emulator by hand**

```bash
./gradlew :app:installDebug
~/Library/Android/sdk/platform-tools/adb shell am start -n vn.hocdantranh.guzheng/.MainActivity
~/Library/Android/sdk/platform-tools/adb exec-out screencap -p > /tmp/dantranh-shell.png
```

Open `/tmp/dantranh-shell.png` and confirm: cream/paper background, five tabs in the order
Khóa học / Video / Lên dây / Sheet / Cài đặt, sage-green selection on the first tab.

The labels must be **Vietnamese even though the emulator's system language is English** —
that is the whole point of applying the stored locale unconditionally in Step 7. If you see
"Lessons / Video / Tuner / Sheet / Settings", the locale is not being applied and the app is
following the system instead of the stored preference. Fix that before committing.

- [ ] **Step 9: Commit**

```bash
cd /Users/kelvin/Code/AppHocDanTranh
git add android/
git commit -m "feat(android): add five-tab navigation shell matching iOS tab order"
```

---

### Task 6: Firebase Firestore connected

**Files:**
- Create: `android/app/google-services.json` (downloaded from Firebase Console)
- Create: `android/app/src/main/java/vn/hocdantranh/guzheng/DanThanhApp.kt` (replace Task 1 placeholder)
- Test: `android/app/src/androidTest/java/vn/hocdantranh/guzheng/FirestoreConnectionTest.kt`
- Modify: `android/app/build.gradle.kts`, `android/build.gradle.kts`, `android/gradle/libs.versions.toml`

**Interfaces:**
- Consumes: Task 1's Gradle project.
- Produces: initialized Firebase in the `apphocdantranh` project; `FirebaseFirestore.getInstance()` usable app-wide. Plan 2's repositories depend on this.

- [ ] **Step 1: Register the Android app in the existing Firebase project**

In the Firebase Console, open the **existing** `apphocdantranh` project (do not create a new one) → Add app → Android:

- Package name: `vn.hocdantranh.guzheng`
- App nickname: `Dan Thanh Android`
- Debug signing SHA-1: leave blank for now (only needed for Auth/Dynamic Links, neither of which this app uses)

Download `google-services.json` and place it at `android/app/google-services.json`.

Verify it points at the right project:

```bash
grep -o '"project_id": "[^"]*"' /Users/kelvin/Code/AppHocDanTranh/android/app/google-services.json
```

Expected: `"project_id": "apphocdantranh"`

- [ ] **Step 2: Add the Firebase plugin and dependencies**

In `libs.versions.toml`, under `[versions]`:

```toml
googleServices = "4.4.2"
firebaseBom = "33.7.0"
```

under `[libraries]`:

```toml
firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebaseBom" }
firebase-firestore = { group = "com.google.firebase", name = "firebase-firestore-ktx" }
```

under `[plugins]`:

```toml
google-services = { id = "com.google.gms.google-services", version.ref = "googleServices" }
```

In `android/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.services) apply false
}
```

In `android/app/build.gradle.kts`, add to the `plugins` block:

```kotlin
    alias(libs.plugins.google.services)
```

and to `dependencies`:

```kotlin
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
```

- [ ] **Step 3: Write the failing connection test**

This is an integration test — it hits the real Firestore and requires network plus a booted emulator. It reads the `sheets` collection the iOS app already populates.

`android/app/src/androidTest/java/vn/hocdantranh/guzheng/FirestoreConnectionTest.kt`:

```kotlin
package vn.hocdantranh.guzheng

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FirestoreConnectionTest {

    @Before
    fun setUp() {
        FirebaseApp.initializeApp(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun initializesAgainstTheSharedProject() {
        val app = FirebaseApp.getInstance()
        assertEquals("apphocdantranh", app.options.projectId)
    }

    @Test
    fun readsTheSheetsCollectionWrittenByTheIosApp() = runBlocking {
        val snapshot = FirebaseFirestore.getInstance()
            .collection("sheets")
            .limit(1)
            .get()
            .await()
        assertTrue(
            "sheets collection returned no documents — check Firestore rules allow unauthenticated reads",
            snapshot.documents.isNotEmpty()
        )
    }
}
```

Add the coroutines-play-services dependency so `.await()` resolves. In `libs.versions.toml` under `[libraries]`:

```toml
kotlinx-coroutines-play-services = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-play-services", version.ref = "coroutines" }
```

In `android/app/build.gradle.kts` `dependencies`:

```kotlin
    implementation(libs.kotlinx.coroutines.play.services)
```

- [ ] **Step 4: Run the test to verify it fails**

Run before adding `google-services.json` is not possible — the plugin fails the build without it. So run now, with the file in place:

```bash
cd /Users/kelvin/Code/AppHocDanTranh/android
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:connectedDebugAndroidTest --tests "*FirestoreConnectionTest*"
```

Expected: FAIL on `readsTheSheetsCollectionWrittenByTheIosApp` — `PERMISSION_DENIED` if Firestore rules block unauthenticated reads, which is the informative failure this test exists to surface.

If it fails with `PERMISSION_DENIED`, check the Firestore rules in the console. The iOS app reads `sheets` and `videos` without authentication, so rules must already allow it — if this fails, the Android app is being blocked by something iOS is not, and that must be resolved before Plan 2.

- [ ] **Step 5: Write the Application class**

Replace `DanThanhApp.kt`:

```kotlin
package vn.hocdantranh.guzheng

import android.app.Application
import com.google.firebase.FirebaseApp

class DanThanhApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
```

- [ ] **Step 6: Run the test to verify it passes**

```bash
./gradlew :app:connectedDebugAndroidTest --tests "*FirestoreConnectionTest*"
```

Expected: PASS, 2 tests. This proves the Android app reads the same live Firestore data the iOS app writes — the single most important assumption in the whole port.

- [ ] **Step 7: Decide on committing google-services.json, then commit**

`google-services.json` contains a project ID and an API key. The key is not a secret in the security sense — it is shipped inside every APK and Firestore access is governed by security rules, not by key secrecy. Committing it is standard practice and keeps builds reproducible. This repo has no public remote today.

Commit it:

```bash
cd /Users/kelvin/Code/AppHocDanTranh
git add android/
git commit -m "feat(android): connect Firestore to shared apphocdantranh project"
```

If you later make this repo public and would rather not publish the file, remove it from tracking and inject it in CI instead — but note it will still be extractable from any published APK.

---

### Task 7: Signed release AAB on a Play Console closed track

This is the task that starts the mandatory 14-day clock. Everything above exists to make this possible in week 1.

**Files:**
- Create: `android/keystore.properties` (git-ignored)
- Create: `~/keystores/dantranh-release.jks` (stored **outside** the repo)
- Modify: `android/app/build.gradle.kts`
- Modify: `.gitignore`

**Interfaces:**
- Consumes: everything from Tasks 1–6.
- Produces: `android/app/build/outputs/bundle/release/app-release.aab`, signed with the permanent upload key.

- [ ] **Step 1: Generate the upload keystore**

**Losing this file means you can never update the app again.** Back it up somewhere durable (password manager, encrypted cloud) the moment it exists.

```bash
mkdir -p ~/keystores
"/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/keytool" \
  -genkeypair -v \
  -keystore ~/keystores/dantranh-release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias dantranh-upload
```

Answer the prompts; use a strong password and record it in your password manager.

- [ ] **Step 2: Reference the keystore without committing secrets**

`android/keystore.properties`:

```properties
storeFile=/Users/kelvin/keystores/dantranh-release.jks
storePassword=<the password you just set>
keyAlias=dantranh-upload
keyPassword=<the key password you just set>
```

Append to `/Users/kelvin/Code/AppHocDanTranh/.gitignore`:

```gitignore
android/keystore.properties
```

Verify it is ignored before going further:

```bash
cd /Users/kelvin/Code/AppHocDanTranh
git check-ignore -v android/keystore.properties
```

Expected: a line naming `.gitignore` and the pattern. If this prints nothing, **stop** — the file is not ignored and you are about to commit a signing password.

- [ ] **Step 3: Wire signing into the release build**

At the very top of `android/app/build.gradle.kts`, above the `plugins` block:

```kotlin
import java.util.Properties

val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) {
        keystorePropsFile.inputStream().use { load(it) }
    }
}
```

Inside the `android { }` block, before `buildTypes`:

```kotlin
    signingConfigs {
        create("release") {
            if (keystoreProps.isNotEmpty()) {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }
```

and in `buildTypes { release { ... } }` add:

```kotlin
            signingConfig = signingConfigs.getByName("release")
```

- [ ] **Step 4: Build the release bundle**

```bash
cd /Users/kelvin/Code/AppHocDanTranh/android
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:bundleRelease
ls -la app/build/outputs/bundle/release/app-release.aab
```

Expected: `BUILD SUCCESSFUL` and the `.aab` file exists.

Confirm it is signed, not unsigned:

```bash
"/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin/jarsigner" \
  -verify -verbose app/build/outputs/bundle/release/app-release.aab | tail -5
```

Expected: `jar verified.`

- [ ] **Step 5: Verify the release build actually runs**

R8 minification is on. A release build can pass compilation and still crash at runtime from over-aggressive shrinking, so never upload without launching it:

```bash
./gradlew :app:installRelease
~/Library/Android/sdk/platform-tools/adb shell am start -n vn.hocdantranh.guzheng/.MainActivity
~/Library/Android/sdk/platform-tools/adb logcat -d -s AndroidRuntime | tail -20
```

Expected: app launches to the five-tab shell, and `logcat` shows no `FATAL EXCEPTION`.

- [ ] **Step 6: Create the Play Console app and upload**

In Play Console:

1. Create app → name `Đàn Tranh`, default language Vietnamese, type App, Free.
2. Testing → **Closed testing** → create a track → create an email list with **12 tester Google accounts**.
3. Upload `app-release.aab` to that track.
4. Complete the minimum required declarations to activate the track: app access, ads, content rating, target audience, data safety, privacy policy URL.
5. Roll out to the closed track.

**The 14-day clock starts only when testers are actually opted in and the release is live on the track**, and requires 12 testers continuously enrolled for 14 days. Confirm each tester has accepted the opt-in link — an invited-but-not-joined tester does not count.

- [ ] **Step 7: Commit**

```bash
cd /Users/kelvin/Code/AppHocDanTranh
git add .gitignore android/app/build.gradle.kts
git status --short
git commit -m "build(android): add release signing config for Play Console uploads"
```

Confirm from `git status --short` that `keystore.properties` is **not** in the staged set before committing.

---

## Definition of done for Plan 1

- [ ] `./gradlew :app:testDebugUnitTest` passes (12 unit tests across Tasks 2–4)
- [ ] `./gradlew :app:connectedDebugAndroidTest` passes (6 instrumented tests across Tasks 5–6)
- [ ] The app launches on an emulator showing five tabs in iOS order with the vintage palette
- [ ] The app shows Vietnamese on a device whose system language is English (proves the
      locale default matches iOS rather than following the system)
- [ ] Firestore reads the live `sheets` collection written by the iOS app
- [ ] A signed AAB is live on a Play Console closed track with 12 testers opted in
- [ ] The upload keystore is backed up outside the repository
- [ ] Nothing under `HocDanTranh-Guzheng-DanThanh/` was modified

## What Plan 1 deliberately does not do

Deferred to later plans, listed here so they are not mistaken for oversights:

- Real screen content, Firestore repositories, models — Plan 2
- Play Billing, products, purchase gating — Plan 3
- Tuner, audio permissions, TarsosDSP — Plan 4
- Media3 video, watermark, `FLAG_SECURE`, YouTube, Settings UI, adaptive launcher icons, store listing assets — Plan 5
- Asset export from `Assets.xcassets` — Plan 2, when screens need the images
