package com.khadmati.app

import com.khadmati.app.data.InMemoryHotelRepository
import com.khadmati.app.data.LocalKbAssistant
import com.khadmati.app.data.SeedKnowledge
import com.khadmati.app.ui.screens.chat.ChatViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** ChatViewModel wiring: welcome bubble, typing state, request side effects. */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private lateinit var requests: FakeServiceRequestRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        requests = FakeServiceRequestRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(arabic: Boolean = true) = ChatViewModel(
        roomId = "room_golden_302",
        arabic = arabic,
        hotelRepository = InMemoryHotelRepository(),
        assistant = LocalKbAssistant(
            InMemoryHotelRepository(),
            com.khadmati.app.FakeKnowledgeRepository(SeedKnowledge.items),
            requests,
            FakeEscalationRepository(),
        ),
    )

    @Test
    fun welcomeBubbleMentionsRoomAndHotel() = runTest(dispatcher) {
        val vm = viewModel()
        val welcome = vm.messages.value.first()
        assertTrue(welcome.text.contains("302"))
        assertTrue(welcome.text.contains("المدينة الذهبية"))
        assertEquals(false, welcome.fromUser)
    }

    @Test
    fun sendQuickCreatesRequest_andAppendsBubbles() = runTest(dispatcher) {
        val vm = viewModel()
        vm.sendQuick("عايز مناشف")
        advanceUntilIdle()
        assertEquals(1, requests.requests.value.size)
        val texts = vm.messages.value.map { it.text }
        assertTrue(texts.any { it.contains("هيوصلك مناشف") })
        assertTrue(vm.messages.value.last().fromUser == false)
    }

    @Test
    fun typedInputFlowsThroughSend() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onInputChange("إيه باسوورد الواي فاي؟")
        vm.send()
        advanceUntilIdle()
        assertTrue(vm.input.isEmpty())
        assertTrue(vm.messages.value.last().text.contains("golden2026"))
        assertEquals(0, requests.requests.value.size)
    }

    @Test
    fun englishChatUsesEnglishContext() = runTest(dispatcher) {
        val vm = viewModel(arabic = false)
        assertTrue(vm.hotelName == "Golden Madinah Hotel")
        vm.sendQuick("Please bring towels")
        advanceUntilIdle()
        assertTrue(vm.messages.value.last().text.contains("room 302"))
    }
}
