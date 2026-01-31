import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../../services/egk_service.dart';
import '../../services/storage_service.dart';
import '../components/egk_data_view.dart';

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
                if (widget.egkService.errorMessage case final error?) ...[
                  _ErrorCard(errorMessage: error),
                  const SizedBox(height: 16),
                ],
                if (widget.egkService.egkData case final egkData?)
                  EgkDataView(egkData: egkData),
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
  final String errorMessage;

  // ignore: unused_element_parameter
  const _ErrorCard({super.key, required this.errorMessage});

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
                errorMessage,
                style: TextStyle(color: Colors.red.shade700),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
