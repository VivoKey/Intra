# Intra - Android NFC Library

**Intra** is an Android NFC library geared towards removing the boilerplate overhead of the Android NFC stack. Geared towards implantable NFC devices, **Intra** lets you interface with your devices for a verity of security-related features.

**Intra** is intended for use in modern, single-activity, compose oriented applications through the use of **Hilt** dependency injection. Intra requires an understanding of **Hilt** in order to use, and obviously requires **Hilt** libraries in order to use. 

## Dependencies

You'll first need to import the following dependencies in your module's `build.gradle`:

```
implementation "com.google.dagger:hilt-android:<hilt_android_version>"  
ksp "com.google.dagger:hilt-compiler:<hilt_android_version>"
implementation "androidx.hilt:hilt-navigation-compose:<hilt_navigation_compose_version>"
```

Then add the following plugins in your module's `build.gradle` under the `pluigins` section:

```
id 'dagger.hilt.android.plugin'  
id 'com.google.devtools.ksp
```

Then in your project's `build.gradle`, add the following under your `plugins` section:

```
id 'com.google.devtools.ksp' version "1.9.10-1.0.13" apply false
```

As well as the following classpath under the `dependencies` section off your project's `build.gradle`:

```
classpath 'com.google.dagger:hilt-android-gradle-plugin:2.48'
```

Finally, back in your module's `build.gradle`, add **Intra**'s import:

```
implementation "com.github.carbidecowboy:intra:<intra_version>"
```

After a quick Gradle sync, you'll now be ready to use **Intra** in your Android application.

---
