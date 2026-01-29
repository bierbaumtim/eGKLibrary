// Data models for German electronic health card (eGK) data
// Based on VSD Schema (Versichertenstammdaten)

import 'package:xml/xml.dart';
import 'package:xml/xpath.dart';

/// Complete eGK data container
class EGKDaten {
  final PersoenlicheVersichertenDaten? persoenlicheVersichertenDaten;
  final AllgemeineVersicherungsdaten? allgemeineVersicherungsdaten;
  final String? rawPersonalDataXml;
  final String? rawInsuranceDataXml;
  final String? rawSecureInsuranceDataXml;

  const EGKDaten({
    this.persoenlicheVersichertenDaten,
    this.allgemeineVersicherungsdaten,
    this.rawPersonalDataXml,
    this.rawInsuranceDataXml,
    this.rawSecureInsuranceDataXml,
  });

  factory EGKDaten.fromJson(Map<String, dynamic> map) {
    final rawPersonalDataXml = map['rawPersonalDataXml'];
    final rawInsuranceDataXml = map['rawInsuranceDataXml'];
    final rawSecureInsuranceDataXml = map['rawSecureInsuranceDataXml'];

    return EGKDaten(
      persoenlicheVersichertenDaten: switch (rawPersonalDataXml) {
        String xml when xml.isNotEmpty => PersoenlicheVersichertenDaten.fromXml(
          xml,
        ),
        _ => null,
      },
      allgemeineVersicherungsdaten: switch (rawInsuranceDataXml) {
        final xml? when xml.isNotEmpty => AllgemeineVersicherungsdaten.fromXml(
          xml,
        ),
        _ => null,
      },
      rawPersonalDataXml: rawPersonalDataXml,
      rawInsuranceDataXml: rawInsuranceDataXml,
      rawSecureInsuranceDataXml: rawSecureInsuranceDataXml,
    );
  }

  Map<String, dynamic> toJson() => {
    'rawPersonalDataXml': rawPersonalDataXml,
    'rawInsuranceDataXml': rawInsuranceDataXml,
    'rawSecureInsuranceDataXml': rawSecureInsuranceDataXml,
  };
}

/// Personal insurance data from UC_PersoenlicheVersichertendatenXML
class PersoenlicheVersichertenDaten {
  final String insurantId;
  final String? geburtsdatum;
  final String vorname;
  final String nachname;
  final String? geschlecht;
  final String? vorsatzwort; // Vorsatzwort
  final String? namenszusatz; // Namenszusatz
  final String? titel;
  final StrassenAdresse? strassenAdresse;
  final PostfachAdresse? postfachAdresse;

  const PersoenlicheVersichertenDaten({
    required this.insurantId,
    this.geburtsdatum,
    required this.vorname,
    required this.nachname,
    this.geschlecht,
    this.vorsatzwort,
    this.namenszusatz,
    this.titel,
    this.strassenAdresse,
    this.postfachAdresse,
  });

  static PersoenlicheVersichertenDaten? fromXml(String xml) {
    final document = XmlDocument.parse(xml);
    final insurant = document
        .xpath('/UC_PersoenlicheVersichertendatenXML/Versicherter')
        .firstOrNull;
    if (insurant == null) return null;

    final person = insurant.xpath('Person').firstOrNull;
    final postOfficeAdress = person?.xpath('PostfachAdresse').firstOrNull;
    final streetAddress = person?.xpath('StrassenAdresse').firstOrNull;

    return PersoenlicheVersichertenDaten(
      insurantId: insurant.getElement('Versicherten_ID')?.innerText ?? '',
      geburtsdatum: person?.getElement('Geburtsdatum')?.innerText,
      vorname: person?.getElement('Vorname')?.innerText ?? '',
      nachname: person?.getElement('Nachname')?.innerText ?? '',
      geschlecht: person?.getElement('Geschlecht')?.innerText,
      vorsatzwort: person
          ?.findElements('Vorsatzwort')
          .map((e) => e.innerText)
          .join(' '),
      namenszusatz: person
          ?.findElements('Namenszusatz')
          .map((e) => e.innerText)
          .join(' '),
      titel: person?.findElements('Titel').map((e) => e.innerText).join(' '),
      strassenAdresse: switch (streetAddress) {
        final addr? => StrassenAdresse.fromXmlNode(addr),
        _ => null,
      },
      postfachAdresse: switch (postOfficeAdress) {
        final addr? => PostfachAdresse.fromXmlNode(addr),
        _ => null,
      },
    );
  }

  String get fullName {
    final parts = <String>[];
    if (titel != null && titel!.isNotEmpty) parts.add(titel!);
    if (vorsatzwort case final vorsatzwort? when vorsatzwort.isNotEmpty) {
      parts.add(vorsatzwort);
    }
    parts.add(vorname);
    if (namenszusatz case final namenszusatz? when namenszusatz.isNotEmpty) {
      parts.add(namenszusatz);
    }
    parts.add(nachname);

    return parts.join(' ');
  }

  String get genderDisplay => switch (geschlecht) {
    'M' => 'Männlich',
    'W' => 'Weiblich',
    'X' => 'Unbestimmt',
    'D' => 'Divers',
    null => 'Unbekannt',
    final other => other,
  };

  String? get formattedBirthDate {
    if (geburtsdatum == null || geburtsdatum!.length != 8) return geburtsdatum;
    // Format: YYYYMMDD -> DD.MM.YYYY
    final year = geburtsdatum!.substring(0, 4);
    final month = geburtsdatum!.substring(4, 6);
    final day = geburtsdatum!.substring(6, 8);

    return '$day.$month.$year';
  }
}

/// Street address from StrassenAdresse
class StrassenAdresse {
  final String? postleitzahl;
  final String ort;
  final String land;
  final String? strasse;
  final String? hausnummer;
  final String? anschriftenzusatz;

  const StrassenAdresse({
    this.postleitzahl,
    required this.ort,
    required this.land,
    this.strasse,
    this.hausnummer,
    this.anschriftenzusatz,
  });

  static StrassenAdresse fromXmlNode(XmlNode node) => StrassenAdresse(
    postleitzahl: node.getElement('Postleitzahl')?.innerText,
    ort: node.getElement('Ort')?.innerText ?? '',
    land: node.getElement('Land')?.innerText ?? '',
    strasse: node.getElement('Strasse')?.innerText,
    hausnummer: node.getElement('Hausnummer')?.innerText,
    anschriftenzusatz: node.getElement('Anschriftenzusatz')?.innerText,
  );

  String get formattedAddress {
    final lines = <String>[];
    if (strasse case final strasse? when strasse.isNotEmpty) {
      final streetLine = hausnummer != null && hausnummer!.isNotEmpty
          ? '$strasse $hausnummer'
          : strasse;
      lines.add(streetLine);
    }
    if (anschriftenzusatz case final anschriftenzusatz?
        when anschriftenzusatz.isNotEmpty) {
      lines.add(anschriftenzusatz);
    }
    final cityLine = postleitzahl != null && postleitzahl!.isNotEmpty
        ? '$postleitzahl $ort'
        : ort;
    lines.add(cityLine);
    if (land.isNotEmpty && land != 'D') {
      lines.add(land);
    }

    return lines.join('\n');
  }
}

/// Post office address from PostfachAdresse
class PostfachAdresse {
  final String? postleitzahl;
  final String ort;
  final String postfach;
  final String land;

  const PostfachAdresse({
    this.postleitzahl,
    required this.ort,
    required this.postfach,
    required this.land,
  });

  static PostfachAdresse fromXmlNode(XmlNode node) => PostfachAdresse(
    postleitzahl: node.getElement('Postleitzahl')?.innerText,
    ort: node.getElement('Ort')?.innerText ?? '',
    postfach: node.getElement('Postfach')?.innerText ?? '',
    land: node.getElement('Land')?.innerText ?? '',
  );
}

/// Insurance data from UC_AllgemeineVersicherungsdatenXML
class AllgemeineVersicherungsdaten {
  final String? versicherungsSchutzBeginn;
  final String? versicherungsSchutzEnde;
  final Kostentraeger kostentraeger;
  final Kostentraeger? abrechnenderKostentraeger;
  final String? versichertenart;
  final String? wop; // Wohnortprinzip
  final Kostenerstattung? kostenerstattung;

  const AllgemeineVersicherungsdaten({
    this.versicherungsSchutzBeginn,
    this.versicherungsSchutzEnde,
    required this.kostentraeger,
    this.abrechnenderKostentraeger,
    this.versichertenart,
    this.wop,
    this.kostenerstattung,
  });

  static AllgemeineVersicherungsdaten? fromXml(String xml) {
    final document = XmlDocument.parse(xml);
    final insurant = document
        .xpath('/UC_AllgemeineVersicherungsdatenXML/Versicherter')
        .firstOrNull;
    if (insurant == null) return null;

    final coverageNode = insurant.getElement('Versicherungsschutz');
    final costCarrierNode = insurant.getElement('Kostentraeger');
    final billingCostCarrierNode = costCarrierNode?.getElement(
      'AbrechnungsKostentraeger',
    );
    final additionalInfoGKVNode = insurant
        .xpath('//Zusatzinfos/ZusatzinfosGKV')
        .firstOrNull;
    final additionalInfoBillingGKVNode = additionalInfoGKVNode?.getElement(
      'Zusatzinfos_Abrechnung_GKV',
    );
    final wop = additionalInfoBillingGKVNode?.getElement('WOP')?.innerText;

    final costReimbursementNode = additionalInfoBillingGKVNode?.getElement(
      'Kostenerstattung',
    );

    return AllgemeineVersicherungsdaten(
      versicherungsSchutzBeginn: coverageNode?.getElement('Beginn')?.innerText,
      versicherungsSchutzEnde: coverageNode?.getElement('Ende')?.innerText,
      kostentraeger: switch (costCarrierNode) {
        final node? => Kostentraeger.fromXmlNode(node),
        _ => Kostentraeger(),
      },
      abrechnenderKostentraeger: switch (billingCostCarrierNode) {
        final node? => Kostentraeger.fromXmlNode(node),
        _ => null,
      },
      versichertenart: additionalInfoGKVNode
          ?.getElement('Versichertenart')
          ?.innerText,
      wop: wop,
      kostenerstattung: switch (costReimbursementNode) {
        final node? => Kostenerstattung.fromXmlNode(node),
        _ => null,
      },
    );
  }

  String get insurantTypeDisplay => switch (versichertenart) {
    '1' => 'Mitglied',
    '3' => 'Familienversicherter',
    '5' => 'Rentner/Familienangehörige',
    null => 'Unbekannt',
    final other => other,
  };

  /// Gemäß Anlage 21 BMV-Ä und EKV
  String get wopDisplay => switch (wop) {
    '01' => 'Schleswig-Holstein',
    '02' => 'Hamburg',
    '03' => 'Bremen',
    '17' => 'Niedersachsen',
    '20' => 'Westfalen-Lippe',
    '38' => 'Nordrhein',
    '46' => 'Hessen',
    '51' => 'Rheinland-Pfalz',
    '52' => 'Baden-Württemberg',
    '71' => 'Bayerns',
    '72' => 'Berlin',
    '73' => 'Saarland',
    '78' => 'Mecklenburg-Vorpommern',
    '83' => 'Brandenburg',
    '88' => 'Sachsen-Anhalt',
    '93' => 'Thüringen',
    '98' => 'Sachsen',
    _ => 'Unbekannt',
  };

  String? get formattedCoverageStart => _formatDate(versicherungsSchutzBeginn);

  String? get formattedCoverageEnd => _formatDate(versicherungsSchutzEnde);

  String? _formatDate(String? date) {
    if (date == null || date.length != 8) return date;
    if (DateTime.tryParse(date) case final dt?) {
      return '${dt.day.toString().padLeft(2, '0')}.${dt.month.toString().padLeft(2, '0')}.${dt.year}';
    }

    final year = date.substring(0, 4);
    final month = date.substring(4, 6);
    final day = date.substring(6, 8);
    return '$day.$month.$year';
  }
}

/// Cost carrier / insurance company
class Kostentraeger {
  final String? identifier; // Kostentraegerkennung (IK)
  final String? countryCode;
  final String? name;

  const Kostentraeger({this.identifier, this.countryCode, this.name});

  static Kostentraeger fromXmlNode(XmlNode node) => Kostentraeger(
    identifier: node.getElement('Kostentraegerkennung')?.innerText,
    countryCode: node.getElement('Kostentraegerlaendercode')?.innerText,
    name: node.getElement('Name')?.innerText,
  );
}

/// Cost reimbursement settings
class Kostenerstattung {
  final bool aerztlicheVersorgung;
  final bool zahnaerztlicheVersorgung;
  final bool stationaererBereich;
  final bool veranlassteLeistungen;

  const Kostenerstattung({
    required this.aerztlicheVersorgung,
    required this.zahnaerztlicheVersorgung,
    required this.stationaererBereich,
    required this.veranlassteLeistungen,
  });

  static Kostenerstattung fromXmlNode(XmlNode node) => Kostenerstattung(
    aerztlicheVersorgung:
        node.getElement('AerztlicheVersorgung')?.innerText == '1',
    zahnaerztlicheVersorgung:
        node.getElement('ZahnaerztlicheVersorgung')?.innerText == '1',
    stationaererBereich:
        node.getElement('StationaererBereich')?.innerText == '1',
    veranlassteLeistungen:
        node.getElement('VeranlassteLeistungen')?.innerText == '1',
  );
}

/// NFC session state events
enum NfcSessionState {
  idle,
  discovering,
  connecting,
  establishingSecureChannel,
  reading,
  success,
  error;

  static NfcSessionState fromString(String state) => switch (state) {
    'idle' => NfcSessionState.idle,
    'discovering' => NfcSessionState.discovering,
    'connecting' => NfcSessionState.connecting,
    'establishingSecureChannel' => NfcSessionState.establishingSecureChannel,
    'reading' => NfcSessionState.reading,
    'success' => NfcSessionState.success,
    'error' => NfcSessionState.error,
    _ => NfcSessionState.idle,
  };
}

/// NFC session error types
class NfcError {
  final String code;
  final String message;

  const NfcError({required this.code, required this.message});

  factory NfcError.fromMap(Map<String, dynamic> map) {
    return NfcError(
      code: map['code'] ?? 'unknown',
      message: map['message'] ?? 'Unknown error',
    );
  }

  @override
  String toString() => '$code: $message';
}
