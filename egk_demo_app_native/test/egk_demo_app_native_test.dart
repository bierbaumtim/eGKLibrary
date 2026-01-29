import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:egk_demo_app_native/egk_demo_app_native.dart';
import 'package:egk_demo_app_native/egk_demo_app_native_platform_interface.dart';
import 'package:egk_demo_app_native/egk_demo_app_native_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockEgkDemoAppNativePlatform
    with MockPlatformInterfaceMixin
    implements EgkDemoAppNativePlatform {
  @override
  Future<String?> getPlatformVersion() => Future.value('42');

  @override
  Future<bool> isNfcAvailable() => Future.value(true);

  @override
  Future<bool> isNfcEnabled() => Future.value(true);

  @override
  Future<EGKDaten> readEgkData(String can) => Future.value(
    EGKDaten(
      persoenlicheVersichertenDaten: PersoenlicheVersichertenDaten(
        insurantId: 'A123456789',
        vorname: 'Max',
        nachname: 'Mustermann',
      ),
    ),
  );

  @override
  Future<void> cancelSession() => Future.value();

  @override
  Stream<NfcSessionState> get sessionStateStream => const Stream.empty();
}

void main() {
  final EgkDemoAppNativePlatform initialPlatform =
      EgkDemoAppNativePlatform.instance;

  test('$MethodChannelEgkDemoAppNative is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelEgkDemoAppNative>());
  });

  test('getPlatformVersion', () async {
    EgkDemoAppNative egkDemoAppNativePlugin = EgkDemoAppNative();
    MockEgkDemoAppNativePlatform fakePlatform = MockEgkDemoAppNativePlatform();
    EgkDemoAppNativePlatform.instance = fakePlatform;

    expect(await egkDemoAppNativePlugin.getPlatformVersion(), '42');
  });

  test('isNfcAvailable', () async {
    EgkDemoAppNative egkDemoAppNativePlugin = EgkDemoAppNative();
    MockEgkDemoAppNativePlatform fakePlatform = MockEgkDemoAppNativePlatform();
    EgkDemoAppNativePlatform.instance = fakePlatform;

    expect(await egkDemoAppNativePlugin.isNfcAvailable(), true);
  });

  test('readEgkData', () async {
    EgkDemoAppNative egkDemoAppNativePlugin = EgkDemoAppNative();
    MockEgkDemoAppNativePlatform fakePlatform = MockEgkDemoAppNativePlatform();
    EgkDemoAppNativePlatform.instance = fakePlatform;

    final data = await egkDemoAppNativePlugin.readEgkData('123456');
    expect(data.persoenlicheVersichertenDaten?.insurantId, 'A123456789');
    expect(data.persoenlicheVersichertenDaten?.vorname, 'Max');
    expect(data.persoenlicheVersichertenDaten?.nachname, 'Mustermann');
  });
}
