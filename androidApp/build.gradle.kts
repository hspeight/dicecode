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
