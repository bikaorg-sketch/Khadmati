package com.khadmati.app.ui.screens.chat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.khadmati.app.data.AssistantContext
import com.khadmati.app.data.AssistantRepository
import com.khadmati.app.data.HotelRepository
import com.khadmati.app.di.ServiceLocator
import com.khadmati.app.domain.model.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Holds one room's chat session. Messages are session-only (like a live
 * conversation); requests/escalations created by the brain persist.
 */
class ChatViewModel(
    private val roomId: String,
    private val arabic: Boolean,
    private val hotelRepository: HotelRepository = ServiceLocator.hotelRepository,
    private val assistant: AssistantRepository = ServiceLocator.assistantRepository,
) : ViewModel() {

    private val room = hotelRepository.room(roomId)
    private val hotel = room?.let { hotelRepository.hotel(it.hotelId) }

    val roomNumber: String = room?.number.orEmpty()
    val roomType: String = room?.let { if (arabic) it.typeAr else it.typeEn }.orEmpty()
    val hotelName: String = hotel?.let { if (arabic) it.nameAr else it.nameEn }.orEmpty()
    val receptionPhone: String = hotel?.receptionPhone.orEmpty()

    private val _messages = MutableStateFlow(listOf(welcome()))
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _thinking = MutableStateFlow(false)
    val thinking: StateFlow<Boolean> = _thinking.asStateFlow()

    var input by mutableStateOf("")
        private set

    private fun welcome() = ChatMessage(
        text = if (arabic) {
            "أهلاً وسهلاً في غرفة $roomNumber بـ$hotelName 🌟 أنا خدمتي، مساعدك الذكي. اسألني أي حاجة: باسوورد الواي فاي، طلب أغراض، خدمة الغرف، مواعيد… أو استخدم الأزرار السريعة تحت 👇"
        } else {
            "Welcome to room $roomNumber at $hotelName 🌟 I am Khadmati, your smart assistant. Ask me anything: WiFi password, item requests, room service, schedules… or use the quick buttons below 👇"
        },
        fromUser = false,
        millis = System.currentTimeMillis(),
    )

    fun onInputChange(value: String) {
        input = value
    }

    /** Sends the typed text. */
    fun send() {
        val text = input.trim()
        if (text.isEmpty()) return
        input = ""
        respond(text)
    }

    /** Sends a quick action text (chips / sheets). */
    fun sendQuick(text: String) {
        if (_thinking.value) return
        respond(text)
    }

    private fun respond(text: String) {
        viewModelScope.launch {
            _messages.value = _messages.value + ChatMessage(text, true, System.currentTimeMillis())
            _thinking.value = true
            val reply = assistant.reply(
                text,
                AssistantContext(
                    hotelId = hotel?.id.orEmpty(),
                    roomId = roomId,
                    roomNumber = roomNumber,
                    arabic = arabic,
                ),
            )
            _thinking.value = false
            _messages.value = _messages.value + reply.lines.map {
                ChatMessage(it, false, System.currentTimeMillis())
            }
        }
    }
}
