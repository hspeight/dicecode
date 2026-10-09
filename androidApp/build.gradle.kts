plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.masterdice.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.masterdice.app"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.activity)
}

// :shared uses com.android.kotlin.multiplatform.library, whose variant API
// exposes no assets source dir, so the Compose plugin can't package
// shared/src/commonMain/composeResources into the APK itself. Copy them here,
// under the path the generated Res class expects
// (composeResources/<packageOfResClass>/...) and register the output as
// generated assets for every variant.
val syncSharedComposeAssets = tasks.register<Sync>("syncSharedComposeAssets") {
    from("../shared/src/commonMain/composeResources") {
        into("composeResources/com.masterdice.resources")
    }
    into(layout.buildDirectory.dir("sharedComposeAssets"))
}

androidComponents {
    onVariants(selector().all()) { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(
            syncSharedComposeAssets,
            { it.destinationDirectory },
        )
    }
}
