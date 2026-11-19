package com.khadmati.app

import com.khadmati.app.data.AssistantContext
import com.khadmati.app.data.InMemoryHotelRepository
import com.khadmati.app.data.KnowledgeRepository
import com.khadmati.app.data.LocalKbAssistant
import com.khadmati.app.data.SeedKnowledge
import com.khadmati.app.domain.model.RequestStatus
import com.khadmati.app.domain.model.RequestType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The assistant brain: intents must resolve in the right order and produce the
 * right side effects (service requests / escalations) with room context.
 */
class AssistantTest {

    private lateinit var knowledge: FakeKnowledgeRepository
    private lateinit var requests: FakeServiceRequestRepository
    private lateinit var escalations: FakeEscalationRepository
    private lateinit var assistant: LocalKbAssistant

    private val arContext = AssistantContext(
        hotelId = "hotel_golden_ryd",
        roomId = "room_golden_302",
        roomNumber = "302",
        arabic = true,
    )
    private val enContext = arContext.copy(arabic = false)

    @Before
    fun setUp() {
        knowledge = FakeKnowledgeRepository(SeedKnowledge.items)
        requests = FakeServiceRequestRepository()
        escalations = FakeEscalationRepository()
        assistant = LocalKbAssistant(
            InMemoryHotelRepository(),
            knowledge,
            requests,
            escalations,
        )
    }

    @Test
    fun wifiQuestionAnswersFromKnowledgeBase_arabic() = runTest {
        val reply = assistant.reply("إيه باسوورد الواي فاي؟", arContext)
        assertTrue(reply.lines.first().contains("golden2026"))
        assertTrue(reply.lines.first().contains("Golden-Guest"))
        assertEquals(0, requests.requests.value.size)
    }

    @Test
    fun wifiQuestionAnswersFromKnowledgeBase_english() = runTest {
        val reply = assistant.reply("What is the WiFi password?", enContext)
        assertTrue(reply.lines.first().contains("golden2026"))
    }

    @Test
    fun towelRequest_createsAmenitiesRequest_withRoomNumber() = runTest {
        val reply = assistant.reply("عايز مناشف جديدة لو سمحت", arContext)
        assertTrue(reply.lines.first().contains("302"))
        val created = requests.requests.value
        assertEquals(1, created.size)
        assertEquals(RequestType.AMENITIES, created.first().type)
        assertEquals(listOf("مناشف"), created.first().items)
        assertEquals(RequestStatus.OPEN, created.first().status)
    }

    @Test
    fun towelRequest_english() = runTest {
        val reply = assistant.reply("Please bring towels", enContext)
        assertTrue(reply.lines.first().contains("room 302"))
        assertEquals(listOf("Towels"), requests.requests.value.first().items)
    }

    @Test
    fun genericAmenitiesAsk_promptsForItems() = runTest {
        val reply = assistant.reply("محتاج أغراض", arContext)
        assertTrue(reply.lines.first().contains("مناشف"))
        assertEquals(0, requests.requests.value.size)
    }

    @Test
    fun foodOrder_createsRoomServiceRequest() = runTest {
        val reply = assistant.reply("أريد برجر وقهوة", arContext)
        assertTrue(reply.lines.first().contains("302"))
        val created = requests.requests.value
        assertEquals(1, created.size)
        assertEquals(RequestType.ROOM_SERVICE, created.first().type)
        assertEquals(listOf("برجر", "قهوة"), created.first().items)
    }

    @Test
    fun menuAsk_repliesWithMenuFromKnowledgeBase() = runTest {
        val reply = assistant.reply("ممكن المنيو؟", arContext)
        assertTrue(reply.lines.first().contains("فطور 45 ر.س"))
        assertTrue(reply.lines.size >= 2)
        assertEquals(0, requests.requests.value.size)
    }

    @Test
    fun cleaningRequest_createsHousekeepingRequest() = runTest {
        assistant.reply("أريد تنظيف الغرفة", arContext)
        val created = requests.requests.value
        assertEquals(1, created.size)
        assertEquals(RequestType.HOUSEKEEPING, created.first().type)
    }

    @Test
    fun brokenAc_createsMaintenanceRequest() = runTest {
        val reply = assistant.reply("المكيف مش شغال من الصبح", arContext)
        assertTrue(reply.lines.first().contains("302"))
        assertEquals(RequestType.MAINTENANCE, requests.requests.value.first().type)
    }

    @Test
    fun distressEscalates_andSharesReceptionPhone() = runTest {
        val reply = assistant.reply("في حريق في الفندق!", arContext)
        assertTrue(reply.escalated)
        assertTrue(reply.lines.first().contains("+966 11 234 5678"))
        assertEquals(1, escalations.escalations.value.size)
        assertEquals(0, requests.requests.value.size)
    }

    @Test
    fun humanAsk_sharesReceptionPhone() = runTest {
        val reply = assistant.reply("عايز أتكلم مع موظف", arContext)
        assertTrue(reply.lines.first().contains("+966 11 234 5678"))
        assertEquals(0, escalations.escalations.value.size)
    }

    @Test
    fun unknownQuestion_fallsBackToEscalation() = runTest {
        val reply = assistant.reply("كام الساعة دلوقتي؟", arContext)
        assertTrue(reply.escalated)
        assertEquals(1, escalations.escalations.value.size)
    }

    @Test
    fun escalationCarriesRoomAndHotelContext() = runTest {
        assistant.reply("كام الساعة دلوقتي؟", arContext)
        val escalation = escalations.escalations.value.first()
        assertEquals("room_golden_302", escalation.roomId)
        assertEquals("hotel_golden_ryd", escalation.hotelId)
    }
}
