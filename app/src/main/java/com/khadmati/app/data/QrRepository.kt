package com.khadmati.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

interface QrRepository {
    /** roomId -> active token, observable for the host QR tab. */
    val activeTokens: StateFlow<Map<String, String>>

    /** The active token for [roomId], or null if never generated. */
    fun activeFor(roomId: String): String?

    /** Creates a new token and invalidates the previous one (rotation). */
    fun generate(roomId: String): String

    /** Validates a scanned token against the active credential. */
    fun isValid(roomId: String, token: String): Boolean
}

/**
 * Per-room QR registry. Tokens persist across app restarts - a printed code
 * keeps working until the host rotates it.
 */
class LocalQrRepository(context: Context) : QrRepository {

    private val prefs = context.getSharedPreferences("room_qr_prefs", Context.MODE_PRIVATE)

    private val _active = MutableStateFlow(loadPersisted())
    override val activeTokens: StateFlow<Map<String, String>> = _active.asStateFlow()

    override fun activeFor(roomId: String): String? = _active.value[roomId]

    override fun generate(roomId: String): String {
        val token = signToken(roomId, System.currentTimeMillis())
        _active.value = _active.value + (roomId to token)
        persist()
        return token
    }

    override fun isValid(roomId: String, token: String): Boolean =
        activeFor(roomId) == token && token.isNotEmpty()

    private fun signToken(roomId: String, issuedAt: Long): String {
        // TODO: move signing server-side (HMAC/JWT) when the backend lands,
        //  with revocation + rotation history stored centrally.
        val secret = "khadmati-local-signing-secret"
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(), "HmacSHA256"))
        return mac.doFinal("$roomId:$issuedAt".toByteArray())
            .joinToString("") { "%02x".format(it) }
            .take(24)
    }

    private fun persist() {
        val json = JSONObject()
        _active.value.forEach { (roomId, token) -> json.put(roomId, token) }
        prefs.edit().putString("active", json.toString()).apply()
    }

    private fun loadPersisted(): Map<String, String> {
        val raw = prefs.getString("active", null) ?: return emptyMap()
        return runCatching {
            val json = JSONObject(raw)
            json.keys().asSequence().associateWith { json.getString(it) }
        }.getOrDefault(emptyMap())
    }
}
