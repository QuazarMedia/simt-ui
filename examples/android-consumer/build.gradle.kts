import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Base64
import java.util.zip.ZipFile

plugins {
    id("com.android.application") version "9.5.1"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21"
}

android {
    namespace = "app.simt.fixture.android"
    compileSdk = 37
    defaultConfig {
        applicationId = "app.simt.fixture.android"
        minSdk = 28
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = null
        }
    }
    testOptions { unitTests.isIncludeAndroidResources = true }
    sourceSets {
        getByName("test").kotlin.directories.add("src/smokeTest/kotlin")
        getByName("androidTest").kotlin.directories.add("src/smokeTest/kotlin")
    }
}

// AGP Compose Preview task creation requires host resource packaging for release too.
androidComponents.beforeVariants { variant ->
    variant.hostTests[com.android.build.api.variant.HostTestBuilder.UNIT_TEST_TYPE]?.enable = true
}

kotlin {
    jvmToolchain(21)
    compilerOptions { jvmTarget = JvmTarget.JVM_11 }
}

dependencies {
    implementation("app.simt:app-ui:${providers.gradleProperty("uiVersion").getOrElse("0.1.0-SNAPSHOT")}")
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.activity:activity-compose:1.13.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.17")
    testImplementation("androidx.compose.ui:ui-test-junit4:1.12.1")
    // Robolectric reads the resource APK manifest; keep the test activity out of release.
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.12.1")
    testImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.12.1")
    androidTestImplementation("androidx.compose.ui:ui-test-manifest:1.12.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}

tasks.withType<Test>().configureEach {
    jvmArgs("--add-exports=java.base/jdk.internal.access=ALL-UNNAMED")
    maxParallelForks = 1
}

// Assert the producer's resource payload survives both packaging and R8.
tasks.register("verifyPackagedLocales") {
    dependsOn("assembleDebug", "assembleRelease")
    doLast {
        listOf("debug/android-consumer-debug.apk", "release/android-consumer-release-unsigned.apk").forEach { apk ->
            ZipFile(layout.buildDirectory.file("outputs/apk/$apk").get().asFile).use { zip ->
                val expected =
                    mapOf(
                        "values" to ("Clear text" to "Close"),
                        "values-fr" to ("Effacer le texte" to "Fermer"),
                        "values-es" to ("Borrar texto" to "Cerrar"),
                        "values-ar" to ("مسح النص" to "إغلاق"),
                    )
                val resourcePaths =
                    zip.entries().asSequence()
                        .filter { it.name.endsWith("/strings.commonMain.cvr") }
                        .map { it.name }.toSet()
                check(resourcePaths == expected.keys.map { "assets/composeResources/app.simt.ui.resources/$it/strings.commonMain.cvr" }.toSet()) {
                    "Unexpected published resource namespaces or locales in $apk"
                }
                expected.forEach { (locale, labels) ->
                    val path = "assets/composeResources/app.simt.ui.resources/$locale/strings.commonMain.cvr"
                    val entry = checkNotNull(zip.getEntry(path)) { "Missing published $locale resources in $apk" }
                    val values =
                        zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { reader ->
                            reader.readLines().drop(1).associate { line ->
                                val parts = line.split('|')
                                parts[1] to String(Base64.getDecoder().decode(parts[2]), Charsets.UTF_8)
                            }
                        }
                    check(values["simt_ui_cd_clear"] == labels.first && values["simt_ui_close"] == labels.second) {
                        "Incorrect published clear/close text for $locale in $apk"
                    }
                }
            }
        }
    }
}
