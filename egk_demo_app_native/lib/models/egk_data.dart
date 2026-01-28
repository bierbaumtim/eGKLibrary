/// Data models for German electronic health card (eGK) data
/// Based on VSD Schema (Versichertenstammdaten)

/// Personal insurance data from UC_PersoenlicheVersichertendatenXML
class PersonalData {
  final String insurantId;
  final String? birthDate;
  final String firstName;
  final String lastName;
  final String? gender;
  final String? namePrefix; // Vorsatzwort
  final String? nameSuffix; // Namenszusatz
  final String? title;
  final StreetAddress? streetAddress;
  final PostOfficeAddress? postOfficeAddress;

  PersonalData({
    required this.insurantId,
    this.birthDate,
    required this.firstName,
    required this.lastName,
    this.gender,
    this.namePrefix,
    this.nameSuffix,
    this.title,
    this.streetAddress,
    this.postOfficeAddress,
  });

  factory PersonalData.fromMap(Map<String, dynamic> map) {
    return PersonalData(
      insurantId: map['insurantId'] ?? '',
      birthDate: map['birthDate'],
      firstName: map['firstName'] ?? '',
      lastName: map['lastName'] ?? '',
      gender: map['gender'],
      namePrefix: map['namePrefix'],
      nameSuffix: map['nameSuffix'],
      title: map['title'],
      streetAddress: map['streetAddress'] != null
          ? StreetAddress.fromMap(
              Map<String, dynamic>.from(map['streetAddress']),
            )
          : null,
      postOfficeAddress: map['postOfficeAddress'] != null
          ? PostOfficeAddress.fromMap(
              Map<String, dynamic>.from(map['postOfficeAddress']),
            )
          : null,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'insurantId': insurantId,
      'birthDate': birthDate,
      'firstName': firstName,
      'lastName': lastName,
      'gender': gender,
      'namePrefix': namePrefix,
      'nameSuffix': nameSuffix,
      'title': title,
      'streetAddress': streetAddress?.toMap(),
      'postOfficeAddress': postOfficeAddress?.toMap(),
    };
  }

  String get fullName {
    final parts = <String>[];
    if (title != null && title!.isNotEmpty) parts.add(title!);
    if (namePrefix != null && namePrefix!.isNotEmpty) parts.add(namePrefix!);
    parts.add(firstName);
    if (nameSuffix != null && nameSuffix!.isNotEmpty) parts.add(nameSuffix!);
    parts.add(lastName);
    return parts.join(' ');
  }

  String get genderDisplay {
    switch (gender) {
      case 'M':
        return 'Männlich';
      case 'W':
        return 'Weiblich';
      case 'X':
        return 'Unbestimmt';
      case 'D':
        return 'Divers';
      default:
        return gender ?? 'Unbekannt';
    }
  }

  String? get formattedBirthDate {
    if (birthDate == null || birthDate!.length != 8) return birthDate;
    // Format: YYYYMMDD -> DD.MM.YYYY
    final year = birthDate!.substring(0, 4);
    final month = birthDate!.substring(4, 6);
    final day = birthDate!.substring(6, 8);
    return '$day.$month.$year';
  }
}

/// Street address from StrassenAdresse
class StreetAddress {
  final String? postalCode;
  final String city;
  final String countryCode;
  final String? street;
  final String? houseNumber;
  final String? addressSupplement;

  StreetAddress({
    this.postalCode,
    required this.city,
    required this.countryCode,
    this.street,
    this.houseNumber,
    this.addressSupplement,
  });

  factory StreetAddress.fromMap(Map<String, dynamic> map) {
    return StreetAddress(
      postalCode: map['postalCode'],
      city: map['city'] ?? '',
      countryCode: map['countryCode'] ?? '',
      street: map['street'],
      houseNumber: map['houseNumber'],
      addressSupplement: map['addressSupplement'],
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'postalCode': postalCode,
      'city': city,
      'countryCode': countryCode,
      'street': street,
      'houseNumber': houseNumber,
      'addressSupplement': addressSupplement,
    };
  }

  String get formattedAddress {
    final lines = <String>[];
    if (street != null && street!.isNotEmpty) {
      final streetLine = houseNumber != null && houseNumber!.isNotEmpty
          ? '$street $houseNumber'
          : street!;
      lines.add(streetLine);
    }
    if (addressSupplement != null && addressSupplement!.isNotEmpty) {
      lines.add(addressSupplement!);
    }
    final cityLine = postalCode != null && postalCode!.isNotEmpty
        ? '$postalCode $city'
        : city;
    lines.add(cityLine);
    if (countryCode.isNotEmpty && countryCode != 'D') {
      lines.add(countryCode);
    }
    return lines.join('\n');
  }
}

/// Post office address from PostfachAdresse
class PostOfficeAddress {
  final String? postalCode;
  final String city;
  final String postOfficeBox;
  final String countryCode;

  PostOfficeAddress({
    this.postalCode,
    required this.city,
    required this.postOfficeBox,
    required this.countryCode,
  });

  factory PostOfficeAddress.fromMap(Map<String, dynamic> map) {
    return PostOfficeAddress(
      postalCode: map['postalCode'],
      city: map['city'] ?? '',
      postOfficeBox: map['postOfficeBox'] ?? '',
      countryCode: map['countryCode'] ?? '',
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'postalCode': postalCode,
      'city': city,
      'postOfficeBox': postOfficeBox,
      'countryCode': countryCode,
    };
  }
}

/// Insurance data from UC_AllgemeineVersicherungsdatenXML
class InsuranceData {
  final String? coverageStart;
  final String? coverageEnd;
  final CostCarrier costCarrier;
  final CostCarrier? billingCostCarrier;
  final String? insurantType; // Versichertenart
  final String? wop; // Wohnortprinzip
  final CostReimbursement? costReimbursement;

  InsuranceData({
    this.coverageStart,
    this.coverageEnd,
    required this.costCarrier,
    this.billingCostCarrier,
    this.insurantType,
    this.wop,
    this.costReimbursement,
  });

  factory InsuranceData.fromMap(Map<String, dynamic> map) {
    return InsuranceData(
      coverageStart: map['coverageStart'],
      coverageEnd: map['coverageEnd'],
      costCarrier: CostCarrier.fromMap(
        Map<String, dynamic>.from(map['costCarrier'] ?? {}),
      ),
      billingCostCarrier: map['billingCostCarrier'] != null
          ? CostCarrier.fromMap(
              Map<String, dynamic>.from(map['billingCostCarrier']),
            )
          : null,
      insurantType: map['insurantType'],
      wop: map['wop'],
      costReimbursement: map['costReimbursement'] != null
          ? CostReimbursement.fromMap(
              Map<String, dynamic>.from(map['costReimbursement']),
            )
          : null,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'coverageStart': coverageStart,
      'coverageEnd': coverageEnd,
      'costCarrier': costCarrier.toMap(),
      'billingCostCarrier': billingCostCarrier?.toMap(),
      'insurantType': insurantType,
      'wop': wop,
      'costReimbursement': costReimbursement?.toMap(),
    };
  }

  String get insurantTypeDisplay {
    switch (insurantType) {
      case '1':
        return 'Mitglied';
      case '3':
        return 'Familienversicherter';
      case '5':
        return 'Rentner/Familienangehörige';
      default:
        return insurantType ?? 'Unbekannt';
    }
  }

  String? get formattedCoverageStart {
    return _formatDate(coverageStart);
  }

  String? get formattedCoverageEnd {
    return _formatDate(coverageEnd);
  }

  String? _formatDate(String? date) {
    if (date == null || date.length != 8) return date;
    final year = date.substring(0, 4);
    final month = date.substring(4, 6);
    final day = date.substring(6, 8);
    return '$day.$month.$year';
  }
}

/// Cost carrier / insurance company
class CostCarrier {
  final String? identifier; // Kostentraegerkennung (IK)
  final String? countryCode;
  final String? name;

  CostCarrier({this.identifier, this.countryCode, this.name});

  factory CostCarrier.fromMap(Map<String, dynamic> map) {
    return CostCarrier(
      identifier: map['identifier']?.toString(),
      countryCode: map['countryCode'],
      name: map['name'],
    );
  }

  Map<String, dynamic> toMap() {
    return {'identifier': identifier, 'countryCode': countryCode, 'name': name};
  }
}

/// Cost reimbursement settings
class CostReimbursement {
  final bool medicalCare;
  final bool dentalCare;
  final bool inpatientCare;
  final bool initiatedServices;

  CostReimbursement({
    required this.medicalCare,
    required this.dentalCare,
    required this.inpatientCare,
    required this.initiatedServices,
  });

  factory CostReimbursement.fromMap(Map<String, dynamic> map) {
    return CostReimbursement(
      medicalCare: map['medicalCare'] == true || map['medicalCare'] == 1,
      dentalCare: map['dentalCare'] == true || map['dentalCare'] == 1,
      inpatientCare: map['inpatientCare'] == true || map['inpatientCare'] == 1,
      initiatedServices:
          map['initiatedServices'] == true || map['initiatedServices'] == 1,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'medicalCare': medicalCare,
      'dentalCare': dentalCare,
      'inpatientCare': inpatientCare,
      'initiatedServices': initiatedServices,
    };
  }
}

/// Complete eGK data container
class EgkData {
  final PersonalData? personalData;
  final InsuranceData? insuranceData;
  final String? rawPersonalDataXml;
  final String? rawInsuranceDataXml;

  EgkData({
    this.personalData,
    this.insuranceData,
    this.rawPersonalDataXml,
    this.rawInsuranceDataXml,
  });

  factory EgkData.fromMap(Map<String, dynamic> map) {
    return EgkData(
      personalData: map['personalData'] != null
          ? PersonalData.fromMap(Map<String, dynamic>.from(map['personalData']))
          : null,
      insuranceData: map['insuranceData'] != null
          ? InsuranceData.fromMap(
              Map<String, dynamic>.from(map['insuranceData']),
            )
          : null,
      rawPersonalDataXml: map['rawPersonalDataXml'],
      rawInsuranceDataXml: map['rawInsuranceDataXml'],
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'personalData': personalData?.toMap(),
      'insuranceData': insuranceData?.toMap(),
      'rawPersonalDataXml': rawPersonalDataXml,
      'rawInsuranceDataXml': rawInsuranceDataXml,
    };
  }
}

/// NFC session state events
enum NfcSessionState {
  idle,
  discovering,
  connecting,
  establishingSecureChannel,
  reading,
  success,
  error,
}

/// NFC session error types
class NfcError {
  final String code;
  final String message;

  NfcError({required this.code, required this.message});

  factory NfcError.fromMap(Map<String, dynamic> map) {
    return NfcError(
      code: map['code'] ?? 'unknown',
      message: map['message'] ?? 'Unknown error',
    );
  }

  @override
  String toString() => '$code: $message';
}
