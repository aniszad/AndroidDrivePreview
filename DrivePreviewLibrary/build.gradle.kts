import com.android.build.gradle.LibraryExtension

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id ("maven-publish")
}




android {
    namespace = "com.az.drivepreviewlibrary"
    compileSdk = 34

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
    }

    packaging {
        resources.excludes.add("META-INF/DEPENDENCIES")
        resources.excludes.add("META-INF/LICENSE")
        resources.excludes.add("META-INF/LICENSE.txt")
        resources.excludes.add("META-INF/license.txt")
        resources.excludes.add("META-INF/NOTICE")
        resources.excludes.add("META-INF/NOTICE.txt")
        resources.excludes.add("META-INF/notice.txt")
        resources.excludes.add("META-INF/ASL2.0")
        resources.excludes.add("META-INF/*.kotlin_module")
    }
}


dependencies {
    implementation ("com.google.api-client:google-api-client-android:2.4.0")
    implementation ("com.google.api-client:google-api-client-gson:2.4.0")
    implementation ("com.fasterxml.jackson.core:jackson-core:2.17.0") // Optional for JSON parsing

    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    testImplementation("junit:junit:4.13.2")

    // required if you want to use Mockito for unit tests
    testImplementation ("org.mockito:mockito-core:5.11.0")
    // required if you want to use Mockito for Android tests
    androidTestImplementation ("org.mockito:mockito-android:5.11.0")

    implementation("com.google.api-client:google-api-client-jackson2:2.4.0")

    implementation ("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // google drive :
    implementation ("com.google.android.gms:play-services-auth:21.0.0")
    implementation ("com.google.apis:google-api-services-drive:v3-rev20220815-2.0.0")
    implementation ("com.google.oauth-client:google-oauth-client-jetty:1.35.0")
    implementation ("com.google.auth:google-auth-library-oauth2-http:1.23.0")


    // SSP and SDP library
    implementation ("com.intuit.ssp:ssp-android:1.1.0")
    implementation ("com.intuit.sdp:sdp-android:1.1.0")



    testImplementation("junit:junit:4.13.2")


    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

