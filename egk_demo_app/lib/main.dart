import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:egk_demo_app_native/egk_demo_app_native.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'eGK Demo',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.blue),
        useMaterial3: true,
      ),
      darkTheme: ThemeData.dark(useMaterial3: true),
      home: const EgkReaderScreen(),
    );
  }
}

class EgkReaderScreen extends StatefulWidget {
  const EgkReaderScreen({super.key});

  @override
  State<EgkReaderScreen> createState() => _EgkReaderScreenState();
}

class _EgkReaderScreenState extends State<EgkReaderScreen> {
  final _egkPlugin = EgkDemoAppNative();
  final _canController = TextEditingController();

  bool _isLoading = false;
  bool _nfcAvailable = false;
  bool _nfcEnabled = false;
  String? _errorMessage;
  EgkData? _egkData;
  NfcSessionState _sessionState = NfcSessionState.idle;

  @override
  void initState() {
    super.initState();
    _checkNfcStatus();
    _listenToSessionState();
  }

  Future<void> _checkNfcStatus() async {
    try {
      final available = await _egkPlugin.isNfcAvailable();
      final enabled = await _egkPlugin.isNfcEnabled();
      setState(() {
        _nfcAvailable = available;
        _nfcEnabled = enabled;
      });
    } catch (e) {
      setState(() {
        _errorMessage = 'Fehler beim Prüfen des NFC-Status: $e';
      });
    }
  }

  void _listenToSessionState() {
    _egkPlugin.sessionStateStream.listen((state) {
      if (mounted) {
        setState(() {
          _sessionState = state;
        });
      }
    });
  }

  Future<void> _readEgkData() async {
    final can = _canController.text.trim();

    if (can.length != 6 || !RegExp(r'^\d{6}$').hasMatch(can)) {
      setState(() {
        _errorMessage = 'Die CAN muss aus 6 Ziffern bestehen.';
      });
      return;
    }

    setState(() {
      _isLoading = true;
      _errorMessage = null;
      _egkData = null;
    });

    try {
      final data = await _egkPlugin.readEgkData(can);
      setState(() {
        _egkData = data;
        _isLoading = false;
      });
    } on PlatformException catch (e) {
      setState(() {
        _errorMessage = _getErrorMessage(e.code, e.message);
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _errorMessage = 'Unbekannter Fehler: $e';
        _isLoading = false;
      });
    }
  }

  String _getErrorMessage(String code, String? message) {
    switch (code) {
      case 'USER_CANCELED':
        return 'Vorgang abgebrochen.';
      case 'NFC_NOT_AVAILABLE':
        return 'NFC ist auf diesem Gerät nicht verfügbar.';
      case 'NFC_DISABLED':
        return 'Bitte aktivieren Sie NFC in den Einstellungen.';
      case 'INVALID_CAN':
        return 'Ungültige CAN. Bitte geben Sie 6 Ziffern ein.';
      case 'READ_ERROR':
        return 'Fehler beim Lesen der Karte: ${message ?? "Unbekannt"}';
      default:
        return message ?? 'Ein Fehler ist aufgetreten.';
    }
  }

  String _getSessionStateText() {
    switch (_sessionState) {
      case NfcSessionState.discovering:
        return 'Suche nach Karte...';
      case NfcSessionState.connecting:
        return 'Verbinde mit Karte...';
      case NfcSessionState.establishingSecureChannel:
        return 'Sichere Verbindung wird hergestellt...';
      case NfcSessionState.reading:
        return 'Lese Daten...';
      case NfcSessionState.success:
        return 'Erfolgreich!';
      case NfcSessionState.error:
        return 'Fehler';
      default:
        return '';
    }
  }

  @override
  void dispose() {
    _canController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('eGK Demo'),
        backgroundColor: Theme.of(context).colorScheme.inversePrimary,
      ),
      body: GestureDetector(
        onTap: () => FocusScope.of(context).unfocus(),
        behavior: .translucent,
        child: SingleChildScrollView(
          padding: const .all(16),
          child: Column(
            crossAxisAlignment: .stretch,
            children: [
              _buildNfcStatusCard(),
              const SizedBox(height: 16),
              _buildCanInputCard(),
              const SizedBox(height: 16),
              if (_isLoading) _buildLoadingCard(),
              if (_errorMessage != null) ...[
                _buildErrorCard(),
                const SizedBox(height: 16),
              ],
              if (_egkData != null) ...[
                _buildPersonalDataCard(),
                const SizedBox(height: 16),
                _buildInsuranceDataCard(),
              ],
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildNfcStatusCard() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            Icon(
              _nfcAvailable && _nfcEnabled ? Icons.nfc : Icons.nfc_rounded,
              color: _nfcAvailable && _nfcEnabled ? Colors.green : Colors.red,
              size: 32,
            ),
            const SizedBox(width: 16),
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'NFC Status',
                  style: Theme.of(context).textTheme.titleMedium,
                ),
                Text(
                  _nfcAvailable
                      ? (_nfcEnabled
                            ? 'Verfügbar und aktiviert'
                            : 'Verfügbar aber deaktiviert')
                      : 'Nicht verfügbar',
                  style: TextStyle(
                    color: _nfcAvailable && _nfcEnabled
                        ? Colors.green
                        : Colors.red,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildCanInputCard() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(
              'Card Access Number (CAN)',
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const SizedBox(height: 8),
            Text(
              'Die 6-stellige CAN finden Sie auf der Vorderseite Ihrer Gesundheitskarte.',
              style: Theme.of(context).textTheme.bodySmall,
            ),
            const SizedBox(height: 16),
            TextField(
              controller: _canController,
              keyboardType: TextInputType.number,
              maxLength: 6,
              inputFormatters: [FilteringTextInputFormatter.digitsOnly],
              decoration: const InputDecoration(
                labelText: 'CAN eingeben',
                hintText: '123456',
                border: OutlineInputBorder(),
                counterText: '',
              ),
            ),
            const SizedBox(height: 16),
            FilledButton.tonalIcon(
              onPressed: (_nfcAvailable && _nfcEnabled && !_isLoading)
                  ? _readEgkData
                  : null,
              icon: const Icon(Icons.contactless),
              label: const Text('Karte lesen'),
              style: ElevatedButton.styleFrom(
                padding: const EdgeInsets.symmetric(vertical: 16),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildLoadingCard() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          children: [
            const CircularProgressIndicator(),
            const SizedBox(height: 16),
            Text(
              _getSessionStateText(),
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const SizedBox(height: 8),
            const Text('Halten Sie Ihr Gerät an die Gesundheitskarte.'),
          ],
        ),
      ),
    );
  }

  Widget _buildErrorCard() {
    return Card(
      color: Colors.red.shade50,
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            Icon(Icons.error_outline, color: Colors.red.shade700),
            const SizedBox(width: 16),
            Expanded(
              child: Text(
                _errorMessage!,
                style: TextStyle(color: Colors.red.shade700),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildPersonalDataCard() {
    final personal = _egkData?.personalData;
    if (personal == null) return const SizedBox.shrink();

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Icon(Icons.person, color: Colors.blue),
                const SizedBox(width: 8),
                Text(
                  'Persönliche Daten',
                  style: Theme.of(context).textTheme.titleLarge,
                ),
                Spacer(),
                IconButton.filledTonal(
                  onPressed: () => showModalBottomSheet(
                    context: context,
                    isScrollControlled: true,
                    isDismissible: true,
                    useSafeArea: true,
                    builder: (context) => SingleChildScrollView(
                      padding: const EdgeInsets.all(16),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            'Rohdaten der Versichertenstammdaten (VSD)',
                            style: Theme.of(context).textTheme.titleLarge,
                          ),
                          const SizedBox(height: 16),
                          SelectableText(
                            _egkData?.rawPersonalDataXml ?? 'No data',
                            style: const TextStyle(fontFamily: 'monospace'),
                          ),
                        ],
                      ),
                    ),
                  ),
                  icon: const Icon(Icons.document_scanner_rounded),
                ),
              ],
            ),
            const Divider(),
            _buildDataRow('Versichertennummer', personal.insurantId),
            _buildDataRow('Name', personal.fullName),
            if (personal.formattedBirthDate != null)
              _buildDataRow('Geburtsdatum', personal.formattedBirthDate!),
            _buildDataRow('Geschlecht', personal.genderDisplay),
            if (personal.streetAddress != null) ...[
              const SizedBox(height: 8),
              Text('Adresse', style: Theme.of(context).textTheme.titleSmall),
              const SizedBox(height: 4),
              Text(personal.streetAddress!.formattedAddress),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildInsuranceDataCard() {
    final insurance = _egkData?.insuranceData;
    if (insurance == null) return const SizedBox.shrink();

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Icon(Icons.health_and_safety, color: Colors.green),
                const SizedBox(width: 8),
                Text(
                  'Versicherungsdaten',
                  style: Theme.of(context).textTheme.titleLarge,
                ),
                Spacer(),
                IconButton.filledTonal(
                  onPressed: () => showModalBottomSheet(
                    context: context,
                    isScrollControlled: true,
                    isDismissible: true,
                    useSafeArea: true,
                    builder: (context) => SingleChildScrollView(
                      padding: const EdgeInsets.all(16),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            'Rohdaten der Versichertenstammdaten (VSD)',
                            style: Theme.of(context).textTheme.titleLarge,
                          ),
                          const SizedBox(height: 16),
                          SelectableText(
                            _egkData?.rawInsuranceDataXml ?? 'No data',
                            style: const TextStyle(fontFamily: 'monospace'),
                          ),
                        ],
                      ),
                    ),
                  ),
                  icon: const Icon(Icons.document_scanner_rounded),
                ),
              ],
            ),
            const Divider(),
            if (insurance.costCarrier.name != null)
              _buildDataRow('Krankenkasse', insurance.costCarrier.name!),
            if (insurance.costCarrier.identifier != null)
              _buildDataRow('IK-Nummer', insurance.costCarrier.identifier!),
            _buildDataRow('Versichertenart', insurance.insurantTypeDisplay),
            if (insurance.formattedCoverageStart != null)
              _buildDataRow(
                'Versicherungsbeginn',
                insurance.formattedCoverageStart!,
              ),
            if (insurance.formattedCoverageEnd != null)
              _buildDataRow(
                'Versicherungsende',
                insurance.formattedCoverageEnd!,
              ),
            if (insurance.wop != null)
              _buildDataRow('WOP-Kennzeichen', insurance.wop!),
            if (insurance.costReimbursement != null) ...[
              const SizedBox(height: 8),
              Text(
                'Kostenerstattung',
                style: Theme.of(context).textTheme.titleSmall,
              ),
              _buildBoolRow(
                'Ärztliche Versorgung',
                insurance.costReimbursement!.medicalCare,
              ),
              _buildBoolRow(
                'Zahnärztliche Versorgung',
                insurance.costReimbursement!.dentalCare,
              ),
              _buildBoolRow(
                'Stationärer Bereich',
                insurance.costReimbursement!.inpatientCare,
              ),
              _buildBoolRow(
                'Veranlasste Leistungen',
                insurance.costReimbursement!.initiatedServices,
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildDataRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 140,
            child: Text(
              label,
              style: const TextStyle(fontWeight: FontWeight.w500),
            ),
          ),
          Expanded(child: Text(value)),
        ],
      ),
    );
  }

  Widget _buildBoolRow(String label, bool value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 2),
      child: Row(
        children: [
          Icon(
            value ? Icons.check_circle : Icons.cancel,
            size: 16,
            color: value ? Colors.green : Colors.grey,
          ),
          const SizedBox(width: 8),
          Text(label),
        ],
      ),
    );
  }
}
