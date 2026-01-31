import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'package:egk_demo_app_native/egk_demo_app_native.dart';

class EGKService extends ChangeNotifier {
  final _egkPlugin = EgkDemoAppNative();

  bool _isLoading = false;
  bool _nfcAvailable = false;
  bool _nfcEnabled = false;
  String? _errorMessage;
  EGKDaten? _egkData;
  NfcSessionState _sessionState = NfcSessionState.idle;
  StreamSubscription<NfcSessionState>? _sessionStateSubscription;

  bool get isLoading => _isLoading;
  bool get nfcAvailable => _nfcAvailable;
  bool get nfcEnabled => _nfcEnabled;
  bool get isNfcReady => _nfcAvailable && _nfcEnabled;
  String? get errorMessage => _errorMessage;
  EGKDaten? get egkData => _egkData;
  NfcSessionState get sessionState => _sessionState;

  Future<void> init() async {
    try {
      _nfcAvailable = await _egkPlugin.isNfcAvailable();
      _nfcEnabled = await _egkPlugin.isNfcEnabled();

      notifyListeners();
    } catch (e) {
      _errorMessage = 'Fehler beim Prüfen des NFC-Status: $e';
      notifyListeners();
    }

    await _sessionStateSubscription?.cancel();
    _sessionStateSubscription = _egkPlugin.sessionStateStream.listen((state) {
      _sessionState = state;
      notifyListeners();
    });
  }

  @override
  Future<void> dispose() async {
    await _sessionStateSubscription?.cancel();
    super.dispose();
  }

  Future<void> readEGK(String can) async {
    _errorMessage = null;
    _egkData = null;
    notifyListeners();

    if (can.length != 6 || !RegExp(r'^\d{6}$').hasMatch(can)) {
      _errorMessage = 'Die CAN muss aus 6 Ziffern bestehen.';
      notifyListeners();

      return;
    }

    try {
      _egkData = await _egkPlugin.readEgkData(can);
      _errorMessage = null;
    } on PlatformException catch (e) {
      _errorMessage = _getErrorMessage(e.code, e.message);
    } catch (e) {
      _errorMessage = 'Unbekannter Fehler: $e';
    } finally {
      _isLoading = false;
      notifyListeners();
    }
  }

  void reset() {
    _isLoading = false;
    _errorMessage = null;
    _egkData = null;
    notifyListeners();
  }

  String get sessionStateText => switch (sessionState) {
    NfcSessionState.discovering => 'Suche nach Karte...',
    NfcSessionState.connecting => 'Verbinde mit Karte...',
    NfcSessionState.establishingSecureChannel =>
      'Sichere Verbindung wird hergestellt...',
    NfcSessionState.reading => 'Lese Daten...',
    NfcSessionState.success => 'Erfolgreich!',
    NfcSessionState.error => 'Fehler',
    _ => '',
  };

  String _getErrorMessage(String code, String? message) {
    return switch (code) {
      'USER_CANCELED' => 'Vorgang abgebrochen.',
      'NFC_NOT_AVAILABLE' => 'NFC ist auf diesem Gerät nicht verfügbar.',
      'NFC_DISABLED' => 'Bitte aktivieren Sie NFC in den Einstellungen.',
      'INVALID_CAN' => 'Ungültige CAN. Bitte geben Sie 6 Ziffern ein.',
      'READ_ERROR' => 'Fehler beim Lesen der Karte: ${message ?? "Unbekannt"}',
      _ => message ?? 'Ein Fehler ist aufgetreten.',
    };
  }
}
