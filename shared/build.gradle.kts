plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

// Generates a build stamp (commit count + short hash, e.g. "r123-a1b2c3d")
// into a commonMain source so every platform shows the same version.
val versionDir = layout.buildDirectory.dir("generated/version")
val generateVersionInfo = tasks.register("generateVersionInfo") {
    val outFile = versionDir.get().file("com/masterdice/Version.kt").asFile
    val repoDir = rootProject.projectDir
    outputs.file(outFile)
    // Cheap git calls; regenerate every build so the stamp tracks HEAD.
    outputs.upToDateWhen { false }
    doLast {
        fun git(vararg args: String): String = try {
            val p = ProcessBuilder(listOf("git") + args.toList())
                .directory(repoDir)
                .start()
            val text = p.inputStream.bufferedReader().readText().trim()
            p.waitFor()
            text
        } catch (e: Exception) {
            ""
        }
        val count = git("rev-list", "--count", "HEAD").ifEmpty { "0" }
        val hash = git("rev-parse", "--short", "HEAD").ifEmpty { "dev" }
        outFile.parentFile.mkdirs()
        outFile.writeText(
            "package com.masterdice\n\n" +
                "/** Generated at build time — do not edit. */\n" +
                "const val BUILD_VERSION = \"r$count-$hash\"\n",
        )
    }
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
        getByName("commonMain").kotlin.srcDir(versionDir)
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

tasks.matching {
    it.name.startsWith("compile") &&
        (it.name.contains("Kotlin") || it.name.contains("AndroidMain") ||
            it.name.contains("HostTest") || it.name.contains("DesktopTest"))
}.configureEach {
    dependsOn(generateVersionInfo)
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

// Generates review artifacts without changing published bank rows or their IDs.
tasks.register<JavaExec>("generateFourPositionBank") {
    dependsOn("desktopTestClasses")
    classpath = tasks.named<Test>("desktopTest").get().classpath
    mainClass.set("com.masterdice.game.GenerateFourPositionBankKt")
    args(rootProject.layout.buildDirectory.dir("four-position-bank").get().asFile.absolutePath)
}
