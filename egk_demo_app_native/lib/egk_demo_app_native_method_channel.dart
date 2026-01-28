import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'egk_demo_app_native_platform_interface.dart';
import 'models/egk_data.dart';

/// An implementation of [EgkDemoAppNativePlatform] that uses method channels.
class MethodChannelEgkDemoAppNative extends EgkDemoAppNativePlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('egk_demo_app_native');

  /// Event channel for NFC session state updates
  @visibleForTesting
  final eventChannel = const EventChannel('egk_demo_app_native/events');

  StreamController<NfcSessionState>? _stateController;

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>(
      'getPlatformVersion',
    );
    return version;
  }

  @override
  Future<bool> isNfcAvailable() async {
    final result = await methodChannel.invokeMethod<bool>('isNfcAvailable');
    return result ?? false;
  }

  @override
  Future<bool> isNfcEnabled() async {
    final result = await methodChannel.invokeMethod<bool>('isNfcEnabled');
    return result ?? false;
  }

  @override
  Future<EgkData> readEgkData(String can) async {
    try {
      final result = await methodChannel.invokeMethod<Map<dynamic, dynamic>>(
        'readEgkData',
        {'can': can},
      );

      if (result == null) {
        throw PlatformException(
          code: 'NULL_RESULT',
          message: 'No data received from NFC reader',
        );
      }

      return EgkData.fromMap(Map<String, dynamic>.from(result));
    } on PlatformException catch (e) {
      throw PlatformException(
        code: e.code,
        message: e.message ?? 'Failed to read eGK data',
        details: e.details,
      );
    }
  }

  @override
  Future<void> cancelSession() async {
    await methodChannel.invokeMethod<void>('cancelSession');
  }

  @override
  Stream<NfcSessionState> get sessionStateStream {
    _stateController ??= StreamController<NfcSessionState>.broadcast();

    eventChannel.receiveBroadcastStream().listen(
      (event) {
        if (event is String) {
          final state = _parseSessionState(event);
          _stateController?.add(state);
        }
      },
      onError: (error) {
        _stateController?.addError(error);
      },
    );

    return _stateController!.stream;
  }

  NfcSessionState _parseSessionState(String state) {
    switch (state) {
      case 'idle':
        return NfcSessionState.idle;
      case 'discovering':
        return NfcSessionState.discovering;
      case 'connecting':
        return NfcSessionState.connecting;
      case 'establishingSecureChannel':
        return NfcSessionState.establishingSecureChannel;
      case 'reading':
        return NfcSessionState.reading;
      case 'success':
        return NfcSessionState.success;
      case 'error':
        return NfcSessionState.error;
      default:
        return NfcSessionState.idle;
    }
  }
}
