#!/bin/bash
#
# SyncU Project Generator
# Generates complete Android project with SyncU branding
#

set -e

PROJECT_DIR="${1:-./SyncU}"

echo "Creating SyncU Android Project at: $PROJECT_DIR"
echo "================================================"

# Create directory structure
echo "→ Creating directories..."
mkdir -p "$PROJECT_DIR"/{app/src/main/{java/com/syncu/{ui,data,sync,api,utils},res/{layout,values,mipmap-hdpi,mipmap-mdpi,mipmap-xhdpi,mipmap-xxhdpi,mipmap-xxxhdpi}},gradle/wrapper}

# Root build.gradle
echo "→ Creating root build.gradle..."
cat > "$PROJECT_DIR/build.gradle" << 'GRADLE_ROOT'
buildscript {
    ext.kotlin_version = '1.9.20'
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath 'com.android.tools.build:gradle:8.1.2'
        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version"
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

task clean(type: Delete) {
    delete rootProject.buildDir
}
GRADLE_ROOT

# settings.gradle
echo "→ Creating settings.gradle..."
cat > "$PROJECT_DIR/settings.gradle" << 'GRADLE_SETTINGS'
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "SyncU"
include ':app'
GRADLE_SETTINGS

# gradle.properties
echo "→ Creating gradle.properties..."
cat > "$PROJECT_DIR/gradle.properties" << 'GRADLE_PROPS'
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true
android.useAndroidX=true
android.enableJetifier=true
kotlin.code.style=official
android.enableBuildCache=false
android.enableProfileJson=false
android.disableAutomaticComponentCreation=true
GRADLE_PROPS

# app/build.gradle
echo "→ Creating app/build.gradle..."
cat > "$PROJECT_DIR/app/build.gradle" << 'GRADLE_APP'
plugins {
    id 'com.android.application'
    id 'kotlin-android'
    id 'kotlin-kapt'
}

android {
    namespace 'com.syncu'
    compileSdk 34

    defaultConfig {
        applicationId "com.syncu"
        minSdk 26
        targetSdk 34
        versionCode 1
        versionName "1.0.0"
    }

    buildTypes {
        release {
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = '17'
    }

    buildFeatures {
        viewBinding true
        buildConfig true
    }
}

dependencies {
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.10.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'androidx.health.connect:connect-client:1.1.0-alpha07'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3'
    implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.6.2'
    implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2'
    implementation 'androidx.work:work-runtime-ktx:2.9.0'
    implementation 'androidx.security:security-crypto:1.1.0-alpha06'
    implementation 'com.squareup.okhttp3:okhttp:4.12.0'
    implementation 'com.squareup.okhttp3:logging-interceptor:4.12.0'
    implementation 'com.google.code.gson:gson:2.10.1'
    implementation 'androidx.room:room-runtime:2.6.0'
    implementation 'androidx.room:room-ktx:2.6.0'
    kapt 'androidx.room:room-compiler:2.6.0'
    implementation 'androidx.datastore:datastore-preferences:1.0.0'
}
GRADLE_APP

echo "→ Project structure created!"
echo "→ To complete: Copy all .kt files from original project"
echo "→ Update package names to: com.syncu"  
echo "→ Update class name: IntervalsApp → SyncUApp"
echo ""
echo "Done! Open $PROJECT_DIR in Android Studio"
EOFSCRIPT
chmod +x generate_syncu.sh
