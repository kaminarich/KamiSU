import com.android.build.api.dsl.ApplicationDefaultConfig
import com.android.build.api.dsl.CommonExtension
import com.android.build.gradle.api.AndroidBasePlugin
import java.io.ByteArrayOutputStream

plugins {
    alias(libs.plugins.agp.app) apply false
    alias(libs.plugins.agp.lib) apply false
    alias(libs.plugins.kotlin) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.lsplugin.cmaker)
}

cmaker {
    default {
        arguments.addAll(
            arrayOf(
                "-DANDROID_STL=none",
            )
        )
        abiFilters("arm64-v8a", "x86_64", "riscv64", "armeabi-v7a")
    }
    buildTypes {
        if (it.name == "release") {
            arguments += "-DDEBUG_SYMBOLS_PATH=${layout.buildDirectory.asFile.get().absolutePath}/symbols"
        }
    }
}

val androidMinSdkVersion = 26
val androidTargetSdkVersion = 36
val androidCompileSdkVersion = 36
val androidBuildToolsVersion = "36.1.0"
val androidCompileNdkVersion by extra(libs.versions.ndk.get())
val androidCmakeVersion by extra("4.1.0+")
val androidSourceCompatibility = JavaVersion.VERSION_21
val androidTargetCompatibility = JavaVersion.VERSION_21
val managerVersionCode by extra(getVersionCode())
val managerVersionName by extra(getVersionName())

extra["androidCompileNdkVersion"] = androidCompileNdkVersion
extra["androidBuildToolsVersion"] = androidBuildToolsVersion

fun getGitCommitCount(): Int {
    return try {
        val process = Runtime.getRuntime().exec(arrayOf("git", "rev-list", "--count", "HEAD"))
        process.inputStream.bufferedReader().use { it.readText().trim().toInt() }
    } catch (e: Exception) {
        1
    }
}

// [FIX] Use --abbrev=0 to get only the tag name, NOT "tag-N-gSHA".
// The variable-length commit suffix from plain "git describe" changes the
// versionName string length each commit → changes APK resource table size
// → shifts ZIP byte offsets → check_v2_signature reads certificate at
// wrong offset → EXPECTED_SIZE mismatches → hash extraction fails.
// With --abbrev=0, versionName = "v1.0" every build until you push a new tag.
// versionCode (numeric) is fine to change — it does NOT affect APK cert offset.
fun getVersionName(): String {
    return try {
        val process = Runtime.getRuntime().exec(
            arrayOf("git", "describe", "--tags", "--abbrev=0")
        )
        val tag = process.inputStream.bufferedReader().use { it.readText().trim() }
        if (tag.isNotEmpty()) tag else "KamiSU"
    } catch (e: Exception) {
        "KamiSU"
    }
}

fun getVersionCode(): Int {
    val commitCount = getGitCommitCount()
    return 30000 + commitCount
}

subprojects {
    plugins.withType(AndroidBasePlugin::class.java) {
        extensions.configure(CommonExtension::class.java) {
            compileSdk = androidCompileSdkVersion
            ndkVersion = androidCompileNdkVersion
            buildToolsVersion = androidBuildToolsVersion

            defaultConfig {
                minSdk = androidMinSdkVersion

                if (this is ApplicationDefaultConfig) {
                    targetSdk = androidTargetSdkVersion
                    versionCode = managerVersionCode
                    versionName = managerVersionName

                    // App package name
                    applicationId = "com.kamisu.manager"
                }

                ndk {
                    abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
                }
            }

            lint {
                abortOnError = true
                checkReleaseBuilds = false
            }

            compileOptions {
                sourceCompatibility = androidSourceCompatibility
                targetCompatibility = androidTargetCompatibility
            }
        }
    }
}
