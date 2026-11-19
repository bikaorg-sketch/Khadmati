package com.khadmati.app.domain.model

data class KnowledgeItem(
    val id: String,
    val hotelId: String,
    /** Lowercase keywords (ar + en) matched with contains(). */
    val keywords: List<String>,
    val answerAr: String,
    val answerEn: String,
)

enum class EscalationStatus { OPEN, RESOLVED }

data class Escalation(
    val id: String,
    val hotelId: String,
    val roomId: String,
    val guestMessage: String,
    val status: EscalationStatus,
    val createdAtMillis: Long,
)

data class ChatMessage(
    val text: String,
    val fromUser: Boolean,
    val millis: Long,
)

data class AssistantReply(
    val lines: List<String>,
    val escalated: Boolean,
)
