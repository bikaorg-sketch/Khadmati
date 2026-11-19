package com.khadmati.app.data

import android.content.Context
import com.khadmati.app.domain.model.KnowledgeItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

interface KnowledgeRepository {
    /** All items, observable for the host editor. */
    val allItems: StateFlow<List<KnowledgeItem>>

    fun itemsFor(hotelId: String): List<KnowledgeItem>

    fun add(item: KnowledgeItem)

    fun remove(item: KnowledgeItem)
}

/**
 * Persisted KB (host edits must survive restarts) seeded with chain-wide
 * answers: wifi, check-in/out, pool, breakfast, gym, qibla, parking, menu.
 */
class LocalKnowledgeRepository(context: Context) : KnowledgeRepository {

    private val prefs = context.getSharedPreferences("kb_prefs", Context.MODE_PRIVATE)
    private val _items = MutableStateFlow(loadPersisted())
    override val allItems: StateFlow<List<KnowledgeItem>> = _items.asStateFlow()

    override fun itemsFor(hotelId: String): List<KnowledgeItem> =
        _items.value.filter { it.hotelId == hotelId }

    override fun add(item: KnowledgeItem) {
        _items.value = listOf(item) + _items.value
        persist()
    }

    override fun remove(item: KnowledgeItem) {
        _items.value = _items.value - item
        persist()
    }

    private fun loadPersisted(): List<KnowledgeItem> {
        val raw = prefs.getString(KEY, null) ?: return SeedKnowledge.items
        return runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i -> fromJson(arr.getJSONObject(i)) }
        }.getOrDefault(SeedKnowledge.items)
    }

    private fun persist() {
        val arr = JSONArray()
        _items.value.forEach { arr.put(toJson(it)) }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    private fun toJson(item: KnowledgeItem) = JSONObject()
        .put("id", item.id)
        .put("hotelId", item.hotelId)
        .put("keywords", JSONArray(item.keywords))
        .put("answerAr", item.answerAr)
        .put("answerEn", item.answerEn)

    private fun fromJson(o: JSONObject) = KnowledgeItem(
        id = o.getString("id"),
        hotelId = o.getString("hotelId"),
        keywords = (0 until o.getJSONArray("keywords").length())
            .map { i -> o.getJSONArray("keywords").getString(i) },
        answerAr = o.getString("answerAr"),
        answerEn = o.getString("answerEn"),
    )

    private companion object {
        const val KEY = "kb"
    }
}

/** Sample knowledge for the two seeded hotels. */
object SeedKnowledge {

    val items: List<KnowledgeItem> = listOf(
        KnowledgeItem(
            id = "kb_golden_wifi",
            hotelId = "hotel_golden_ryd",
            keywords = listOf("واي فاي", "wifi", "انترنت", "internet", "شبكة", "باسوورد", "password"),
            answerAr = "شبكة Golden-Guest وكلمة المرور golden2026 - مجاناً في كل الغرف والأماكن العامة 📶",
            answerEn = "Network Golden-Guest, password golden2026 - free in all rooms and public areas 📶",
        ),
        KnowledgeItem(
            id = "kb_golden_checkin",
            hotelId = "hotel_golden_ryd",
            keywords = listOf("تسجيل", "وصول", "check-in", "checkin", "مغادرة", "checkout", "استلام"),
            answerAr = "تسجيل الوصول من 3 عصراً، والمغادرة حتى 12 ظهراً. التأخير متاح بطلب مسبق من الاستقبال.",
            answerEn = "Check-in from 3 PM, check-out until 12 PM. Late checkout available on request.",
        ),
        KnowledgeItem(
            id = "kb_golden_pool",
            hotelId = "hotel_golden_ryd",
            keywords = listOf("مسبح", "pool", "سباحة"),
            answerAr = "المسبح مفتوح يومياً من 8 صباحاً حتى 8 مساءً، والمناشف متاحة عند المسبح 🏊",
            answerEn = "The pool is open daily 8 AM - 8 PM, towels available poolside 🏊",
        ),
        KnowledgeItem(
            id = "kb_golden_breakfast",
            hotelId = "hotel_golden_ryd",
            keywords = listOf("فطور", "breakfast", "بوفيه"),
            answerAr = "البوفيه من 6:30 حتى 10:30 صباحاً في الطابق الأول - مطعم الواحة 🍳",
            answerEn = "Breakfast buffet 6:30-10:30 AM, first floor - Oasis restaurant 🍳",
        ),
        KnowledgeItem(
            id = "kb_golden_gym",
            hotelId = "hotel_golden_ryd",
            keywords = listOf("جيم", "رياضة", "gym", "fitness"),
            answerAr = "الصالة الرياضية مفتوحة 24 ساعة في الطابق السفلي، والدخول بالمفتاح من الاستقبال 🏋️",
            answerEn = "The gym is open 24/7 in the basement; get the key card from reception 🏋️",
        ),
        KnowledgeItem(
            id = "kb_golden_qibla",
            hotelId = "hotel_golden_ryd",
            keywords = listOf("قبلة", "qibla", "صلاة", "pray"),
            answerAr = "اتجاه القبلة مُعلّم على منضدة الغرفة، والمصلى في الطابق الأول بجانب الاستقبال 🕌",
            answerEn = "The qibla marker is on the room desk; the prayer room is on the first floor next to reception 🕌",
        ),
        KnowledgeItem(
            id = "kb_golden_menu",
            hotelId = "hotel_golden_ryd",
            keywords = listOf("منيو", "قائمة", "menu", "مطعم"),
            answerAr = "منيو خدمة الغرف: فطور 45 ر.س • برجر 55 ر.س • ساندويتش فلافل 30 ر.س • بيتزا 60 ر.س • شاي/قهوة 15 ر.س",
            answerEn = "Room service menu: Breakfast 45 SAR • Burger 55 SAR • Falafel sandwich 30 SAR • Pizza 60 SAR • Tea/coffee 15 SAR",
        ),
        KnowledgeItem(
            id = "kb_bahar_wifi",
            hotelId = "hotel_albahar_jed",
            keywords = listOf("واي فاي", "wifi", "انترنت", "internet", "شبكة", "باسوورد", "password"),
            answerAr = "شبكة Bahar-Guest وكلمة المرور bahar2026 - مجاناً في كل الغرف 📶",
            answerEn = "Network Bahar-Guest, password bahar2026 - free in all rooms 📶",
        ),
        KnowledgeItem(
            id = "kb_bahar_checkin",
            hotelId = "hotel_albahar_jed",
            keywords = listOf("تسجيل", "وصول", "check-in", "checkin", "مغادرة", "checkout"),
            answerAr = "تسجيل الوصول من 4 عصراً، والمغادرة حتى 12 ظهراً.",
            answerEn = "Check-in from 4 PM, check-out until 12 PM.",
        ),
        KnowledgeItem(
            id = "kb_bahar_beach",
            hotelId = "hotel_albahar_jed",
            keywords = listOf("بحر", "شاطئ", "beach", "sea"),
            answerAr = "الشاطئ الخاص أمام الفندق مباشرة، والكراسي والمظلات مجانية للضيوف من 8 صباحاً حتى الغروب 🏖️",
            answerEn = "The private beach is right in front of the hotel; chairs and umbrellas are free for guests 8 AM to sunset 🏖️",
        ),
        KnowledgeItem(
            id = "kb_bahar_parking",
            hotelId = "hotel_albahar_jed",
            keywords = listOf("مواقف", "parking", "سيارة", "car"),
            answerAr = "المواقف مجانية للضيوف في الطابق السفلي، والدخول ببطاقة الغرفة 🚗",
            answerEn = "Free guest parking in the basement, entry with your room card 🚗",
        ),
        KnowledgeItem(
            id = "kb_bahar_menu",
            hotelId = "hotel_albahar_jed",
            keywords = listOf("منيو", "قائمة", "menu", "مطعم"),
            answerAr = "منيو خدمة الغرف: فطور بحري 55 ر.س • سمك مشوي 85 ر.س • شريحة لحم 75 ر.س • سلطة 25 ر.س • عصير طازج 18 ر.س",
            answerEn = "Room service menu: Seafood breakfast 55 SAR • Grilled fish 85 SAR • Steak 75 SAR • Salad 25 SAR • Fresh juice 18 SAR",
        ),
    )
}
