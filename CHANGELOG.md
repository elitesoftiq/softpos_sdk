## 1.0.0

- Initial release.
- `SoftPosService`: `init(msaPackage:)`, `isSoftPosInstalled()`, `startSaleTransaction(...)`, result stream.
- Caller selects the MineSec SoftPOS app package (prod vs stage) at `init()`.
- Android only; requires `FlutterFragmentActivity`.
