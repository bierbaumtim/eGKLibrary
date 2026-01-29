package com.example.egk_demo_app_native

import android.app.Activity
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Handler
import android.os.Looper
import android.util.Log
import de.gematik.egk.healthcardaccess.HealthCard
import de.gematik.egk.healthcardaccess.cardobjects.ApplicationIdentifier
import de.gematik.egk.healthcardaccess.cardobjects.DedicatedFile
import de.gematik.egk.healthcardaccess.cardobjects.ShortFileIdentifier
import de.gematik.egk.healthcardcontrol.operations.selectDedicated
import de.gematik.egk.healthcardcontrol.operations.readFile
import de.gematik.egk.healthcardcontrol.securemessaging.openSecureSession
import de.gematik.egk.nfccardreaderprovider.NFCCard
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream

/** EgkDemoAppNativePlugin */
class EgkDemoAppNativePlugin :
    FlutterPlugin,
    MethodCallHandler,
    ActivityAware,
    EventChannel.StreamHandler {

    companion object {
        private const val TAG = "EgkDemoAppNativePlugin"
    }

    private lateinit var channel: MethodChannel
    private lateinit var eventChannel: EventChannel
    private var context: Context? = null
    private var activity: Activity? = null
    private var nfcAdapter: NfcAdapter? = null
    private var eventSink: EventChannel.EventSink? = null
    private var pendingResult: Result? = null
    private var pendingCan: String? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "egk_demo_app_native")
        channel.setMethodCallHandler(this)

        eventChannel = EventChannel(flutterPluginBinding.binaryMessenger, "egk_demo_app_native/events")
        eventChannel.setStreamHandler(this)

        context = flutterPluginBinding.applicationContext
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "getPlatformVersion" -> {
                result.success("Android ${android.os.Build.VERSION.RELEASE}")
            }
            "isNfcAvailable" -> {
                val nfcManager = context?.getSystemService(Context.NFC_SERVICE) as? NfcManager
                val adapter = nfcManager?.defaultAdapter
                result.success(adapter != null)
            }
            "isNfcEnabled" -> {
                val nfcManager = context?.getSystemService(Context.NFC_SERVICE) as? NfcManager
                val adapter = nfcManager?.defaultAdapter
                result.success(adapter?.isEnabled == true)
            }
            "readEgkData" -> {
                val can = call.argument<String>("can")
                if (can == null || can.length != 6 || !can.all { it.isDigit() }) {
                    result.error("INVALID_CAN", "CAN must be 6 digits", null)
                    return
                }
                readEgkData(can, result)
            }
            "cancelSession" -> {
                cancelSession()
                result.success(null)
            }
            else -> {
                result.notImplemented()
            }
        }
    }

    private fun readEgkData(can: String, result: Result) {
        val currentActivity = activity
        if (currentActivity == null) {
            result.error("NO_ACTIVITY", "Activity not available", null)
            return
        }

        val nfcManager = context?.getSystemService(Context.NFC_SERVICE) as? NfcManager
        nfcAdapter = nfcManager?.defaultAdapter

        if (nfcAdapter == null) {
            result.error("NFC_NOT_AVAILABLE", "NFC is not available on this device", null)
            return
        }

        if (!nfcAdapter!!.isEnabled) {
            result.error("NFC_DISABLED", "NFC is disabled. Please enable NFC in settings.", null)
            return
        }

        pendingResult = result
        pendingCan = can
        sendEvent("discovering")

        // Enable reader mode
        val callback = NfcAdapter.ReaderCallback { tag ->
            handleTag(tag)
        }

        val flags = NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK

        nfcAdapter?.enableReaderMode(currentActivity, callback, flags, null)
    }

    private fun handleTag(tag: Tag) {
        sendEvent("connecting")

        coroutineScope.launch {
            try {
                val isoDep = IsoDep.get(tag)
                if (isoDep == null) {
                    mainHandler.post {
                        sendEvent("error")
                        pendingResult?.error("UNSUPPORTED_TAG", "Tag does not support IsoDep", null)
                        pendingResult = null
                        disableReaderMode()
                    }
                    return@launch
                }

                // Pass the tag to readCardData instead of isoDep
                val egkData = readCardData(tag, isoDep)

                mainHandler.post {
                    sendEvent("success")
                    pendingResult?.success(egkData)
                    pendingResult = null
                    disableReaderMode()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading card", e)
                mainHandler.post {
                    sendEvent("error")
                    pendingResult?.error("READ_ERROR", e.message ?: "Unknown error", null)
                    pendingResult = null
                    disableReaderMode()
                }
            }
        }
    }

    private fun readCardData(tag: Tag, isoDep: IsoDep): Map<String, Any?> {
        // This method is no longer used directly - PACE authentication is required
        // Create NFCCard wrapper
        val nfcCard = NFCCard.fromTag(tag)
            ?: throw Exception("Failed to create NFCCard from tag")
        
        // Connect
        nfcCard.connect()
        
        try {
            // Create HealthCard
            val healthCard = HealthCard(nfcCard)
            
            // Establish PACE secure channel with CAN
            val can = pendingCan ?: throw Exception("CAN not provided")
            sendEvent("authenticating")
            val secureHealthCard = runBlocking {
                healthCard.openSecureSession(can)
            }
            
            sendEvent("reading")
            
            // Select HCA (Health Care Application) AID: D27600000102
            val hcaAid = ApplicationIdentifier.create(byteArrayOf(
                0xD2.toByte(), 0x76, 0x00, 0x00, 0x01, 0x02
            ))
            val hcaDf = DedicatedFile(hcaAid)
            runBlocking {
                secureHealthCard.selectDedicated(hcaDf)
            }
            
            // Read EF.PD (Personal Data) - SFID: 01
            // First read to get the length header
            val personalDataHeader = runBlocking {
                secureHealthCard.readFile(
                    ShortFileIdentifier.create(0x01u),
                    expectedSize = 2
                )
            }
            
            // Parse the length (little-endian: low byte + high byte)
            val expectedLength = (personalDataHeader[0].toInt() and 0xFF) or 
                                ((personalDataHeader[1].toInt() and 0xFF) shl 8)
            
            // Now read the complete file with the correct expected size
            val personalData = runBlocking {
                secureHealthCard.readFile(
                    ShortFileIdentifier.create(0x01u),
                    expectedSize = expectedLength + 2  // +2 for the length header itself
                )
            }
            
            val personalDataXml = decompressAndDecodeEF_PD(personalData)
            
            // Read EF.VD (Insurance Data) - SFID: 02
            // First read to get the header with offsets (8 bytes)
            val insuranceDataHeader = runBlocking {
                secureHealthCard.readFile(
                    ShortFileIdentifier.create(0x02u),
                    expectedSize = 8
                )
            }
            
            // Parse the VD end offset to determine total size needed (big-endian)
            val vdOffsetEnd = ((insuranceDataHeader[2].toInt() and 0xFF) shl 8) or 
                             (insuranceDataHeader[3].toInt() and 0xFF)
            
            // Now read the complete file with the correct expected size
            val insuranceData = runBlocking {
                secureHealthCard.readFile(
                    ShortFileIdentifier.create(0x02u),
                    expectedSize = vdOffsetEnd + 1  // +1 because offset is inclusive
                )
            }
            
            val insuranceDataXml = decompressAndDecodeEF_VD(insuranceData)
            val secureInsuranceDataXml = decompressAndDecodeEF_GVD(insuranceData)
            
            return mapOf(
                "rawPersonalDataXml" to personalDataXml,
                "rawInsuranceDataXml" to insuranceDataXml,
                "rawSecureInsuranceDataXml" to secureInsuranceDataXml
            )
        } finally {
            nfcCard.disconnect(false)
        }
    }
    
    private fun decompressAndDecodeEF_PD(data: ByteArray): String {
        if (data.size < 2) {
            throw Exception("Data too short")
        }

        // First 2 bytes indicate length (little-endian)
        val length = (data[0].toInt() and 0xFF) or ((data[1].toInt() and 0xFF) shl 8)
        val compressedData = data.copyOfRange(2, minOf(2 + length, data.size))

        // Check if data is gzip compressed (magic bytes 1f 8b)
        val decompressedData = if (compressedData.size >= 2 &&
            compressedData[0] == 0x1F.toByte() &&
            compressedData[1] == 0x8B.toByte()) {
            decompressGzip(compressedData)
        } else {
            compressedData
        }

        return String(decompressedData, Charsets.UTF_8)
    }

    private fun decompressAndDecodeEF_VD(data: ByteArray): String {
        // EF.VD structure according to gemSpec_eGK_Fach_VSDM:
        // - Offset Start VD: 2 bytes (big-endian)
        // - Offset Ende VD: 2 bytes (big-endian)
        // - Offset Start GVD: 2 bytes (big-endian)
        // - Offset Ende GVD: 2 bytes (big-endian)
        // - VD data: variable (gzip compressed XML)
        // - GVD data: variable (gzip compressed XML, optional)
        // Minimum offset value is 8 (header size)
        if (data.size < 8) {
            throw Exception("Data too short")
        }

        // Read offsets as big-endian (different from EF.PD which uses little-endian length)
        val vdOffsetStart = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
        val vdOffsetEnd = ((data[2].toInt() and 0xFF) shl 8) or (data[3].toInt() and 0xFF)
        // Bytes 4-7 are GVD offsets (Offset Start GVD, Offset Ende GVD) - not needed for VD
        
        if (vdOffsetStart < 8 || vdOffsetEnd >= data.size || vdOffsetStart >= vdOffsetEnd) {
            throw Exception("Invalid VD offsets: start=$vdOffsetStart, end=$vdOffsetEnd, dataSize=${data.size}")
        }

        // Extract range [vdOffsetStart, vdOffsetEnd] inclusive (matching iOS implementation)
        val compressedData = data.copyOfRange(vdOffsetStart, vdOffsetEnd + 1)

        // Check if data is gzip compressed (magic bytes 1f 8b)
        val decompressedData = if (compressedData.size >= 2 &&
            compressedData[0] == 0x1F.toByte() &&
            compressedData[1] == 0x8B.toByte()) {
            decompressGzip(compressedData)
        } else {
            compressedData
        }

        return String(decompressedData, Charsets.UTF_8)
    }

    private fun decompressAndDecodeEF_GVD(data: ByteArray): String {
        // EF.VD structure according to gemSpec_eGK_Fach_VSDM:
        // - Offset Start VD: 2 bytes (big-endian)
        // - Offset Ende VD: 2 bytes (big-endian)
        // - Offset Start GVD: 2 bytes (big-endian)
        // - Offset Ende GVD: 2 bytes (big-endian)
        // - VD data: variable (gzip compressed XML)
        // - GVD data: variable (gzip compressed XML, optional)
        // Minimum offset value is 8 (header size)
        if (data.size < 8) {
            throw Exception("Data too short")
        }

        // Read offsets as big-endian (different from EF.PD which uses little-endian length)
        val vdOffsetStart = ((data[4].toInt() and 0xFF) shl 8) or (data[5].toInt() and 0xFF)
        val vdOffsetEnd = ((data[6].toInt() and 0xFF) shl 8) or (data[7].toInt() and 0xFF)
        // Bytes 4-7 are GVD offsets (Offset Start GVD, Offset Ende GVD) - not needed for VD
        
        if (vdOffsetStart < 8 || vdOffsetEnd >= data.size || vdOffsetStart >= vdOffsetEnd) {
            throw Exception("Invalid VD offsets: start=$vdOffsetStart, end=$vdOffsetEnd, dataSize=${data.size}")
        }

        // Extract range [vdOffsetStart, vdOffsetEnd] inclusive (matching iOS implementation)
        val compressedData = data.copyOfRange(vdOffsetStart, vdOffsetEnd + 1)

        // Check if data is gzip compressed (magic bytes 1f 8b)
        val decompressedData = if (compressedData.size >= 2 &&
            compressedData[0] == 0x1F.toByte() &&
            compressedData[1] == 0x8B.toByte()) {
            decompressGzip(compressedData)
        } else {
            compressedData
        }

        return String(decompressedData, Charsets.UTF_8)
    }

    private fun decompressGzip(data: ByteArray): ByteArray {
        return GZIPInputStream(data.inputStream()).use { gzip ->
            gzip.readBytes()
        }
    }

    private fun parsePersonalDataXml(xml: String): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()

        result["insurantId"] = extractXmlValue(xml, "Versicherten_ID")
        result["birthDate"] = extractXmlValue(xml, "Geburtsdatum")
        result["firstName"] = extractXmlValue(xml, "Vorname")
        result["lastName"] = extractXmlValue(xml, "Nachname")
        result["gender"] = extractXmlValue(xml, "Geschlecht")
        result["namePrefix"] = extractXmlValue(xml, "Vorsatzwort")
        result["nameSuffix"] = extractXmlValue(xml, "Namenszusatz")
        result["title"] = extractXmlValue(xml, "Titel")

        // Parse street address
        if (xml.contains("<StrassenAdresse>")) {
            val streetAddress = mutableMapOf<String, Any?>()
            extractXmlSection(xml, "StrassenAdresse")?.let { section ->
                streetAddress["postalCode"] = extractXmlValue(section, "Postleitzahl")
                streetAddress["city"] = extractXmlValue(section, "Ort")
                streetAddress["street"] = extractXmlValue(section, "Strasse")
                streetAddress["houseNumber"] = extractXmlValue(section, "Hausnummer")
                streetAddress["addressSupplement"] = extractXmlValue(section, "Anschriftenzusatz")
                streetAddress["countryCode"] = extractXmlValue(section, "Wohnsitzlaendercode") ?: "D"
            }
            result["streetAddress"] = streetAddress
        }

        // Parse post office address
        if (xml.contains("<PostfachAdresse>")) {
            val postOfficeAddress = mutableMapOf<String, Any?>()
            extractXmlSection(xml, "PostfachAdresse")?.let { section ->
                postOfficeAddress["postalCode"] = extractXmlValue(section, "Postleitzahl")
                postOfficeAddress["city"] = extractXmlValue(section, "Ort")
                postOfficeAddress["postOfficeBox"] = extractXmlValue(section, "Postfach")
                postOfficeAddress["countryCode"] = extractXmlValue(section, "Wohnsitzlaendercode") ?: "D"
            }
            result["postOfficeAddress"] = postOfficeAddress
        }

        return result
    }

    private fun parseInsuranceDataXml(xml: String): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()

        // Parse coverage dates
        extractXmlSection(xml, "Versicherungsschutz")?.let { section ->
            result["coverageStart"] = extractXmlValue(section, "Beginn")
            result["coverageEnd"] = extractXmlValue(section, "Ende")
        }

        // Parse cost carrier
        val costCarrier = mutableMapOf<String, Any?>()
        extractXmlSection(xml, "Kostentraeger")?.let { section ->
            costCarrier["identifier"] = extractXmlValue(section, "Kostentraegerkennung")
            costCarrier["countryCode"] = extractXmlValue(section, "Kostentraegerlaendercode")
            costCarrier["name"] = extractXmlValue(section, "Name")
        }
        result["costCarrier"] = costCarrier

        // Parse billing cost carrier if present
        extractXmlSection(xml, "AbrechnenderKostentraeger")?.let { section ->
            val billingCostCarrier = mutableMapOf<String, Any?>()
            billingCostCarrier["identifier"] = extractXmlValue(section, "Kostentraegerkennung")
            billingCostCarrier["countryCode"] = extractXmlValue(section, "Kostentraegerlaendercode")
            billingCostCarrier["name"] = extractXmlValue(section, "Name")
            result["billingCostCarrier"] = billingCostCarrier
        }

        // Parse additional info
        result["insurantType"] = extractXmlValue(xml, "Versichertenart")
        result["wop"] = extractXmlValue(xml, "WOP")

        // Parse cost reimbursement if present
        if (xml.contains("<Kostenerstattung>")) {
            val costReimbursement = mutableMapOf<String, Any?>()
            extractXmlSection(xml, "Kostenerstattung")?.let { section ->
                costReimbursement["medicalCare"] = extractXmlValue(section, "AerztlicheVersorgung") == "1"
                costReimbursement["dentalCare"] = extractXmlValue(section, "ZahnaerztlicheVersorgung") == "1"
                costReimbursement["inpatientCare"] = extractXmlValue(section, "StationaererBereich") == "1"
                costReimbursement["initiatedServices"] = extractXmlValue(section, "VeranlassteLeistungen") == "1"
            }
            result["costReimbursement"] = costReimbursement
        }

        return result
    }

    private fun extractXmlValue(xml: String, tag: String): String? {
        val pattern = "<$tag>([^<]*)</$tag>".toRegex()
        return pattern.find(xml)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun extractXmlSection(xml: String, tag: String): String? {
        val pattern = "<$tag>(.*?)</$tag>".toRegex(RegexOption.DOT_MATCHES_ALL)
        return pattern.find(xml)?.groupValues?.get(1)
    }

    private fun ByteArray.toHexString(): String = joinToString("") { "%02X".format(it) }

    private fun cancelSession() {
        disableReaderMode()
        pendingResult = null
        sendEvent("idle")
    }

    private fun disableReaderMode() {
        activity?.let { act ->
            try {
                nfcAdapter?.disableReaderMode(act)
            } catch (e: Exception) {
                Log.e(TAG, "Error disabling reader mode", e)
            }
        }
    }

    private fun sendEvent(state: String) {
        mainHandler.post {
            eventSink?.success(state)
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        eventChannel.setStreamHandler(null)
        coroutineScope.cancel()
    }

    // ActivityAware implementation
    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivity() {
        disableReaderMode()
        activity = null
    }

    // EventChannel.StreamHandler implementation
    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        eventSink = events
    }

    override fun onCancel(arguments: Any?) {
        eventSink = null
    }
}
