plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.tourbuddy.tourbuddy"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.tourbuddy.tourbuddy"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    buildFeatures{
        viewBinding = true
        dataBinding = true

    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }


}

dependencies {

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Spinner
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // Firebase
    implementation("com.google.firebase:firebase-auth:22.3.1")
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("com.google.firebase:firebase-firestore:24.10.1") // Firestore
    implementation("com.google.firebase:firebase-storage:20.3.0") // Firebase Storage


    // Image Picker
    implementation("com.github.dhaval2404:imagepicker:2.1")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("jp.wasabeef:glide-transformations:4.3.0")

    implementation("androidx.core:core-ktx:1.12.0")


    //
    implementation("com.google.code.gson:gson:2.8.9")

    // Customizable Calendar
    implementation("io.github.architshah248.calendar:awesome-calendar:2.0.0")
    implementation("com.squareup:android-times-square:1.5.0@aar")


    implementation("com.github.prolificinteractive:material-calendarview:2.0.1")
    implementation("com.jakewharton.threetenabp:threetenabp:1.3.1")

    // Color Picker
    implementation("com.github.QuadFlask:colorpicker:0.0.15")

}

