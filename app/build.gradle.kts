plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("dagger.hilt.android.plugin")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.hoker.intra_example"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.hoker.intra_example"
        minSdk = 31
        targetSdk = 37
        versionCode = 162
        versionName = "1.6.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) // Update to your target Java version
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.19.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")

    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.14.0")
    implementation("androidx.hilt:hilt-navigation-compose:1.3.0")
    implementation("androidx.navigation:navigation-compose:2.9.8")
    implementation("androidx.compose.ui:ui:1.11.3")
    implementation("androidx.compose.material:material:1.11.3")
    implementation("com.google.dagger:hilt-android:2.59.2")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    ksp("com.google.dagger:hilt-compiler:2.59.2")
    implementation("androidx.constraintlayout:constraintlayout-compose-android:1.1.1")

    implementation(project(":intra"))
    implementation("com.github.h0ker:Supra:0.2.11")
}