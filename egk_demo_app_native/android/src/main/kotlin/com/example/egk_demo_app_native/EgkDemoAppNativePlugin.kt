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

                isoDep.connect()
                isoDep.timeout = 5000 // 5 seconds timeout

                sendEvent("reading")

                val egkData = readCardData(isoDep)

                isoDep.close()

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

    private fun readCardData(isoDep: IsoDep): Map<String, Any?> {
        // Select HCA (Health Care Application) AID: D27600000102
        val hcaAid = byteArrayOf(0xD2.toByte(), 0x76, 0x00, 0x00, 0x01, 0x02)
        val selectCommand = byteArrayOf(
            0x00, 0xA4.toByte(), 0x04, 0x0C,
            hcaAid.size.toByte()
        ) + hcaAid

        val selectResponse = isoDep.transceive(selectCommand)
        if (!checkSuccess(selectResponse)) {
            throw Exception("Failed to select HCA application: ${selectResponse.toHexString()}")
        }

        // Read EF.PD (Personal Data) - SFID: 01
        val personalData = readFile(isoDep, 0x01)
        val personalDataXml = decompressAndDecode(personalData)
        val parsedPersonalData = parsePersonalDataXml(personalDataXml)

        // Read EF.VD (Insurance Data) - SFID: 02
        val insuranceData = readFile(isoDep, 0x02)
        val insuranceDataXml = decompressAndDecode(insuranceData)
        val parsedInsuranceData = parseInsuranceDataXml(insuranceDataXml)

        return mapOf(
            "personalData" to parsedPersonalData,
            "insuranceData" to parsedInsuranceData,
            "rawPersonalDataXml" to personalDataXml,
            "rawInsuranceDataXml" to insuranceDataXml
        )
    }

    private fun readFile(isoDep: IsoDep, sfid: Int): ByteArray {
        val result = ByteArrayOutputStream()
        var offset = 0

        while (true) {
            // READ BINARY with short file identifier
            val p1 = (0x80 or sfid).toByte()
            val p2 = (offset and 0xFF).toByte()

            val readCommand = byteArrayOf(0x00, 0xB0.toByte(), p1, p2, 0x00)
            val response = isoDep.transceive(readCommand)

            if (response.size < 2) {
                throw Exception("Invalid response")
            }

            val sw1 = response[response.size - 2].toInt() and 0xFF
            val sw2 = response[response.size - 1].toInt() and 0xFF
            val data = response.copyOfRange(0, response.size - 2)

            when {
                sw1 == 0x90 && sw2 == 0x00 -> {
                    result.write(data)
                    offset += data.size
                }
                sw1 == 0x62 && sw2 == 0x82 -> {
                    // End of file warning
                    result.write(data)
                    break
                }
                sw1 == 0x6B && sw2 == 0x00 -> {
                    // Wrong parameters - past end of file
                    break
                }
                else -> {
                    throw Exception("Read error: ${String.format("%02X%02X", sw1, sw2)}")
                }
            }

            if (data.size < 256) {
                break // Last chunk
            }
        }

        return result.toByteArray()
    }

    private fun decompressAndDecode(data: ByteArray): String {
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

    private fun checkSuccess(response: ByteArray): Boolean {
        if (response.size < 2) return false
        val sw1 = response[response.size - 2].toInt() and 0xFF
        val sw2 = response[response.size - 1].toInt() and 0xFF
        return sw1 == 0x90 && sw2 == 0x00
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
