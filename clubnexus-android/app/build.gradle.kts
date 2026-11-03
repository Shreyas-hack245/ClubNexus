import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val developmentApiUrl = providers.gradleProperty("clubnexusDevApiUrl")
    .orElse("http://10.0.2.2:4000/api/")
    .get()
val productionApiUrl = providers.gradleProperty("clubnexusProdApiUrl")
    .orElse(providers.environmentVariable("CLUBNEXUS_PROD_API_URL"))
    .orElse("")
    .get()

fun quotedBuildConfig(value: String): String = "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

android {
    namespace = "com.clubnexus.android"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.clubnexus.android"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    flavorDimensions += "environment"
    productFlavors {
        create("development") {
            dimension = "environment"
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev"
            buildConfigField("String", "API_BASE_URL", quotedBuildConfig(developmentApiUrl))
        }
        create("production") {
            dimension = "environment"
            buildConfigField("String", "API_BASE_URL", quotedBuildConfig(productionApiUrl))
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.navigation:navigation-compose:2.9.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.coil-kt:coil-compose:2.7.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}

tasks.matching { it.name == "assembleProductionRelease" || it.name == "bundleProductionRelease" }.configureEach {
    doFirst {
        require(productionApiUrl.startsWith("https://") && productionApiUrl.endsWith('/')) {
            "Set -PclubnexusProdApiUrl=https://your-api-host/api/ or CLUBNEXUS_PROD_API_URL before making a production release."
        }
    }
}
