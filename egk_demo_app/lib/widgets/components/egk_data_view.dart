import 'package:flutter/material.dart';

import 'package:egk_demo_app_native/egk_demo_app_native.dart';

class EgkDataView extends StatelessWidget {
  final EGKDaten egkData;

  const EgkDataView({super.key, required this.egkData});

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: .stretch,
      children: [
        if (egkData.persoenlicheVersichertenDaten case final personal?)
          _PersonalDataCard(
            personal: personal,
            rawData: egkData.rawPersonalDataXml,
          ),
        const SizedBox(height: 16),
        if (egkData.allgemeineVersicherungsdaten case final general?)
          _GeneralInsuranceDataCard(
            insurance: general,
            rawData: egkData.rawInsuranceDataXml,
          ),
      ],
    );
  }
}

// MARK: - Personal Data Card

class _PersonalDataCard extends StatelessWidget {
  final PersoenlicheVersichertenDaten personal;
  final String? rawData;

  const _PersonalDataCard({
    // ignore: unused_element_parameter
    super.key,
    required this.personal,
    required this.rawData,
  });

  @override
  Widget build(BuildContext context) {
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
                    showDragHandle: true,
                    builder: (context) => SafeArea(
                      child: SingleChildScrollView(
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
                              rawData ?? 'No data',
                              style: const TextStyle(fontFamily: 'monospace'),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                  icon: const Icon(Icons.document_scanner_rounded),
                ),
              ],
            ),
            const Divider(),
            _DataRow(label: 'Versichertennummer', value: personal.insurantId),
            _DataRow(label: 'Name', value: personal.fullName),
            if (personal.formattedBirthDate != null)
              _DataRow(
                label: 'Geburtsdatum',
                value: personal.formattedBirthDate!,
              ),
            _DataRow(label: 'Geschlecht', value: personal.genderDisplay),
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
}

// MARK: - General Insurance Data Card
class _GeneralInsuranceDataCard extends StatelessWidget {
  final AllgemeineVersicherungsdaten insurance;
  final String? rawData;

  const _GeneralInsuranceDataCard({
    // ignore: unused_element_parameter
    super.key,
    required this.insurance,
    required this.rawData,
  });

  @override
  Widget build(BuildContext context) {
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
                    showDragHandle: true,
                    builder: (context) => SafeArea(
                      child: SingleChildScrollView(
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
                              rawData ?? 'No data',
                              style: const TextStyle(fontFamily: 'monospace'),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                  icon: const Icon(Icons.document_scanner_rounded),
                ),
              ],
            ),
            const Divider(),
            if (insurance.kostentraeger.name case final value?)
              _DataRow(label: 'Krankenkasse', value: value),
            if (insurance.kostentraeger.identifier case final value?)
              _DataRow(label: 'IK-Nummer', value: value),
            _DataRow(
              label: 'Versichertenart',
              value: insurance.insurantTypeDisplay,
            ),
            if (insurance.formattedCoverageStart case final value?)
              _DataRow(label: 'Versicherungsbeginn', value: value),
            if (insurance.formattedCoverageEnd case final value?)
              _DataRow(label: 'Versicherungsende', value: value),
            if (insurance.wop case final wop?)
              _DataRow(label: 'WOP-Kennzeichen', value: wop),
            if (insurance.kostenerstattung case final kostenerstattung?) ...[
              const SizedBox(height: 8),
              Text(
                'Kostenerstattung',
                style: Theme.of(context).textTheme.titleSmall,
              ),
              _BoolRow(
                label: 'Ärztliche Versorgung',
                value: kostenerstattung.aerztlicheVersorgung,
              ),
              _BoolRow(
                label: 'Zahnärztliche Versorgung',
                value: kostenerstattung.zahnaerztlicheVersorgung,
              ),
              _BoolRow(
                label: 'Stationärer Bereich',
                value: kostenerstattung.stationaererBereich,
              ),
              _BoolRow(
                label: 'Veranlasste Leistungen',
                value: kostenerstattung.veranlassteLeistungen,
              ),
            ],
          ],
        ),
      ),
    );
  }
}

// MARK: - Helper Widgets
class _DataRow extends StatelessWidget {
  final String label;
  final String value;

  // ignore: unused_element_parameter
  const _DataRow({super.key, required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
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
}

class _BoolRow extends StatelessWidget {
  final String label;
  final bool value;

  // ignore: unused_element_parameter
  const _BoolRow({super.key, required this.label, required this.value});

  @override
  Widget build(BuildContext context) {
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
