package com.khadmati.app.data

import com.khadmati.app.domain.model.Hotel
import com.khadmati.app.domain.model.HotelRoom
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

interface HotelRepository {
    val hotels: StateFlow<List<Hotel>>

    /** Observable for host UI (add-room updates live). */
    val roomsFlow: StateFlow<List<HotelRoom>>

    fun roomsFor(hotelId: String): List<HotelRoom>

    fun allRooms(): List<HotelRoom>

    fun room(roomId: String): HotelRoom?

    fun hotel(hotelId: String): Hotel?

    /** Adds a room to a hotel (host side). */
    fun addRoom(room: HotelRoom)
}

/**
 * Session-only seed of the owner's chain (2 hotels x 4 rooms). Properties and
 * rooms are demo data; requests/QR/KB persist across restarts like printed
 * stickers would.
 */
class InMemoryHotelRepository : HotelRepository {

    private val _hotels = MutableStateFlow(SeedChain.hotels)
    override val hotels: StateFlow<List<Hotel>> = _hotels.asStateFlow()

    private val _rooms = MutableStateFlow(SeedChain.rooms)
    override val roomsFlow: StateFlow<List<HotelRoom>> = _rooms.asStateFlow()

    override fun roomsFor(hotelId: String): List<HotelRoom> =
        _rooms.value.filter { it.hotelId == hotelId }

    override fun allRooms(): List<HotelRoom> = _rooms.value

    override fun room(roomId: String): HotelRoom? = _rooms.value.firstOrNull { it.id == roomId }

    override fun hotel(hotelId: String): Hotel? = _hotels.value.firstOrNull { it.id == hotelId }

    override fun addRoom(room: HotelRoom) {
        _rooms.value = _rooms.value + room
    }
}

/** The demo chain: Golden Madinah (Riyadh) + Al Bahar (Jeddah). */
object SeedChain {

    val hotels = listOf(
        Hotel(
            id = "hotel_golden_ryd",
            nameAr = "فندق المدينة الذهبية",
            nameEn = "Golden Madinah Hotel",
            cityAr = "الرياض",
            cityEn = "Riyadh",
            receptionPhone = "+966 11 234 5678",
        ),
        Hotel(
            id = "hotel_albahar_jed",
            nameAr = "فندق البحر",
            nameEn = "Al Bahar Hotel",
            cityAr = "جدة",
            cityEn = "Jeddah",
            receptionPhone = "+966 12 987 6543",
        ),
    )

    val rooms = listOf(
        HotelRoom("room_golden_302", "hotel_golden_ryd", "302", 3, "غرفة كينج", "King room"),
        HotelRoom("room_golden_304", "hotel_golden_ryd", "304", 3, "غرفة توين", "Twin room"),
        HotelRoom("room_golden_401", "hotel_golden_ryd", "401", 4, "سويت عائلي", "Family suite"),
        HotelRoom("room_golden_205", "hotel_golden_ryd", "205", 2, "غرفة سوبريور", "Superior room"),
        HotelRoom("room_bahar_1101", "hotel_albahar_jed", "1101", 11, "غرفة بحرية", "Sea-view room"),
        HotelRoom("room_bahar_1102", "hotel_albahar_jed", "1102", 11, "غرفة بحرية", "Sea-view room"),
        HotelRoom("room_bahar_1201", "hotel_albahar_jed", "1201", 12, "سويت جونيور", "Junior suite"),
        HotelRoom("room_bahar_1202", "hotel_albahar_jed", "1202", 12, "غرفة ديلوكس", "Deluxe room"),
    )
}
