import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'package:egk_demo_app/egk_service.dart';
import 'package:egk_demo_app/storage_service.dart';

class EgkReaderScreen extends StatefulWidget {
  final EGKService egkService;

  const EgkReaderScreen({super.key, required this.egkService});

  @override
  State<EgkReaderScreen> createState() => _EgkReaderScreenState();
}

class _EgkReaderScreenState extends State<EgkReaderScreen> {
  late final TextEditingController _canController;

  @override
  void initState() {
    super.initState();
    _canController = TextEditingController();
  }

  @override
  void dispose() {
    _canController.dispose();
    super.dispose();
  }

  Future<void> _readEgkData() async {
    final can = _canController.text.trim();

    await widget.egkService.readEGK(can);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      floatingActionButton: AnimatedBuilder(
        animation: widget.egkService,
        builder: (context, _) => widget.egkService.egkData != null
            ? FloatingActionButton.extended(
                onPressed: () async {
                  await StorageService.instance.saveEgkData(
                    widget.egkService.egkData!,
                  );

                  widget.egkService.reset();
                },
                label: const Text('Daten speichern'),
                icon: const Icon(Icons.save_rounded),
              )
            : const SizedBox(),
      ),
      body: GestureDetector(
        onTap: () => FocusScope.of(context).unfocus(),
        behavior: .translucent,
        child: SingleChildScrollView(
          padding: const .fromLTRB(16, 16, 16, 120),
          child: AnimatedBuilder(
            animation: widget.egkService,
            builder: (_, _) => Column(
              crossAxisAlignment: .stretch,
              children: [
                _NFCStatusCard(egkService: widget.egkService),
                const SizedBox(height: 16),
                if (widget.egkService.egkData == null) ...[
                  _buildCanInputCard(),
                  const SizedBox(height: 16),
                ],
                if (widget.egkService.isLoading)
                  _LoadingCard(egkService: widget.egkService),
                if (widget.egkService.errorMessage != null) ...[
                  _ErrorCard(egkService: widget.egkService),
                  const SizedBox(height: 16),
                ],
                if (widget.egkService.egkData != null) ...[
                  _buildPersonalDataCard(),
                  const SizedBox(height: 16),
                  _buildInsuranceDataCard(),
                ],
              ],
            ),
          ),
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
              onPressed:
                  (widget.egkService.isNfcReady && !widget.egkService.isLoading)
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

  Widget _buildPersonalDataCard() {
    final personal = widget.egkService.egkData?.persoenlicheVersichertenDaten;
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
                            widget.egkService.egkData?.rawPersonalDataXml ??
                                'No data',
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
            if (personal.strassenAdresse case final value?) ...[
              const SizedBox(height: 8),
              Text('Adresse', style: Theme.of(context).textTheme.titleSmall),
              const SizedBox(height: 4),
              Text(value.formattedAddress),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildInsuranceDataCard() {
    final insurance = widget.egkService.egkData?.allgemeineVersicherungsdaten;
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
                            widget.egkService.egkData?.rawInsuranceDataXml ??
                                'No data',
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
            if (insurance.kostentraeger.name case final value?)
              _buildDataRow('Krankenkasse', value),
            if (insurance.kostentraeger.identifier case final value?)
              _buildDataRow('IK-Nummer', value),
            _buildDataRow('Versichertenart', insurance.insurantTypeDisplay),
            if (insurance.formattedCoverageStart case final value?)
              _buildDataRow('Versicherungsbeginn', value),
            if (insurance.formattedCoverageEnd case final value?)
              _buildDataRow('Versicherungsende', value),
            if (insurance.wop case final wop?)
              _buildDataRow('WOP-Kennzeichen', wop),
            if (insurance.kostenerstattung case final kostenerstattung?) ...[
              const SizedBox(height: 8),
              Text(
                'Kostenerstattung',
                style: Theme.of(context).textTheme.titleSmall,
              ),
              _buildBoolRow(
                'Ärztliche Versorgung',
                kostenerstattung.aerztlicheVersorgung,
              ),
              _buildBoolRow(
                'Zahnärztliche Versorgung',
                kostenerstattung.zahnaerztlicheVersorgung,
              ),
              _buildBoolRow(
                'Stationärer Bereich',
                kostenerstattung.stationaererBereich,
              ),
              _buildBoolRow(
                'Veranlasste Leistungen',
                kostenerstattung.veranlassteLeistungen,
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

class _NFCStatusCard extends StatelessWidget {
  final EGKService egkService;

  // ignore: unused_element_parameter
  const _NFCStatusCard({super.key, required this.egkService});

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            Icon(
              egkService.isNfcReady ? Icons.nfc : Icons.nfc_rounded,
              color: egkService.isNfcReady ? Colors.green : Colors.red,
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
                  egkService.nfcAvailable
                      ? (egkService.nfcEnabled
                            ? 'Verfügbar und aktiviert'
                            : 'Verfügbar aber deaktiviert')
                      : 'Nicht verfügbar',
                  style: TextStyle(
                    color: egkService.isNfcReady ? Colors.green : Colors.red,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _LoadingCard extends StatelessWidget {
  final EGKService egkService;

  // ignore: unused_element_parameter
  const _LoadingCard({super.key, required this.egkService});

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          children: [
            const CircularProgressIndicator(),
            const SizedBox(height: 16),
            Text(
              egkService.sessionStateText,
              style: Theme.of(context).textTheme.titleMedium,
            ),
            const SizedBox(height: 8),
            const Text('Halten Sie Ihr Gerät an die Gesundheitskarte.'),
          ],
        ),
      ),
    );
  }
}

class _ErrorCard extends StatelessWidget {
  final EGKService egkService;

  // ignore: unused_element_parameter
  const _ErrorCard({super.key, required this.egkService});

  @override
  Widget build(BuildContext context) {
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
                egkService.errorMessage ?? 'Unbekannter Fehler',
                style: TextStyle(color: Colors.red.shade700),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
