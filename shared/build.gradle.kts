plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

kotlin {
    android {
        namespace = "com.masterdice"
        compileSdk = 37
        minSdk = 24
        withHostTestBuilder {}
    }

    jvm("desktop")

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "MasterDiceShared"
            isStatic = false
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.compose.runtime)
            api(libs.compose.foundation)
            api(libs.compose.material)
            api(libs.compose.ui)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.lifecycle.runtime.compose)
            implementation(libs.okio)
            implementation(libs.compose.components.resources)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.okio.fakefilesystem)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
        getByName("desktopTest").kotlin.srcDir(rootProject.file("tools/puzzles"))
        getByName("desktopTest").dependencies {
            implementation(libs.compose.ui.test)
            implementation(compose.desktop.currentOs)
        }
    }
}

compose.resources {
    packageOfResClass = "com.masterdice.resources"
    publicResClass = false
    generateResClass = always
}

// Regenerates review artifacts only; never overwrites the shipped bank or stable puzzle IDs.
tasks.register<JavaExec>("generateTwoDecoyBank") {
    dependsOn("desktopTestClasses")
    classpath = tasks.named<Test>("desktopTest").get().classpath
    mainClass.set("com.masterdice.game.GenerateTwoDecoyBankKt")
    args(rootProject.layout.buildDirectory.dir("two-decoy-bank").get().asFile.absolutePath)
}

// Exhaustive audit of all 376,992 five-die deals for four-position count-only play.
tasks.register<JavaExec>("validateFourPositionDeals") {
    dependsOn("desktopTestClasses")
    classpath = tasks.named<Test>("desktopTest").get().classpath
    mainClass.set("com.masterdice.game.ValidateFourPositionDealsKt")
    args(rootProject.layout.buildDirectory.dir("four-position-validation").get().asFile.absolutePath)
}
