import 'package:flutter/material.dart';

import 'package:egk_demo_app_native/egk_demo_app_native.dart';

import '../../services/storage_service.dart';
import '../components/egk_data_view.dart';

class StorageScreen extends StatelessWidget {
  const StorageScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: FutureBuilder<List<EGKDaten>>(
        future: () async {
          return StorageService.instance.getStoredEgkData();
        }(),
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const Center(child: CircularProgressIndicator());
          } else if (snapshot.hasError) {
            return Center(
              child: Text(
                'Fehler beim Laden der gespeicherten Daten: ${snapshot.error}',
              ),
            );
          } else if (!snapshot.hasData || snapshot.data!.isEmpty) {
            return const Center(
              child: Text('Keine gespeicherten eGK-Daten gefunden.'),
            );
          } else {
            final egkDataList = snapshot.data!;

            return ListView.builder(
              itemCount: egkDataList.length,
              itemBuilder: (context, index) =>
                  _StoredItemRow(egkData: egkDataList[index]),
            );
          }
        },
      ),
    );
  }
}

class _StoredItemRow extends StatelessWidget {
  final EGKDaten egkData;

  // ignore: unused_element_parameter
  const _StoredItemRow({super.key, required this.egkData});

  @override
  Widget build(BuildContext context) {
    final insurantName =
        egkData.persoenlicheVersichertenDaten?.fullName ?? 'Unbekannt';
    final insurantId =
        egkData.persoenlicheVersichertenDaten?.insurantId ?? 'Keine ID';

    return Dismissible(
      key: ValueKey(
        egkData.persoenlicheVersichertenDaten?.insurantId ?? egkData.hashCode,
      ),
      onDismissed: (direction) async {
        if (direction == DismissDirection.endToStart) {
          await StorageService.instance.deleteEgkData(
            egkData.persoenlicheVersichertenDaten?.insurantId ?? '',
          );

          if (!context.mounted) return;

          ScaffoldMessenger.of(
            context,
          ).showSnackBar(const SnackBar(content: Text('eGK-Daten gelöscht')));
        }
      },
      background: Container(
        color: Colors.red,
        alignment: Alignment.centerRight,
        padding: const EdgeInsets.symmetric(horizontal: 20),
        child: const Icon(Icons.delete, color: Colors.white),
      ),
      child: ListTile(
        title: Text(insurantName),
        subtitle: Text('Versichertennummer: $insurantId'),
        onTap: () async => showModalBottomSheet(
          context: context,
          showDragHandle: true,
          isScrollControlled: true,
          builder: (_) => SafeArea(
            child: SingleChildScrollView(child: EgkDataView(egkData: egkData)),
          ),
        ),
      ),
    );
  }
}
