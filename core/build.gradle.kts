plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
}

kotlin {
    explicitApi()

    androidLibrary {
        namespace = "com.omarshehe.tzaddress"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdkCore.get().toInt()

        withHostTest {}
    }
    jvm()
    iosArm64()
    iosSimulatorArm64()

    jvmToolchain(17)

    sourceSets {
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
