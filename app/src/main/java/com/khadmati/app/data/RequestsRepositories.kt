package com.khadmati.app.data

import android.content.Context
import com.khadmati.app.domain.model.Escalation
import com.khadmati.app.domain.model.EscalationStatus
import com.khadmati.app.domain.model.RequestStatus
import com.khadmati.app.domain.model.RequestType
import com.khadmati.app.domain.model.ServiceRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

// ---------- Service requests (persisted) ----------

interface ServiceRequestRepository {
    val requests: StateFlow<List<ServiceRequest>>

    fun add(request: ServiceRequest): ServiceRequest

    fun updateStatus(requestId: String, status: RequestStatus)
}

class LocalServiceRequestRepository(context: Context) : ServiceRequestRepository {

    private val prefs = context.getSharedPreferences("requests_prefs", Context.MODE_PRIVATE)
    private val _requests = MutableStateFlow(loadPersisted())
    override val requests: StateFlow<List<ServiceRequest>> = _requests.asStateFlow()

    override fun add(request: ServiceRequest): ServiceRequest {
        val numbered = if (request.id.isBlank()) request.copy(id = nextId()) else request
        _requests.value = listOf(numbered) + _requests.value
        persist()
        return numbered
    }

    override fun updateStatus(requestId: String, status: RequestStatus) {
        _requests.value = _requests.value.map {
            if (it.id == requestId) it.copy(status = status) else it
        }
        persist()
    }

    private fun nextId(): String = "req_${System.currentTimeMillis()}"

    private fun loadPersisted(): List<ServiceRequest> = runCatching {
        val arr = JSONArray(prefs.getString(KEY, null) ?: return@runCatching emptyList())
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            ServiceRequest(
                id = o.getString("id"),
                hotelId = o.getString("hotelId"),
                roomId = o.getString("roomId"),
                type = RequestType.valueOf(o.getString("type")),
                items = (0 until o.getJSONArray("items").length())
                    .map { j -> o.getJSONArray("items").getString(j) },
                status = RequestStatus.valueOf(o.getString("status")),
                createdAtMillis = o.getLong("createdAt"),
            )
        }
    }.getOrDefault(emptyList())

    private fun persist() {
        val arr = JSONArray()
        _requests.value.forEach { r ->
            arr.put(
                JSONObject()
                    .put("id", r.id)
                    .put("hotelId", r.hotelId)
                    .put("roomId", r.roomId)
                    .put("type", r.type.name)
                    .put("items", JSONArray(r.items))
                    .put("status", r.status.name)
                    .put("createdAt", r.createdAtMillis),
            )
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    private companion object {
        const val KEY = "requests"
    }
}

// ---------- Escalations (persisted) ----------

interface EscalationRepository {
    val escalations: StateFlow<List<Escalation>>

    fun add(escalation: Escalation): Escalation

    fun resolve(escalationId: String)
}

class LocalEscalationRepository(context: Context) : EscalationRepository {

    private val prefs = context.getSharedPreferences("escalations_prefs", Context.MODE_PRIVATE)
    private val _escalations = MutableStateFlow(loadPersisted())
    override val escalations: StateFlow<List<Escalation>> = _escalations.asStateFlow()

    override fun add(escalation: Escalation): Escalation {
        val numbered = if (escalation.id.isBlank()) {
            escalation.copy(id = "esc_${System.currentTimeMillis()}")
        } else {
            escalation
        }
        _escalations.value = listOf(numbered) + _escalations.value
        persist()
        return numbered
    }

    override fun resolve(escalationId: String) {
        _escalations.value = _escalations.value.map {
            if (it.id == escalationId) it.copy(status = EscalationStatus.RESOLVED) else it
        }
        persist()
    }

    private fun loadPersisted(): List<Escalation> = runCatching {
        val arr = JSONArray(prefs.getString(KEY, null) ?: return@runCatching emptyList())
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            Escalation(
                id = o.getString("id"),
                hotelId = o.getString("hotelId"),
                roomId = o.getString("roomId"),
                guestMessage = o.getString("message"),
                status = EscalationStatus.valueOf(o.getString("status")),
                createdAtMillis = o.getLong("createdAt"),
            )
        }
    }.getOrDefault(emptyList())

    private fun persist() {
        val arr = JSONArray()
        _escalations.value.forEach { e ->
            arr.put(
                JSONObject()
                    .put("id", e.id)
                    .put("hotelId", e.hotelId)
                    .put("roomId", e.roomId)
                    .put("message", e.guestMessage)
                    .put("status", e.status.name)
                    .put("createdAt", e.createdAtMillis),
            )
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    private companion object {
        const val KEY = "escalations"
    }
}
