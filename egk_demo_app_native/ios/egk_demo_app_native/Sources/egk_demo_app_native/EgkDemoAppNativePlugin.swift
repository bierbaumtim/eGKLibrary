import Flutter
import UIKit
import CoreNFC
import Compression
import Gzip
import CardReaderProviderApi
import HealthCardAccess
import HealthCardControl
import NFCCardReaderProvider
import SwiftyXMLParser
import zlib

public class EgkDemoAppNativePlugin: NSObject, FlutterPlugin {
    private var channel: FlutterMethodChannel?
    private var eventChannel: FlutterEventChannel?
    private var eventSink: FlutterEventSink?
    private var currentNfcSession: NFCHealthCardSession<[String: Any]>?
    
    public static func register(with registrar: FlutterPluginRegistrar) {
        let channel = FlutterMethodChannel(name: "egk_demo_app_native", binaryMessenger: registrar.messenger())
        let eventChannel = FlutterEventChannel(name: "egk_demo_app_native/events", binaryMessenger: registrar.messenger())
        
        let instance = EgkDemoAppNativePlugin()
        instance.channel = channel
        instance.eventChannel = eventChannel
        
        registrar.addMethodCallDelegate(instance, channel: channel)
        eventChannel.setStreamHandler(instance)
    }
    
    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        switch call.method {
        case "getPlatformVersion":
            result("iOS " + UIDevice.current.systemVersion)
            
        case "isNfcAvailable":
            result(NFCTagReaderSession.readingAvailable)
            
        case "isNfcEnabled":
            // On iOS, if NFC is available, it's enabled (no user toggle)
            result(NFCTagReaderSession.readingAvailable)
            
        case "readEgkData":
            guard let args = call.arguments as? [String: Any],
                  let can = args["can"] as? String else {
                result(FlutterError(code: "INVALID_ARGUMENTS", message: "CAN is required", details: nil))
                return
            }
            readEgkData(can: can, result: result)
            
        case "cancelSession":
            cancelSession()
            result(nil)
            
        default:
            result(FlutterMethodNotImplemented)
        }
    }
    
    // MARK: - NFC Session Messages
    private let nfcMessages = NFCHealthCardSession<[String: Any]>.Messages(
        discoveryMessage: "Halten Sie Ihr iPhone an die Gesundheitskarte",
        connectMessage: "Verbinde mit Karte...",
        secureChannelMessage: "Sichere Verbindung wird hergestellt...",
        wrongCardAccessNumberMessage: "Falsche CAN. Bitte überprüfen Sie die 6-stellige Nummer auf Ihrer Karte.",
        noCardMessage: "Keine Karte gefunden",
        multipleCardsMessage: "Mehrere Karten erkannt. Bitte nur eine Karte verwenden.",
        unsupportedCardMessage: "Nicht unterstützter Kartentyp",
        connectionErrorMessage: "Verbindungsfehler"
    )
    
    private func readEgkData(can: String, result: @escaping FlutterResult) {
        guard NFCTagReaderSession.readingAvailable else {
            result(FlutterError(code: "NFC_NOT_AVAILABLE", message: "NFC is not available on this device", details: nil))
            return
        }
        
        guard can.count == 6, can.allSatisfy({ $0.isNumber }) else {
            result(FlutterError(code: "INVALID_CAN", message: "CAN must be 6 digits", details: nil))
            return
        }
        
        sendEvent("discovering")
        
        // Create NFCHealthCardSession with OpenHealthCardKit
        guard let nfcHealthCardSession = NFCHealthCardSession(
            messages: nfcMessages,
            can: can,
            operation: { [weak self] session in
                guard let self = self else {
                    throw EgkReadError.sessionInvalidated
                }
                return try await self.performCardReading(session: session)
            }
        ) else {
            result(FlutterError(code: "SESSION_ERROR", message: "Could not initialize NFC session", details: nil))
            return
        }
        
        self.currentNfcSession = nfcHealthCardSession
        
        // Execute the reading operation
        Task {
            do {
                let egkData = try await nfcHealthCardSession.executeOperation()
                
                DispatchQueue.main.async {
                    self.sendEvent("success")
                    result(egkData)
                    self.currentNfcSession = nil
                }
            } catch NFCHealthCardSessionError.coreNFC(.userCanceled) {
                nfcHealthCardSession.invalidateSession(with: nil)
                DispatchQueue.main.async {
                    self.sendEvent("idle")
                    result(FlutterError(code: "USER_CANCELED", message: "NFC session was canceled by user", details: nil))
                    self.currentNfcSession = nil
                }
            } catch NFCHealthCardSessionError.unsupportedTag {
                nfcHealthCardSession.invalidateSession(with: "Nicht unterstützte Karte")
                DispatchQueue.main.async {
                    self.sendEvent("error")
                    result(FlutterError(code: "UNSUPPORTED_CARD", message: "The card is not a supported German health card (eGK). Please ensure you are using a valid eGK.", details: nil))
                    self.currentNfcSession = nil
                }
            } catch NFCHealthCardSessionError.wrongCAN {
                nfcHealthCardSession.invalidateSession(with: "Falsche CAN")
                DispatchQueue.main.async {
                    self.sendEvent("error")
                    result(FlutterError(code: "WRONG_CAN", message: "The Card Access Number (CAN) is incorrect. Please check the 6-digit number on your health card.", details: nil))
                    self.currentNfcSession = nil
                }
            } catch NFCHealthCardSessionError.establishingSecureChannel(let underlyingError) {
                nfcHealthCardSession.invalidateSession(with: "Sichere Verbindung fehlgeschlagen")
                DispatchQueue.main.async {
                    self.sendEvent("error")
                    result(FlutterError(code: "SECURE_CHANNEL_ERROR", message: "Failed to establish secure channel: \(underlyingError.localizedDescription)", details: nil))
                    self.currentNfcSession = nil
                }
            } catch {
                nfcHealthCardSession.invalidateSession(with: error.localizedDescription)
                DispatchQueue.main.async {
                    self.sendEvent("error")
                    result(FlutterError(code: "READ_ERROR", message: error.localizedDescription, details: nil))
                    self.currentNfcSession = nil
                }
            }
        }
    }
    
    private func performCardReading(session: NFCHealthCardSessionHandle) async throws -> [String: Any] {
        // Update alert to show reading status
        session.updateAlert(message: "Lese persönliche Daten...")
        
        // Read Personal Data (EF.PD)
        let hcaApplicationIdentifier = EgkFileSystem.DF.HCA.aid
        let hcaPdFileIdentifier = EgkFileSystem.EF.hcaPD.fid
        
        _ = try await session.card.selectDedicatedAsync(
            file: DedicatedFile(aid: hcaApplicationIdentifier, fid: hcaPdFileIdentifier)
        )
        
        let personalDataRaw = try await session.card.readSelectedFileAsync(
            expected: nil,
            failOnEndOfFileWarning: false,
            offset: 0
        )
        
        let personalDataXml = try decompressEF_PDData(data: personalDataRaw)
        let parsedPersonalData = parsePersonalDataXml(personalDataXml)
        
        // Read Insurance Data (EF.VD)
        session.updateAlert(message: "Lese Versicherungsdaten...")
        
        let hcaVdFileIdentifier = EgkFileSystem.EF.hcaVD.fid
        
        _ = try await session.card.selectDedicatedAsync(
            file: DedicatedFile(aid: hcaApplicationIdentifier, fid: hcaVdFileIdentifier)
        )
        
        let insuranceDataRaw = try await session.card.readSelectedFileAsync(
            expected: nil,
            failOnEndOfFileWarning: false,
            offset: 0
        )
        
        let insuranceDataXml = try decompressEF_VDData(data: insuranceDataRaw)
        let parsedInsuranceData = parseInsuranceDataXml(insuranceDataXml)
        
        session.updateAlert(message: "Daten erfolgreich gelesen")
        
        return [
            "personalData": parsedPersonalData,
            "insuranceData": parsedInsuranceData,
            "rawPersonalDataXml": personalDataXml,
            "rawInsuranceDataXml": insuranceDataXml
        ]
    }
    
    private func cancelSession() {
        currentNfcSession?.invalidateSession(with: nil)
        currentNfcSession = nil
        sendEvent("idle")
    }
    
    private func sendEvent(_ state: String) {
        DispatchQueue.main.async {
            self.eventSink?(state)
        }
    }
}

// MARK: - FlutterStreamHandler
extension EgkDemoAppNativePlugin: FlutterStreamHandler {
    public func onListen(withArguments arguments: Any?, eventSink events: @escaping FlutterEventSink) -> FlutterError? {
        self.eventSink = events
        return nil
    }
    
    public func onCancel(withArguments arguments: Any?) -> FlutterError? {
        self.eventSink = nil
        return nil
    }
}

// MARK: - Error Types
enum EgkReadError: Swift.Error, LocalizedError {
    case sessionInvalidated
    case decompressionFailed
    case invalidData
    
    var errorDescription: String? {
        switch self {
        case .sessionInvalidated:
            return "NFC session was invalidated"
        case .decompressionFailed:
            return "Failed to decompress card data"
        case .invalidData:
            return "Invalid data received from card"
        }
    }
}

// MARK: - Data Decompression
extension EgkDemoAppNativePlugin {
    /// Decompress VSD data from eGK
    /// First 2 bytes indicate the length of the compressed data (little-endian)
    /// Refer to https://gemspec.gematik.de/docs/gemSpec/gemSpec_eGK_Fach_VSDM/gemSpec_eGK_Fach_VSDM_V1.2.1/#2.4
    private func decompressEF_PDData(data: Data) throws -> String {
        guard data.count >= 2 else {
            throw EgkReadError.invalidData
        }
        
        // First 2 bytes indicate length (little-endian)
        let lengthBytes = data.prefix(2)
        let length = UInt16(lengthBytes.withUnsafeBytes { $0.load(as: UInt16.self) })
        let compressedData = data.suffix(from: 2).prefix(Int(length))
        
        // Decompress if gzipped
        let decompressedData: Data
        if compressedData.isGzipped {
            decompressedData = try compressedData.gunzipped()
        } else {
            decompressedData = Data(compressedData)
        }
        
        guard let xmlString = String(data: decompressedData, encoding: .utf8) 
                ?? String(data: decompressedData, encoding: .isoLatin1) else {
            throw EgkReadError.decompressionFailed
        }
        
        return xmlString
    }

    private func decompressEF_VDData(data: Data) throws -> String {
        // EF.VD structure according to gemSpec_eGK_Fach_VSDM:
        // - Offset Start VD: 2 bytes (big-endian)
        // - Offset Ende VD: 2 bytes (big-endian)
        // - Offset Start GVD: 2 bytes (big-endian)
        // - Offset Ende GVD: 2 bytes (big-endian)
        // - VD data: variable (gzip compressed XML)
        // - GVD data: variable (gzip compressed XML, optional)
        // Minimum offset value is 8 (header size)
        
        guard data.count >= 8 else {
            throw EgkReadError.invalidData
        }
        
        // Read offsets as big-endian (different from EF.PD which uses little-endian length)
        let vdOffsetStart = Int(UInt16(data[0]) << 8 | UInt16(data[1]))
        let vdOffsetEnd = Int(UInt16(data[2]) << 8 | UInt16(data[3]))
        // Bytes 4-7 are GVD offsets (Offset Start GVD, Offset Ende GVD) - not needed for VD
        
        // Validate offsets before accessing
        guard vdOffsetStart >= 8,
              vdOffsetEnd > vdOffsetStart,
              vdOffsetEnd <= data.count else {
            throw EgkReadError.invalidData
        }
        
        // Extract compressed VD data
        let compressedData = Data(data.subdata(in: vdOffsetStart..<vdOffsetEnd + 1))
        
        // Decompress gzip data
        // Gzip format: 10-byte header, compressed data (DEFLATE), 8-byte trailer (CRC32 + original size)
        // Skip gzip header (minimum 10 bytes) and decompress the DEFLATE payload
        let decompressedData: Data
        if compressedData.isGzipped {
            decompressedData = try compressedData.gunzipped()
        } else {
            decompressedData = compressedData
        }
        
        // Per spec: "Der zu verwendende Zeichensatz für die fachlichen Inhalte ist ISO8859-15"
        guard let xmlString = String(data: decompressedData, encoding: .isoLatin1)
                ?? String(data: decompressedData, encoding: .utf8) else {
            throw EgkReadError.decompressionFailed
        }
        
        return xmlString
    }
}
    
// MARK: - XML Parsing using SwiftyXMLParser
extension EgkDemoAppNativePlugin {
    private func parsePersonalDataXml(_ xmlString: String) -> [String: Any] {
        var result: [String: Any] = [:]
        
        guard let data = xmlString.data(using: .utf8) else {
            return result
        }
        
        let xml = XML.parse(data)
        let insurant = xml["UC_PersoenlicheVersichertendatenXML"]["Versicherter"]
        
        // Parse personal data fields
        // Refer to XML schema: UC_PersoenlicheVersichertendatenXML.xsd
        result["insurantId"] = insurant["Versicherten_ID"].element?.text
        result["birthDate"] = insurant["Person"]["Geburtsdatum"].element?.text
        result["firstName"] = insurant["Person"]["Vorname"].element?.text
        result["lastName"] = insurant["Person"]["Nachname"].element?.text
        result["gender"] = insurant["Person"]["Geschlecht"].element?.text
        result["namePrefix"] = insurant["Person"]["Vorsatzwort"].element?.text
        result["nameSuffix"] = insurant["Person"]["Namenszusatz"].element?.text
        result["title"] = insurant["Person"]["Titel"].element?.text
        
        // Parse street address
        let streetAddr = insurant["Person"]["StrassenAdresse"]
        if streetAddr.element != nil {
            var streetAddress: [String: Any] = [:]
            streetAddress["postalCode"] = streetAddr["Postleitzahl"].element?.text
            streetAddress["city"] = streetAddr["Ort"].element?.text
            streetAddress["street"] = streetAddr["Strasse"].element?.text
            streetAddress["houseNumber"] = streetAddr["Hausnummer"].element?.text
            streetAddress["addressSupplement"] = streetAddr["Anschriftenzusatz"].element?.text
            streetAddress["countryCode"] = streetAddr["Wohnsitzlaendercode"].element?.text ?? "D"
            result["streetAddress"] = streetAddress
        }
        
        // Parse post office address
        let postAddr = insurant["Person"]["PostfachAdresse"]
        if postAddr.element != nil {
            var postOfficeAddress: [String: Any] = [:]
            postOfficeAddress["postalCode"] = postAddr["Postleitzahl"].element?.text
            postOfficeAddress["city"] = postAddr["Ort"].element?.text
            postOfficeAddress["postOfficeBox"] = postAddr["Postfach"].element?.text
            postOfficeAddress["countryCode"] = postAddr["Wohnsitzlaendercode"].element?.text ?? "D"
            result["postOfficeAddress"] = postOfficeAddress
        }
        
        return result
    }
    
    private func parseInsuranceDataXml(_ xmlString: String) -> [String: Any] {
        var result: [String: Any] = [:]
        
        guard let data = xmlString.data(using: .utf8) else {
            return result
        }
        
        let xml = XML.parse(data)
        let allgemeineVD = xml["UC_AllgemeineVersicherungsdatenXML"]["Versicherter"]["Versicherungsschutz"]
        
        // Parse coverage dates
        result["coverageStart"] = allgemeineVD["Beginn"].element?.text
        result["coverageEnd"] = allgemeineVD["Ende"].element?.text
        
        // Parse cost carrier
        let kostentraeger = allgemeineVD["Kostentraeger"]
        var costCarrier: [String: Any] = [:]
        costCarrier["identifier"] = kostentraeger["Kostentraegerkennung"].element?.text
        costCarrier["countryCode"] = kostentraeger["Kostentraegerlaendercode"].element?.text
        costCarrier["name"] = kostentraeger["Name"].element?.text
        result["costCarrier"] = costCarrier
        
        // Parse billing cost carrier if present
        let abrechnender = allgemeineVD["AbrechnenderKostentraeger"]
        if abrechnender.element != nil {
            var billingCostCarrier: [String: Any] = [:]
            billingCostCarrier["identifier"] = abrechnender["Kostentraegerkennung"].element?.text
            billingCostCarrier["countryCode"] = abrechnender["Kostentraegerlaendercode"].element?.text
            billingCostCarrier["name"] = abrechnender["Name"].element?.text
            result["billingCostCarrier"] = billingCostCarrier
        }
        
        // Parse additional info from Zusatzinfos
        let zusatzinfos = xml["UC_AllgemeineVersicherungsdatenXML"]["Versicherter"]["Zusatzinfos"]["ZusatzinfosGKV"]
        result["insurantType"] = zusatzinfos["Versichertenart"].element?.text
        
        // Parse cost reimbursement if present
        let kostenerstattung = zusatzinfos["Kostenerstattung"]
        if kostenerstattung.element != nil {
            var costReimbursement: [String: Any] = [:]
            costReimbursement["medicalCare"] = kostenerstattung["AerztlicheVersorgung"].element?.text == "1"
            costReimbursement["dentalCare"] = kostenerstattung["ZahnaerztlicheVersorgung"].element?.text == "1"
            costReimbursement["inpatientCare"] = kostenerstattung["StationaererBereich"].element?.text == "1"
            costReimbursement["initiatedServices"] = kostenerstattung["VeranlassteLeistungen"].element?.text == "1"
            result["costReimbursement"] = costReimbursement
        }
        
        // Parse WOP (Wohnortprinzip)
        result["wop"] = zusatzinfos["WOP"].element?.text
        
        return result
    }
}
