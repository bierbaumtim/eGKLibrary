// Copyright (c) 2024 gematik GmbH
// 
// Licensed under the Apache License, Version 2.0 (the License);
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// 
//     http://www.apache.org/licenses/LICENSE-2.0
// 
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an AS IS BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package de.gematik.egk.nfccardreaderprovider

import android.app.Activity
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.util.Log
import de.gematik.egk.healthcardaccess.HealthCard
import de.gematik.egk.healthcardaccess.HealthCardType
import de.gematik.egk.healthcardcontrol.securemessaging.KeyAgreement
import de.gematik.egk.healthcardcontrol.securemessaging.SecureHealthCard
import de.gematik.egk.healthcardcontrol.securemessaging.openSecureSession
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * NFCHealthCardSession facilitates communication between Android applications and NFC-enabled health cards.
 * It leverages Android NFC to establish a session with a health card and perform operations on it,
 * such as reading data or executing commands, in a secure manner through a previously established
 * secure channel (PACE).
 * 
 * Usage example:
 * ```kotlin
 * val nfcSession = NFCHealthCardSession(
 *     activity = this,
 *     messages = NFCHealthCardSession.Messages(
 *         discoveryMessage = "Hold your phone near the health card",
 *         connectMessage = "Connecting...",
 *         secureChannelMessage = "Establishing secure channel...",
 *         noCardMessage = "No card found",
 *         multipleCardsMessage = "Multiple cards detected",
 *         unsupportedCardMessage = "Unsupported card",
 *         connectionErrorMessage = "Connection error"
 *     ),
 *     can = "123456"
 * ) { handle ->
 *     // Perform operations with the secure health card
 *     handle.updateAlert("Reading data...")
 *     // ... your card operations
 *     result
 * }
 * 
 * try {
 *     val result = nfcSession.executeOperation()
 *     // Handle success
 * } catch (e: NFCHealthCardSessionError) {
 *     // Handle error
 * }
 * ```
 */
class NFCHealthCardSession<Output>(
    private val activity: Activity,
    private val messages: Messages,
    private val can: String,
    private val operation: suspend (NFCHealthCardSessionHandle) -> Output
) {
    
    companion object {
        private const val TAG = "NFCHealthCardSession"
    }
    
    private var nfcAdapter: NfcAdapter? = null
    private var currentContinuation: CancellableContinuation<Output>? = null
    private var isSessionActive = false
    
    /**
     * Messages displayed during NFC operations.
     */
    data class Messages(
        val discoveryMessage: String,
        val connectMessage: String,
        val secureChannelMessage: String,
        val wrongCardAccessNumberMessage: String? = null,
        val noCardMessage: String,
        val multipleCardsMessage: String,
        val unsupportedCardMessage: String,
        val connectionErrorMessage: String
    )
    
    /**
     * Execute the operation on the NFC HealthCard.
     * A secure channel (PACE) is established before executing the operation.
     * 
     * @return The result of the operation
     * @throws NFCHealthCardSessionError on failure
     */
    suspend fun executeOperation(): Output = suspendCancellableCoroutine { continuation ->
        nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
        
        if (nfcAdapter == null) {
            continuation.resumeWithException(NFCHealthCardSessionError.CouldNotInitializeSession)
            return@suspendCancellableCoroutine
        }
        
        if (!nfcAdapter!!.isEnabled) {
            continuation.resumeWithException(NFCHealthCardSessionError.CouldNotInitializeSession)
            return@suspendCancellableCoroutine
        }
        
        currentContinuation = continuation
        isSessionActive = true
        
        Log.d(TAG, "Starting NFC discovery...")
        
        // Enable NFC reader mode
        val callback = NfcAdapter.ReaderCallback { tag ->
            handleTag(tag, continuation)
        }
        
        val flags = NfcAdapter.FLAG_READER_NFC_A or
                    NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
        
        nfcAdapter?.enableReaderMode(activity, callback, flags, null)
        
        continuation.invokeOnCancellation {
            Log.d(TAG, "Operation cancelled, disabling reader mode")
            disableReaderMode()
        }
    }
    
    private fun handleTag(tag: Tag, continuation: CancellableContinuation<Output>) {
        if (!isSessionActive) return
        
        Log.d(TAG, "Tag detected: ${tag.techList.joinToString()}")
        
        // Check if IsoDep is supported
        if (!tag.techList.contains(IsoDep::class.java.name)) {
            Log.w(TAG, "Tag does not support IsoDep")
            return
        }
        
        // Process the tag in a coroutine
        kotlinx.coroutines.GlobalScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val result = processTag(tag)
                if (continuation.isActive) {
                    continuation.resume(result)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing tag", e)
                if (continuation.isActive) {
                    val sessionError = when (e) {
                        is NFCHealthCardSessionError -> e
                        is KeyAgreement.Error.MacPcdVerificationFailedOnCard -> 
                            NFCHealthCardSessionError.WrongCAN
                        is NFCCardError -> 
                            NFCHealthCardSessionError.CoreNFC(e)
                        else -> 
                            NFCHealthCardSessionError.Operation(e)
                    }
                    continuation.resumeWithException(sessionError)
                }
            } finally {
                disableReaderMode()
            }
        }
    }
    
    private suspend fun processTag(tag: Tag): Output {
        val nfcCard = NFCCard.fromTag(tag)
            ?: throw NFCHealthCardSessionError.UnsupportedTag
        
        try {
            nfcCard.connect()
            
            val healthCard = HealthCard(nfcCard)
            
            // Establish secure channel (PACE)
            Log.d(TAG, "Establishing secure channel...")
            val secureHealthCard = healthCard.openSecureSession(can)
            
            // Create session handle
            val sessionHandle = DefaultNFCHealthCardSessionHandle(secureHealthCard)
            
            // Execute the operation
            Log.d(TAG, "Executing operation...")
            return operation(sessionHandle)
        } finally {
            nfcCard.disconnect(false)
        }
    }
    
    private fun disableReaderMode() {
        try {
            nfcAdapter?.disableReaderMode(activity)
        } catch (e: Exception) {
            Log.e(TAG, "Error disabling reader mode", e)
        }
        isSessionActive = false
    }
    
    /**
     * Invalidate the current NFC session.
     * 
     * @param error Optional error message
     */
    fun invalidateSession(error: String?) {
        Log.d(TAG, "Invalidating session${error?.let { ": $it" } ?: ""}")
        disableReaderMode()
    }
}

/**
 * Errors that can occur during NFC Health Card session.
 */
sealed class NFCHealthCardSessionError : Exception() {
    /** Session could not be initialized (NFC not available or disabled) */
    object CouldNotInitializeSession : NFCHealthCardSessionError() {
        override val message = "Could not initialize NFC session"
    }
    
    /** The detected tag is not supported */
    object UnsupportedTag : NFCHealthCardSessionError() {
        override val message = "Unsupported NFC tag"
    }
    
    /** NFC framework error */
    data class CoreNFC(val error: NFCCardError) : NFCHealthCardSessionError() {
        override val message = "NFC error: ${error.message}"
    }
    
    /** Wrong Card Access Number */
    object WrongCAN : NFCHealthCardSessionError() {
        override val message = "Wrong Card Access Number (CAN)"
    }
    
    /** Error establishing secure channel */
    data class EstablishingSecureChannel(val error: Throwable) : NFCHealthCardSessionError() {
        override val message = "Failed to establish secure channel: ${error.message}"
    }
    
    /** Error during operation execution */
    data class Operation(val error: Throwable) : NFCHealthCardSessionError() {
        override val message = "Operation failed: ${error.message}"
    }
}

/**
 * Handle to interact with the NFC Health Card session.
 */
interface NFCHealthCardSessionHandle {
    /** Update the alert message displayed to the user */
    fun updateAlert(message: String)
    
    /** End the session with an optional error message */
    fun invalidateSession(error: String?)
    
    /** The health card with established secure channel */
    val card: HealthCardType
}

/**
 * Default implementation of NFCHealthCardSessionHandle.
 */
private class DefaultNFCHealthCardSessionHandle(
    override val card: HealthCardType
) : NFCHealthCardSessionHandle {
    
    override fun updateAlert(message: String) {
        // On Android, we typically update the UI through a callback or LiveData
        // For now, just log the message
        Log.d("NFCHealthCardSession", "Alert: $message")
    }
    
    override fun invalidateSession(error: String?) {
        Log.d("NFCHealthCardSession", "Session invalidated${error?.let { ": $it" } ?: ""}")
    }
}

// Required import for coroutine scope
private fun <T> kotlinx.coroutines.GlobalScope.launch(
    context: kotlinx.coroutines.CoroutineDispatcher,
    block: suspend kotlinx.coroutines.CoroutineScope.() -> T
) = kotlinx.coroutines.GlobalScope.launch(context) { block() }
