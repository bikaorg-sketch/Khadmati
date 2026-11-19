package com.khadmati.app.data

import com.khadmati.app.domain.model.AssistantReply
import com.khadmati.app.domain.model.Escalation
import com.khadmati.app.domain.model.EscalationStatus
import com.khadmati.app.domain.model.RequestStatus
import com.khadmati.app.domain.model.RequestType
import com.khadmati.app.domain.model.ServiceRequest
import kotlinx.coroutines.delay

data class AssistantContext(
    val hotelId: String,
    val roomId: String,
    /** Room number as printed on the door, used in confirmations. */
    val roomNumber: String,
    val arabic: Boolean,
)

/**
 * The assistant boundary. Phase 1 ships a local knowledge-base engine reading
 * the hotel KB and creating service requests; a cloud LLM client will
 * implement the same interface later (the API key never lives in the app).
 */
interface AssistantRepository {
    suspend fun reply(message: String, context: AssistantContext): AssistantReply
}

/**
 * Local retrieval assistant for in-room service:
 * 1. Guest-distress keywords -> immediate escalate to staff (never auto-answer).
 * 2. Maintenance issue -> create a MAINTENANCE request.
 * 3. Amenity request -> create an AMENITIES request with the detected items.
 * 4. Room-service food order -> create a ROOM_SERVICE request; menu ask -> reply menu.
 * 5. Housekeeping -> create a HOUSEKEEPING request.
 * 6. Human/reception ask -> reply with the front-desk phone.
 * 7. Knowledge-base retrieval (wifi, check-in, pool, qibla...).
 * 8. Fallback -> log unanswered question + escalate.
 */
class LocalKbAssistant(
    private val hotelRepository: HotelRepository,
    private val knowledgeRepository: KnowledgeRepository,
    private val serviceRequestRepository: ServiceRequestRepository,
    private val escalationRepository: EscalationRepository,
) : AssistantRepository {

    private val recentCalls = ArrayDeque<Long>()

    override suspend fun reply(message: String, context: AssistantContext): AssistantReply {
        delay(450) // simulate round trip (drives the typing indicator)
        val now = System.currentTimeMillis()
        while (recentCalls.isNotEmpty() && now - recentCalls.first() > 60_000) {
            recentCalls.removeFirst()
        }
        if (recentCalls.size >= 10) {
            return AssistantReply(
                listOf(if (context.arabic) "رسائل كتير في الدقيقة - استنى لحظة وجرّب تاني 🙏" else "Too many messages in a minute - please wait a moment 🙏"),
                escalated = false,
            )
        }
        recentCalls.addLast(now)

        val lower = message.lowercase()
        val ar = context.arabic

        // 1. Guest distress -> escalate immediately.
        val distressKeywords = listOf(
            "حريق", "دخان", "غاز", "إصابة", "اصابه", "مريض", "اسعاف", "إسعاف", "سرقة", "سرق",
            "قذر", "قذرة", "حشرات", "حشريات", "سارق",
            "fire", "smoke", "gas leak", "injury", "sick", "ambulance", "theft", "stolen", "dirty", "insects",
        )
        if (distressKeywords.any { lower.contains(it) }) {
            escalate(context, message)
            return AssistantReply(
                listOf(
                    if (ar) "أعتذر عن ذلك 🙏 حولت حالتك فوراً لإدارة الفندق وسيتواصلون معاك بأسرع وقت. للاستفسار العاجل: ${phoneOf(context)}"
                    else "I am sorry about this 🙏 I escalated your case to hotel management immediately; they will reach out ASAP. Urgent line: ${phoneOf(context)}",
                ),
                escalated = true,
            )
        }

        // 2. Maintenance -> request (broken AC, TV, lights, water...).
        val maintenanceKeywords = listOf(
            "معطل", "خربان", "مش شغال", "مش راضي", "تالف", "انسداد", "بالوعة", "مكيف", "تلفزيون",
            "سخان", "دش", "لمبة", "الضوء", "صيانة", "المقبض", "انسكاب",
            "broken", "not working", "stopped working", "leak", "clogged", "ac ", "air condition",
            "tv ", "television", "heater", "shower", "lamp", "light", "maintenance",
        )
        if (maintenanceKeywords.any { lower.contains(it) }) {
            val request = serviceRequestRepository.add(
                ServiceRequest(
                    id = "",
                    hotelId = context.hotelId,
                    roomId = context.roomId,
                    type = RequestType.MAINTENANCE,
                    items = listOf(message),
                    status = RequestStatus.OPEN,
                    createdAtMillis = System.currentTimeMillis(),
                ),
            )
            return AssistantReply(
                listOf(
                    if (ar) "سجّلت بلاغ الصيانة للغرفة ${context.roomNumber} ✅ (رقم الطلب ${shortId(request.id)}) - فريق الصيانة هيتواصل معاك قريباً."
                    else "Maintenance report for room ${context.roomNumber} logged ✅ (request ${shortId(request.id)}) - the team will contact you shortly.",
                ),
                escalated = false,
            )
        }

        // 3. Amenity request -> request with the detected items.
        val amenityItems = detectAmenities(lower, ar)
        val asksAmenities = amenityItems.isNotEmpty() ||
            lower.contains("أغراض") || lower.contains("مستلزمات") ||
            lower.contains("amenities") || lower.contains("towel")
        if (amenityItems.isNotEmpty()) {
            val request = serviceRequestRepository.add(
                ServiceRequest(
                    id = "",
                    hotelId = context.hotelId,
                    roomId = context.roomId,
                    type = RequestType.AMENITIES,
                    items = amenityItems,
                    status = RequestStatus.OPEN,
                    createdAtMillis = System.currentTimeMillis(),
                ),
            )
            return AssistantReply(
                listOf(
                    if (ar) "تم ✅ هيوصلك ${amenityItems.joinToString(" و")} للغرفة ${context.roomNumber} خلال 10-15 دقيقة (رقم الطلب ${shortId(request.id)})."
                    else "Done ✅ ${amenityItems.joinToString(" & ")} will reach room ${context.roomNumber} within 10-15 minutes (request ${shortId(request.id)}).",
                ),
                escalated = false,
            )
        }
        if (asksAmenities) {
            return AssistantReply(
                listOf(
                    if (ar) "أكدلي إيه اللي ناقصك في الغرفة؟ مناشف، ورق حمام، شامبو، صابون، مياه معدنية، مخدات، بطانية؟"
                    else "What do you need in the room? Towels, toilet paper, shampoo, soap, bottled water, pillows, a blanket?",
                ),
                escalated = false,
            )
        }

        // 4. Room service: menu ask or food order.
        val foodItems = detectFood(lower, ar)
        if (foodItems.isNotEmpty()) {
            val request = serviceRequestRepository.add(
                ServiceRequest(
                    id = "",
                    hotelId = context.hotelId,
                    roomId = context.roomId,
                    type = RequestType.ROOM_SERVICE,
                    items = foodItems,
                    status = RequestStatus.OPEN,
                    createdAtMillis = System.currentTimeMillis(),
                ),
            )
            return AssistantReply(
                listOf(
                    if (ar) "طلبك اتحجز 🍽️ ${foodItems.joinToString(" و")} للغرفة ${context.roomNumber} (رقم الطلب ${shortId(request.id)}) - التوصيل خلال 20-30 دقيقة."
                    else "Order placed 🍽️ ${foodItems.joinToString(" & ")} for room ${context.roomNumber} (request ${shortId(request.id)}) - delivery in 20-30 minutes.",
                ),
                escalated = false,
            )
        }
        val asksMenu = lower.contains("منيو") || lower.contains("قائمة") ||
            lower.contains("menu") || lower.contains("أكل") || lower.contains("طعام") ||
            lower.contains("food") || lower.contains("order") || lower.contains("طلبيه") ||
            lower.contains("جعان") || lower.contains("hungry")
        if (asksMenu) {
            return AssistantReply(
                listOf(menuText(context), if (ar) "اكتب اسم اللي عايزه وهجهزلك الطلب فوراً 🍽️" else "Type what you would like and I will place the order right away 🍽️"),
                escalated = false,
            )
        }

        // 5. Housekeeping -> request.
        if (lower.contains("تنظيف") || lower.contains("نظافة") || lower.contains("ترتيب") ||
            lower.contains("clean") || lower.contains("housekeeping") || lower.contains("tidy")
        ) {
            val request = serviceRequestRepository.add(
                ServiceRequest(
                    id = "",
                    hotelId = context.hotelId,
                    roomId = context.roomId,
                    type = RequestType.HOUSEKEEPING,
                    items = listOf(if (ar) "تنظيف الغرفة" else "Clean the room"),
                    status = RequestStatus.OPEN,
                    createdAtMillis = System.currentTimeMillis(),
                ),
            )
            return AssistantReply(
                listOf(
                    if (ar) "تم جدولة تنظيف الغرفة ${context.roomNumber} ✅ (رقم الطلب ${shortId(request.id)}) - بيت أُمنة الغرفة هيتواصل قبل الدخول."
                    else "Room ${context.roomNumber} cleaning scheduled ✅ (request ${shortId(request.id)}) - housekeeping will knock before entering.",
                ),
                escalated = false,
            )
        }

        // 6. Human / reception.
        if (lower.contains("استقبال") || lower.contains("موظف") || lower.contains("إنسان") ||
            lower.contains("انسان") || lower.contains("اتصال") || lower.contains("اتصل") ||
            lower.contains("reception") || lower.contains("human") || lower.contains("staff") ||
            lower.contains("call me")
        ) {
            return AssistantReply(
                listOf(
                    if (ar) "تقدر تتواصل مع الاستقبال مباشرة على ${phoneOf(context)} - متاحين 24 ساعة ☎️"
                    else "You can reach the front desk directly at ${phoneOf(context)} - available 24/7 ☎️",
                ),
                escalated = false,
            )
        }

        // 7. Knowledge base retrieval.
        val kb = knowledgeRepository.itemsFor(context.hotelId)
        val match = kb.firstOrNull { item -> item.keywords.any { lower.contains(it.lowercase()) } }
        if (match != null) {
            return AssistantReply(listOf(if (ar) match.answerAr else match.answerEn), escalated = false)
        }

        // 8. Fallback: log unanswered question + escalate.
        escalate(context, message)
        return AssistantReply(
            listOf(
                if (ar) "مش متأكد من الإجابة - سجلت سؤالك لإدارة الفندق وهيردوا عليك 💬 وتقدر تطلب موظف في أي وقت."
                else "I am not sure about that - I logged your question for hotel management 💬 and you can ask for a staff member anytime.",
            ),
            escalated = true,
        )
    }

    // ---------- helpers ----------

    private fun shortId(id: String) = id.substringAfter("req_").takeLast(6)

    private fun phoneOf(context: AssistantContext): String =
        hotelRepository.hotel(context.hotelId)?.receptionPhone ?: ""

    private fun escalate(context: AssistantContext, message: String) {
        escalationRepository.add(
            Escalation(
                id = "",
                hotelId = context.hotelId,
                roomId = context.roomId,
                guestMessage = message,
                status = EscalationStatus.OPEN,
                createdAtMillis = System.currentTimeMillis(),
            ),
        )
    }

    private fun menuText(context: AssistantContext): String {
        val menu = knowledgeRepository.itemsFor(context.hotelId)
            .firstOrNull { item -> item.keywords.any { it.contains("منيو") || it.contains("menu") } }
        return if (context.arabic) menu?.answerAr ?: DEFAULT_MENU_AR else menu?.answerEn ?: DEFAULT_MENU_EN
    }

    private fun detectAmenities(lower: String, ar: Boolean): List<String> = buildList {
        if (anyOf(lower, "منشفة", "مناشف", "towel")) add(if (ar) "مناشف" else "Towels")
        if (anyOf(lower, "ورق", "تواليت", "toilet paper", "tissue")) add(if (ar) "ورق حمام" else "Toilet paper")
        if (anyOf(lower, "شامبو", "shampoo")) add(if (ar) "شامبو" else "Shampoo")
        if (anyOf(lower, "صابون", "soap")) add(if (ar) "صابون" else "Soap")
        if (anyOf(lower, "مياه", "ماء", "water")) add(if (ar) "مياه معدنية" else "Bottled water")
        if (anyOf(lower, "مخدة", "مخدات", "pillow")) add(if (ar) "مخدات إضافية" else "Extra pillows")
        if (anyOf(lower, "غطاء", "بطانية", "blanket")) add(if (ar) "بطانية إضافية" else "Extra blanket")
    }

    private fun detectFood(lower: String, ar: Boolean): List<String> = buildList {
        if (anyOf(lower, "فطور", "breakfast")) add(if (ar) "فطور" else "Breakfast")
        if (anyOf(lower, "برجر", "burger")) add(if (ar) "برجر" else "Burger")
        if (anyOf(lower, "فلافل", "falafel")) add(if (ar) "ساندويتش فلافل" else "Falafel sandwich")
        if (anyOf(lower, "بيتزا", "pizza")) add(if (ar) "بيتزا" else "Pizza")
        if (anyOf(lower, "سمك", "fish")) add(if (ar) "سمك مشوي" else "Grilled fish")
        if (anyOf(lower, "لحم", "شريحة", "steak")) add(if (ar) "شريحة لحم" else "Steak")
        if (anyOf(lower, "سلطة", "salad")) add(if (ar) "سلطة" else "Salad")
        if (anyOf(lower, "عصير", "juice")) add(if (ar) "عصير طازج" else "Fresh juice")
        if (anyOf(lower, "شاي", "tea")) add(if (ar) "شاي" else "Tea")
        if (anyOf(lower, "قهوة", "coffee")) add(if (ar) "قهوة" else "Coffee")
    }

    private fun anyOf(lower: String, vararg needles: String) = needles.any { lower.contains(it) }

    private companion object {
        const val DEFAULT_MENU_AR = "منيو خدمة الغرف متاح في الدليل الموجود على المكتب 🍽️"
        const val DEFAULT_MENU_EN = "The room service menu is in the guide on your desk 🍽️"
    }
}
