#!/bin/bash
# Assemble le projet Android à partir des fichiers à plat, juste avant la compilation.
set -e

mkdir -p app/src/main/java/com/meslinks/app app/src/main/assets app/src/main/res/drawable

cp MainActivity.java app/src/main/java/com/meslinks/app/MainActivity.java
cp AndroidManifest.xml app/src/main/AndroidManifest.xml
cp index.html app/src/main/assets/index.html

cat > settings.gradle <<'EOF'
pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "MesLiens"
include ':app'
EOF

cat > build.gradle <<'EOF'
plugins {
    id 'com.android.application' version '8.5.2' apply false
}
EOF

cat > app/build.gradle <<'EOF'
plugins {
    id 'com.android.application'
}
android {
    namespace 'com.meslinks.app'
    compileSdk 34
    defaultConfig {
        applicationId 'com.meslinks.app'
        minSdk 24
        targetSdk 34
        versionCode 1
        versionName '1.0'
    }
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
}
EOF

# Icône : fond bleu nuit, maillons de chaîne jaunes
cat > app/src/main/res/drawable/ic_launcher.xml <<'EOF'
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#0B1B3A" android:pathData="M0,0h108v108h-108z"/>
    <path android:strokeColor="#FFD60A" android:strokeWidth="7" android:strokeLineCap="round"
        android:pathData="M30,44h22a10,10 0 0 1 0,20h-22a10,10 0 0 1 0,-20z"/>
    <path android:strokeColor="#FFD60A" android:strokeWidth="7" android:strokeLineCap="round"
        android:pathData="M56,44h22a10,10 0 0 1 0,20h-22a10,10 0 0 1 0,-20z"/>
    <path android:strokeColor="#FFD60A" android:strokeWidth="7" android:strokeLineCap="round"
        android:pathData="M46,54h16"/>
</vector>
EOF
