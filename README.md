# softpos_sdk

Android-only Flutter plugin wrapping the [MineSec](https://theminesec.com)
SoftPOS (`poslib`) **sale**, **warm-up** and **activation** flows. You pick
which MineSec SoftPOS app to drive — prod vs stage — by passing its package
name, so the same plugin works across projects and environments.

## Features

- Tap-to-pay **sale** transactions with a structured result.
- **warm-up** and **activation** calls.
- `isSoftPosInstalled()` check.
- Environment-agnostic: the MineSec app package is a parameter, not baked in.

## Install

```yaml
dependencies:
  softpos_sdk: ^1.0.0
```

## Usage

```dart
import 'package:softpos_sdk/softpos_sdk.dart';

final svc = SoftPosService();
await svc.init(msaPackage: 'com.minesec.tabadul');         // prod
// await svc.init(msaPackage: 'com.minesec.tabadul.stage'); // stage

svc.stream.listen((r) {
  switch (r) {
    case SoftPosSuccess(:final data): print(data.rrn);
    case SoftPosFailure(): print(r.errorMessage);
  }
});

await svc.startSaleTransaction(amount: 12.50, posMessageId: 'order-42');
// ... and svc.dispose() when done.
```

See [`example/`](example/) for a runnable app with a prod/stage toggle.

## Two host requirements

1. **The host Activity must be `FlutterFragmentActivity`** (not `FlutterActivity`)
   — the MineSec activity-result contracts need a `ComponentActivity`:

   ```kotlin
   class MainActivity : FlutterFragmentActivity()
   ```

2. **soft pos registry credentials.** Create `android/key.properties` in your
   app (the same gitignored file the release keystore uses — never committed)
   and add these two lines:
   ```properties
   MINESEC_REGISTRY_LOGIN=minesec-product-support
   MINESEC_REGISTRY_TOKEN=<token from MineSec>
   ```
   The build reads them automatically. If they're missing, the build stops and
   prints these same instructions.

> Requires `minSdk` 26+.
