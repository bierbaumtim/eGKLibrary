import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:egk_demo_app_native/egk_demo_app_native_method_channel.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  MethodChannelEgkDemoAppNative platform = MethodChannelEgkDemoAppNative();
  const MethodChannel channel = MethodChannel('egk_demo_app_native');

  setUp(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(
      channel,
      (MethodCall methodCall) async {
        return '42';
      },
    );
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger.setMockMethodCallHandler(channel, null);
  });

  test('getPlatformVersion', () async {
    expect(await platform.getPlatformVersion(), '42');
  });
}
