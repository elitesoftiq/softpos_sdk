# softpos_sdk

Android Flutter plugin for the [MineSec](https://theminesec.com) SoftPOS
(`poslib`): **sale**, **warm-up**, **activation**, and an `isSoftPosInstalled()`
check. You choose which MineSec app to drive (prod vs stage) by passing its
package name, so the same plugin works across projects and environments.

> Requires `minSdk` 26+. Android only.

## Setup

### 1. Add the dependency

```yaml
dependencies:
  softpos_sdk:
    git:
      url: https://github.com/elitesoftiq/softpos_sdk
```

### 2. Add your MineSec credentials

`poslib` is on a private registry. Create `android/key.properties` (the same
gitignored file the release keystore uses — never commit it) and add:

```properties
MINESEC_REGISTRY_LOGIN=minesec-product-support
MINESEC_REGISTRY_TOKEN=<token from MineSec>
```

The build reads these automatically and fails with this reminder if they're missing.

### 3. Use `FlutterFragmentActivity`

In `android/app/src/main/kotlin/.../MainActivity.kt`, extend
`FlutterFragmentActivity` (not `FlutterActivity` — the MineSec contracts need it):

```kotlin
import io.flutter.embedding.android.FlutterFragmentActivity

class MainActivity : FlutterFragmentActivity()
```

### 4. Declare the MineSec app(s) in the manifest

Android 11+ hides other apps unless you list them. In
`android/app/src/main/AndroidManifest.xml`, add a `<queries>` block as a direct
child of `<manifest>` (a sibling of `<application>`), one `<package>` per env:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <queries>
        <package android:name="com.minesec.tabadul" />
        <package android:name="com.minesec.tabadul.stage" />
    </queries>

    <application ...>
        ...
    </application>
</manifest>
```

## Usage

```dart
import 'package:softpos_sdk/softpos_sdk.dart';

final svc = SoftPosService();

// 1. init with the MineSec app package you want to talk to
await svc.init(msaPackage: 'com.minesec.tabadul');

// 2. listen for results
svc.stream.listen((r) {
  switch (r) {
    case SoftPosSuccess(:final data): print('OK: ${data.rrn}');
    case SoftPosFailure(): print('Failed: ${r.errorMessage}');
  }
});

// 3. run a sale (result arrives on the stream above)
await svc.startSaleTransaction(amount: 12.50, posMessageId: 'order-42');

// 4. clean up when done
await svc.dispose();
```

See [`example/`](example/) for a full runnable app.

## prod vs stage

`init(msaPackage:)` takes a plain string, so pick the env however you like.

**Build-time** (separate builds, simplest):

```dart
const msaPackage = String.fromEnvironment(
  'SOFTPOS_MSA_PACKAGE',
  defaultValue: 'com.minesec.tabadul', // prod
);
await svc.init(msaPackage: msaPackage);
```

```sh
flutter run --dart-define=SOFTPOS_MSA_PACKAGE=com.minesec.tabadul.stage
```

**Runtime** (one build, value from a server/setting/toggle):

```dart
final env = await fetchEnv(); // 'prod' | 'stage'
await svc.init(msaPackage: env == 'stage'
    ? 'com.minesec.tabadul.stage'
    : 'com.minesec.tabadul');
```

To switch env *after* the first init, dispose then re-init (calling `init`
twice leaks the listener):

```dart
await svc.dispose();
await svc.init(msaPackage: newPackage);
```

Either way, keep both `<package>` lines in step 4 so the chosen app is visible.
