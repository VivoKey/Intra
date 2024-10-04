# Intra - Android NFC Library

**Intra** is an Android NFC library geared towards removing the boilerplate overhead of the Android NFC stack. Geared towards implantable NFC devices, **Intra** lets you interface with your devices for a verity of security-related features.

**Intra** is intended for use in modern, single-activity, compose oriented applications through the use of **Hilt** dependency injection. Intra requires an understanding of **Hilt** in order to use, and obviously requires **Hilt** libraries in order to use.

# Permissions

In order to use **Intra** you `AndroidManifest.xml` must include the following permissions:

```
<uses-permission android:name="android.permission.INTERNET"/>  
<uses-permission android:name="android.permission.NFC"/>
```

# Dependencies

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

# Usage

## Activity Setup

Intra provides the `NfcActivity` class that your main activity should inherit from:
```
@AndroidEntryPoint  
class IntraExampleActivity: NfcActivity() {
    ...
}
```


Now, beyond this, there are two approaches you can use in order to intercept NFC events.

## Approach 1 - MVVM/ViewModel/Compose

The first and most recommended approach is to use the MVVM pattern, which Intra is designed to integrate automatically with. In order to use this, for each ViewModel you have that you would like to perform NFC scans with, extend the `NfcViewModel` and provide the injected `NfcAdapterController` value into the constructor of `NfcViewModel`:

```
@HiltViewModel  
class IntraExampleViewModel @Inject constructor(  
    nfcAdapterController: NfcAdapterController 
): NfcViewModel(nfcAdapterController) {
    ...
}
```

Now, all you need to do in order to get NFC scan functionality, tag type fingerprinting and controller auto-injection, as well as access to all of the `NfcController` functions, is override the `onTagDiscovered()` function in your ViewModel and define your behavior:

```
override fun onNfcTagDiscovered(tag: Tag, nfcController: NfcController) {  
    ...
}
```

As soon as you create a new ViewModel that extends `NfcViewModel`, it will add it's associated `onTagDiscovered()` implementation to a stack in `NfcAdapterController`. This means that as you navigate to/from NFC enabled ViewModels and pages in your application, Intra will automatically set the current page's ViewModel as the "Active" NFC scan behavior.

## Approach 2 - Multiple Activity/Android View

If you for whatever reason really want to use the old Android View approach with fragments/activities, Intra does provide support to do so.

First, as you should have already extended `NfcActivity` in your main Activity, you can call `nfcAdapterController.setOnTagDiscoveredListener()` from your `onResume` lifecycle function. This does not track with the stack of your activities, so it will need to be placed in the `onResume` so that any previous definitions for the listener are cleared:

```
override fun onResume() {
    super.onResume()
    nfcAdapterController.setOnTagDiscoveredListener { tag, nfcController ->
        ...
    }
}
```

## Interacting with the tag

In order to use many of the functions belonging to `NfcController`, you need to first start a connection with your scanned tag. You can do so with the `NfcController.withConnection {}` function. Wrap your logic that requires an NFC connection in this function to guarantee safe handling and connection management with your tags. For example, if I want to fetch a VivoKey Auth API JWT, you would do the following:

```
override fun onTagDiscovered(tag: Tag, nfcController: NfcController) {
    viewModelScope.launch(Dispatchers.IO) {
        nfcController.withConnection(tag) {
            val result = nfcController.getVivoKeyJwt(tag)
            if (result is OperationResult.Success) {
                println(result.data)
            }
        }
    }
}
``` 

You can also see here that **Intra** takes advantage of a custom `OperationResult` class. In short, this is just a simple wrapper class that is used as a return type for many **Intra** functions. It either returns `OperationResult.Success` which then contains a `data` property that contains the enclosing data type, or it returns `OperationResult.Failure` which then contains an `exception` property for error logging purposes.