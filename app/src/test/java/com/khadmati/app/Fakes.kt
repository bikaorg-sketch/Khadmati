package com.khadmati.app

import com.khadmati.app.data.EscalationRepository
import com.khadmati.app.data.KnowledgeRepository
import com.khadmati.app.data.ServiceRequestRepository
import com.khadmati.app.domain.model.Escalation
import com.khadmati.app.domain.model.KnowledgeItem
import com.khadmati.app.domain.model.RequestStatus
import com.khadmati.app.domain.model.ServiceRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Pure-JVM fakes for the assistant brain tests (no Android context needed). */
class FakeKnowledgeRepository(items: List<KnowledgeItem> = emptyList()) : KnowledgeRepository {
    private val _items = MutableStateFlow(items)
    override val allItems: StateFlow<List<KnowledgeItem>> = _items.asStateFlow()

    override fun itemsFor(hotelId: String): List<KnowledgeItem> =
        _items.value.filter { it.hotelId == hotelId }

    override fun add(item: KnowledgeItem) {
        _items.value = listOf(item) + _items.value
    }

    override fun remove(item: KnowledgeItem) {
        _items.value = _items.value - item
    }
}

class FakeServiceRequestRepository : ServiceRequestRepository {
    private val _requests = MutableStateFlow(emptyList<ServiceRequest>())
    override val requests: StateFlow<List<ServiceRequest>> = _requests.asStateFlow()
    private var counter = 0

    override fun add(request: ServiceRequest): ServiceRequest {
        counter += 1
        val numbered = if (request.id.isBlank()) request.copy(id = "req_$counter") else request
        _requests.value = listOf(numbered) + _requests.value
        return numbered
    }

    override fun updateStatus(requestId: String, status: RequestStatus) {
        _requests.value = _requests.value.map {
            if (it.id == requestId) it.copy(status = status) else it
        }
    }
}

class FakeEscalationRepository : EscalationRepository {
    private val _escalations = MutableStateFlow(emptyList<Escalation>())
    override val escalations: StateFlow<List<Escalation>> = _escalations.asStateFlow()
    private var counter = 0

    override fun add(escalation: Escalation): Escalation {
        counter += 1
        val numbered = if (escalation.id.isBlank()) escalation.copy(id = "esc_$counter") else escalation
        _escalations.value = listOf(numbered) + _escalations.value
        return numbered
    }

    override fun resolve(escalationId: String) {
        _escalations.value = _escalations.value.map {
            if (it.id == escalationId) it.copy(status = com.khadmati.app.domain.model.EscalationStatus.RESOLVED) else it
        }
    }
}
