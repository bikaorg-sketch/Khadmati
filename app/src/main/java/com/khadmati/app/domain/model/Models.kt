package com.khadmati.app.domain.model

/** A hotel in the owner's chain. Data language pairs are picked by app locale. */
data class Hotel(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val cityAr: String,
    val cityEn: String,
    /** Front-desk phone shown when the assistant cannot help. */
    val receptionPhone: String,
)

/** A physical room with its own printed QR. */
data class HotelRoom(
    val id: String,
    val hotelId: String,
    /** Room number as printed on the door, e.g. "302". */
    val number: String,
    val floor: Int,
    val typeAr: String,
    val typeEn: String,
)

enum class RequestType { AMENITIES, ROOM_SERVICE, HOUSEKEEPING, MAINTENANCE, OTHER }

enum class RequestStatus { OPEN, IN_PROGRESS, DONE }

/** A guest service request created from the chat (items, room service, cleaning...). */
data class ServiceRequest(
    val id: String,
    val hotelId: String,
    val roomId: String,
    val type: RequestType,
    /** Concrete items when the guest picked/typed them, e.g. towels, water. */
    val items: List<String>,
    val status: RequestStatus,
    val createdAtMillis: Long,
)

/** Short display label for a request type (UI-neutral, resolved to strings by screens). */
val RequestType.tagAr: String
    get() = when (this) {
        RequestType.AMENITIES -> "أغراض الغرفة"
        RequestType.ROOM_SERVICE -> "خدمة الغرف"
        RequestType.HOUSEKEEPING -> "تنظيف"
        RequestType.MAINTENANCE -> "صيانة"
        RequestType.OTHER -> "أخرى"
    }

val RequestType.tagEn: String
    get() = when (this) {
        RequestType.AMENITIES -> "Room amenities"
        RequestType.ROOM_SERVICE -> "Room service"
        RequestType.HOUSEKEEPING -> "Housekeeping"
        RequestType.MAINTENANCE -> "Maintenance"
        RequestType.OTHER -> "Other"
    }
