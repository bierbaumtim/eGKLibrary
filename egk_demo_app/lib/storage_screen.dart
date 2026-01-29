import 'package:egk_demo_app/storage_service.dart';
import 'package:egk_demo_app_native/egk_demo_app_native.dart';
import 'package:flutter/material.dart';

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
              itemBuilder: (context, index) {
                final egkData = egkDataList[index];
                final insurantName =
                    egkData.persoenlicheVersichertenDaten?.fullName ??
                    'Unbekannt';
                final insurantId =
                    egkData.persoenlicheVersichertenDaten?.insurantId ??
                    'Keine ID';

                return ListTile(
                  title: Text(insurantName),
                  subtitle: Text('Versichertennummer: $insurantId'),
                );
              },
            );
          }
        },
      ),
    );
  }
}
