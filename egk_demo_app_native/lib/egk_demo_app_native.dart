import 'egk_demo_app_native_platform_interface.dart';
import 'models/egk_data.dart';

export 'models/egk_data.dart';

class EgkDemoAppNative {
  Future<String?> getPlatformVersion() {
    return EgkDemoAppNativePlatform.instance.getPlatformVersion();
  }

  /// Check if NFC is available on this device
  Future<bool> isNfcAvailable() {
    return EgkDemoAppNativePlatform.instance.isNfcAvailable();
  }

  /// Check if NFC is enabled
  Future<bool> isNfcEnabled() {
    return EgkDemoAppNativePlatform.instance.isNfcEnabled();
  }

  /// Read eGK data using NFC
  ///
  /// [can] - The 6-digit Card Access Number printed on the card
  /// Returns [EgkData] with personal and insurance information
  Future<EGKDaten> readEgkData(String can) {
    return EgkDemoAppNativePlatform.instance.readEgkData(can);
  }

  /// Cancel any ongoing NFC session
  Future<void> cancelSession() {
    return EgkDemoAppNativePlatform.instance.cancelSession();
  }

  /// Stream of NFC session state changes
  Stream<NfcSessionState> get sessionStateStream {
    return EgkDemoAppNativePlatform.instance.sessionStateStream;
  }
}
