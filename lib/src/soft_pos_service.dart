import 'dart:async';

import 'package:flutter/services.dart';

import 'soft_pos_result.dart';

class SoftPosService {
  static const _methodChannel = MethodChannel(
    'com.elitesoft.softpos_sdk/channel',
  );
  static const _eventChannel = EventChannel('com.elitesoft.softpos_sdk/events');

  StreamSubscription<dynamic>? _subscription;
  StreamController<SoftPosResult>? _controller;

  bool get isInitialized => _controller != null;

  Stream<SoftPosResult> get stream {
    assert(_controller != null, 'Call init() before accessing stream.');
    return _controller!.stream;
  }

  /// [msaPackage] is the MineSec SoftPOS app package to talk to — the caller
  /// picks prod vs stage (e.g. 'com.minesec.tabadul' vs '.stage').
  Future<void> init({required String msaPackage}) async {
    _controller = StreamController<SoftPosResult>.broadcast();

    _subscription = _eventChannel.receiveBroadcastStream().listen(
      (dynamic event) {
        final map = event as Map<dynamic, dynamic>;
        final isSuccess = map['isSuccess'] as bool? ?? false;

        if (isSuccess) {
          final dataMap = map['data'] as Map<dynamic, dynamic>?;
          if (dataMap != null) {
            _controller?.add(
              SoftPosSuccess(SoftPosTransactionData.fromMap(dataMap)),
            );
          } else {
            _controller?.add(
              const SoftPosFailure(
                errorMessage: 'Success response missing data',
              ),
            );
          }
        } else {
          _controller?.add(
            SoftPosFailure(
              errorCode: map['errorCode'] as String?,
              errorMessage: map['errorMessage'] as String?,
            ),
          );
        }
      },
      onError: (Object error) {
        _controller?.add(SoftPosFailure(errorMessage: error.toString()));
      },
    );

    await _methodChannel.invokeMethod<void>('init', {'msaPackage': msaPackage});
  }

  Future<bool> isSoftPosInstalled() async {
    try {
      return await _methodChannel.invokeMethod<bool>('isSoftPosInstalled') ??
          false;
    } catch (_) {
      return false;
    }
  }

  Future<void> startSaleTransaction({
    required double amount,
    required String posMessageId,
    bool autoDismissResult = true,
  }) async {
    await _methodChannel.invokeMethod<void>('startSaleTransaction', {
      'amount': amount.toString(),
      'posMessageId': posMessageId,
      'autoDismissResult': autoDismissResult,
    });
  }

  Future<void> dispose() async {
    await _subscription?.cancel();
    await _controller?.close();
    _subscription = null;
    _controller = null;
  }
}
