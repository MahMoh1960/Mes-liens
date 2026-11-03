#!/bin/bash
# Assemble le projet Android à partir des fichiers à plat, juste avant la compilation.
set -e

mkdir -p app/src/main/java/com/meslinks/app app/src/main/assets app/src/main/res/drawable app/src/main/res/xml app/src/main/res/values

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

# Icône micro (raccourci vocal)
cat > app/src/main/res/drawable/ic_mic.xml <<'EOF'
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <path android:fillColor="#0B1B3A" android:pathData="M0,0h108v108h-108z"/>
    <group android:scaleX="2.6" android:scaleY="2.6" android:translateX="23" android:translateY="23">
        <path android:fillColor="#FFD60A"
            android:pathData="M12,14c1.66,0 3,-1.34 3,-3V5c0,-1.66 -1.34,-3 -3,-3S9,3.34 9,5v6c0,1.66 1.34,3 3,3zM17.3,11c0,3 -2.54,5.1 -5.3,5.1S6.7,14 6.7,11H5c0,3.41 2.72,6.23 6,6.72V21h2v-3.28c3.28,-0.48 6,-3.3 6,-6.72h-1.7z"/>
    </group>
</vector>
EOF

cat > app/src/main/res/values/strings.xml <<'EOF'
<resources>
    <string name="voice_short">Voix</string>
    <string name="voice_long">Commande vocale</string>
</resources>
EOF

cat > app/src/main/res/xml/shortcuts.xml <<'EOF'
<shortcuts xmlns:android="http://schemas.android.com/apk/res/android">
    <shortcut
        android:shortcutId="voice"
        android:enabled="true"
        android:icon="@drawable/ic_mic"
        android:shortcutShortLabel="@string/voice_short"
        android:shortcutLongLabel="@string/voice_long">
        <intent
            android:action="com.meslinks.app.VOICE"
            android:targetPackage="com.meslinks.app"
            android:targetClass="com.meslinks.app.MainActivity" />
    </shortcut>
</shortcuts>
EOF
