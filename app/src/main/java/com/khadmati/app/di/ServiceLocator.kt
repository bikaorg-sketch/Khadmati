package com.khadmati.app.di

import android.content.Context
import com.khadmati.app.data.AssistantRepository
import com.khadmati.app.data.EscalationRepository
import com.khadmati.app.data.HotelRepository
import com.khadmati.app.data.InMemoryHotelRepository
import com.khadmati.app.data.KnowledgeRepository
import com.khadmati.app.data.LocalEscalationRepository
import com.khadmati.app.data.LocalKbAssistant
import com.khadmati.app.data.LocalKnowledgeRepository
import com.khadmati.app.data.LocalQrRepository
import com.khadmati.app.data.LocalServiceRequestRepository
import com.khadmati.app.data.QrRepository
import com.khadmati.app.data.ServiceRequestRepository

/**
 * Manual dependency container (MVP). The backend swap point: replace the
 * local implementations with cloud-backed ones here only; screens and
 * ViewModels keep consuming the same interfaces.
 */
object ServiceLocator {

    lateinit var hotelRepository: HotelRepository
        private set

    lateinit var qrRepository: QrRepository
        private set

    lateinit var knowledgeRepository: KnowledgeRepository
        private set

    lateinit var serviceRequestRepository: ServiceRequestRepository
        private set

    lateinit var escalationRepository: EscalationRepository
        private set

    lateinit var assistantRepository: AssistantRepository
        private set

    lateinit var appContext: Context
        private set

    fun init(context: Context) {
        appContext = context.applicationContext
        hotelRepository = InMemoryHotelRepository()
        qrRepository = LocalQrRepository(context)
        knowledgeRepository = LocalKnowledgeRepository(context)
        serviceRequestRepository = LocalServiceRequestRepository(context)
        escalationRepository = LocalEscalationRepository(context)
        assistantRepository = LocalKbAssistant(
            hotelRepository,
            knowledgeRepository,
            serviceRequestRepository,
            escalationRepository,
        )
    }
}
