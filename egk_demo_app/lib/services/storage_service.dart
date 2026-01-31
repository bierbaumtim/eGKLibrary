import 'dart:convert';

import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:uuid/uuid.dart';

import 'package:egk_demo_app_native/egk_demo_app_native.dart';

class StorageService {
  static final StorageService instance = StorageService._internal();

  late final FlutterSecureStorage _secureStorage;
  late final SharedPreferencesAsync _prefs;

  factory StorageService() => instance;

  StorageService._internal() {
    _prefs = SharedPreferencesAsync();
    // TOOD: Set AndroidAPI level 28+ for biometric storage
    _secureStorage = const FlutterSecureStorage(
      aOptions: AndroidOptions.biometric(enforceBiometrics: false),
    );
  }

  Future<List<EGKDaten>> getStoredEgkData() async {
    final storedKeys = await _prefs.getStringList('stored_egk_keys') ?? [];
    final List<EGKDaten> egkDataList = [];

    for (final key in storedKeys) {
      final jsonString = await _secureStorage.read(key: key);

      if (jsonString != null) {
        final Map<String, dynamic> jsonData = json.decode(jsonString);

        egkDataList.add(EGKDaten.fromJson(jsonData));
      }
    }

    return egkDataList;
  }

  Future<void> saveEgkData(EGKDaten egkData) async {
    final key =
        egkData.persoenlicheVersichertenDaten?.insurantId ?? Uuid().v4();

    await _secureStorage.write(key: key, value: json.encode(egkData));

    final storedKeys = await _prefs.getStringList('stored_egk_keys') ?? [];
    if (!storedKeys.contains(key)) {
      storedKeys.add(key);
      await _prefs.setStringList('stored_egk_keys', storedKeys);
    }
  }

  Future<void> deleteEgkData(String insurantId) async {
    await _secureStorage.delete(key: insurantId);

    final storedKeys = await _prefs.getStringList('stored_egk_keys') ?? [];
    if (storedKeys.contains(insurantId)) {
      storedKeys.remove(insurantId);
      await _secureStorage.delete(key: insurantId);
      await _prefs.setStringList('stored_egk_keys', storedKeys);
    }
  }
}
