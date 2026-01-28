import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'egk_demo_app_native_method_channel.dart';
import 'models/egk_data.dart';

abstract class EgkDemoAppNativePlatform extends PlatformInterface {
  /// Constructs a EgkDemoAppNativePlatform.
  EgkDemoAppNativePlatform() : super(token: _token);

  static final Object _token = Object();

  static EgkDemoAppNativePlatform _instance = MethodChannelEgkDemoAppNative();

  /// The default instance of [EgkDemoAppNativePlatform] to use.
  ///
  /// Defaults to [MethodChannelEgkDemoAppNative].
  static EgkDemoAppNativePlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [EgkDemoAppNativePlatform] when
  /// they register themselves.
  static set instance(EgkDemoAppNativePlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }

  /// Check if NFC is available on the device
  Future<bool> isNfcAvailable() {
    throw UnimplementedError('isNfcAvailable() has not been implemented.');
  }

  /// Check if NFC is enabled on the device
  Future<bool> isNfcEnabled() {
    throw UnimplementedError('isNfcEnabled() has not been implemented.');
  }

  /// Read eGK data from the health card
  ///
  /// [can] - Card Access Number (6 digits printed on the card)
  /// Returns [EgkData] containing personal and insurance data
  Future<EgkData> readEgkData(String can) {
    throw UnimplementedError('readEgkData() has not been implemented.');
  }

  /// Cancel any ongoing NFC session
  Future<void> cancelSession() {
    throw UnimplementedError('cancelSession() has not been implemented.');
  }

  /// Stream of NFC session state updates
  Stream<NfcSessionState> get sessionStateStream {
    throw UnimplementedError('sessionStateStream has not been implemented.');
  }
}
