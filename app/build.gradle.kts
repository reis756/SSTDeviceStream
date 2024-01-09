plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.sst.sstdevicestream"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.sst.sstdevicestream"
        minSdk = 24
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    packaging {
        pickFirst("**/*.so")
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.6")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.6")
    implementation("androidx.core:core-splashscreen:1.0.1")

    //Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    //Koin
    implementation("io.insert-koin:koin-android:3.4.0")
    implementation("io.insert-koin:koin-core:3.4.0")
    testImplementation("io.insert-koin:koin-test:3.4.0")

    //Gson
    implementation("com.google.code.gson:gson:2.10.1")

    //RabbitMQ
    implementation("com.rabbitmq:amqp-client:5.18.0")

    //Location
    implementation("com.google.android.gms:play-services-location:21.0.1")

    //Speed test connection
    implementation("com.squareup.okhttp:okhttp-urlconnection:2.0.0")

    //Tensorflow
    implementation("org.tensorflow:tensorflow-lite-task-vision:0.4.0")
    // Import the GPU delegate plugin Library for GPU inference
    implementation("org.tensorflow:tensorflow-lite-gpu-delegate-plugin:0.4.0")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.9.0")

    // CameraX core library
    implementation("androidx.camera:camera-core:1.1.0-beta03")
    implementation("androidx.camera:camera-camera2:1.1.0-beta03")
    implementation("androidx.camera:camera-lifecycle:1.1.0-beta03")
    implementation("androidx.camera:camera-view:1.1.0-beta03")

    //WindowManager
    implementation("androidx.window:window:1.0.0-alpha09")

    //SRT
    implementation("io.github.thibaultbee:streampack:2.5.2")
    implementation("io.github.thibaultbee:streampack-extension-srt:2.5.2")

    //RabbitMQ
    implementation("com.rabbitmq:amqp-client:5.18.0")

    //RTSP
    //implementation("com.github.pedroSG94:RTSP-Server:1.1.9")
    //implementation("com.github.pedroSG94.RootEncoder:library:2.3.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}