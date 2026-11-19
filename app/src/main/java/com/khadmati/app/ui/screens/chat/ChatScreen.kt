package com.khadmati.app.ui.screens.chat

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.khadmati.app.R
import com.khadmati.app.di.ServiceLocator
import com.khadmati.app.domain.model.ChatMessage
import com.khadmati.app.ui.theme.isArabicLocale
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The guest chat - opened directly by scanning the room QR (deep link) or
 * from the demo list. No sign-in, no hub: the printed card is the entry.
 */
@Composable
fun ChatScreen(
    roomId: String,
    token: String?,
    onBack: () -> Unit,
) {
    val isArabic = isArabicLocale()
    val room = remember(roomId) { ServiceLocator.hotelRepository.room(roomId) }
    // token == null means demo entry from guest home (no printed card scanned).
    val tokenValid = remember(roomId, token) {
        when {
            room == null -> false
            token == null -> true
            else -> ServiceLocator.qrRepository.isValid(roomId, token)
        }
    }
    if (room == null || !tokenValid) {
        InvalidCodeState(roomId = roomId, onBack = onBack)
        return
    }

    val viewModel: ChatViewModel = viewModel(
        key = "chat_$roomId",
        factory = viewModelFactory {
            initializer { ChatViewModel(roomId = roomId, arabic = isArabic) }
        },
    )
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val thinking by viewModel.thinking.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, thinking) {
        val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
        val target = if (thinking) messages.size else messages.size - 1
        if (target >= 0 && target != last) {
            listState.animateScrollToItem(target)
        }
    }

    var sheet by remember { mutableStateOf<ChatSheet?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            // Gradient header: brand + hotel + room context.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary),
                        ),
                    )
                    .statusBarsPadding()
                    .padding(vertical = 10.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = viewModel.hotelName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Text(
                            text = if (isArabic) {
                                "غرفة ${viewModel.roomNumber} • ${viewModel.roomType}"
                            } else {
                                "Room ${viewModel.roomNumber} • ${viewModel.roomType}"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (isArabic) "خ" else "K",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(messages) { message ->
                    Bubble(message)
                }
                if (thinking) {
                    item(key = "typing") { TypingBubble() }
                }
            }

            val wifiQuestion = stringResource(R.string.faq_q_wifi)
            QuickChipsRow(
                enabled = !thinking,
                onWifi = { viewModel.sendQuick(wifiQuestion) },
                onAmenities = { sheet = ChatSheet.AMENITIES },
                onService = { sheet = ChatSheet.FOOD },
                onClean = {
                    viewModel.sendQuick(
                        if (isArabic) "أريد تنظيف الغرفة من فضلك" else "Please clean my room",
                    )
                },
                onFaq = { sheet = ChatSheet.FAQ },
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = viewModel.input,
                    onValueChange = viewModel::onInputChange,
                    placeholder = { Text(stringResource(R.string.chat_input_hint)) },
                    modifier = Modifier.weight(1f),
                    maxLines = 3,
                    shape = MaterialTheme.shapes.large,
                )
                FilledIconButton(
                    onClick = viewModel::send,
                    enabled = viewModel.input.isNotBlank() && !thinking,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
                }
            }
        }
    }

    when (sheet) {
        ChatSheet.AMENITIES -> AmenitiesSheet(
            onDismiss = { sheet = null },
            onConfirm = { items ->
                sheet = null
                viewModel.sendQuick(items.joinToString(if (isArabic) " و" else " & "))
            },
        )
        ChatSheet.FOOD -> FoodSheet(
            onDismiss = { sheet = null },
            onConfirm = { items ->
                sheet = null
                viewModel.sendQuick(items.joinToString(if (isArabic) " و" else " & "))
            },
        )
        ChatSheet.FAQ -> FaqSheet(
            onDismiss = { sheet = null },
            onQuestion = { question ->
                sheet = null
                viewModel.sendQuick(question)
            },
        )
        null -> Unit
    }
}

private enum class ChatSheet { AMENITIES, FOOD, FAQ }

@Composable
private fun Bubble(message: ChatMessage) {
    val fromUser = message.fromUser
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (fromUser) Alignment.End else Alignment.Start,
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomEnd = if (fromUser) 4.dp else 14.dp,
                bottomStart = if (fromUser) 14.dp else 4.dp,
            ),
            color = if (fromUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyMedium,
                color = if (fromUser) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
        Text(
            text = formatTime(message.millis),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun TypingBubble() {
    val transition = rememberInfiniteTransition(label = "typing")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "typingAlpha",
    )
    Surface(
        shape = RoundedCornerShape(
            topStart = 14.dp,
            topEnd = 14.dp,
            bottomStart = 4.dp,
            bottomEnd = 14.dp,
        ),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(3) { index ->
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                            CircleShape,
                        ),
                )
            }
        }
    }
}

@Composable
private fun QuickChipsRow(
    enabled: Boolean,
    onWifi: () -> Unit,
    onAmenities: () -> Unit,
    onService: () -> Unit,
    onClean: () -> Unit,
    onFaq: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AssistChip(onClick = onWifi, label = { Text(stringResource(R.string.chip_wifi)) }, enabled = enabled)
        AssistChip(onClick = onAmenities, label = { Text(stringResource(R.string.chip_amenities)) }, enabled = enabled)
        AssistChip(onClick = onService, label = { Text(stringResource(R.string.chip_service)) }, enabled = enabled)
        AssistChip(onClick = onClean, label = { Text(stringResource(R.string.chip_clean)) }, enabled = enabled)
        AssistChip(onClick = onFaq, label = { Text(stringResource(R.string.chip_faq)) }, enabled = enabled)
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun AmenitiesSheet(
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit,
) {
    val labels = listOf(
        stringResource(R.string.amenity_towels),
        stringResource(R.string.amenity_tp),
        stringResource(R.string.amenity_shampoo),
        stringResource(R.string.amenity_soap),
        stringResource(R.string.amenity_water),
        stringResource(R.string.amenity_pillows),
        stringResource(R.string.amenity_blanket),
    )
    val selected = remember { mutableStateOf(setOf<Int>()) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.sheet_amenities_title),
                style = MaterialTheme.typography.titleMedium,
            )
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                labels.forEachIndexed { index, label ->
                    FilterChip(
                        selected = index in selected.value,
                        onClick = {
                            selected.value = if (index in selected.value) {
                                selected.value - index
                            } else {
                                selected.value + index
                            }
                        },
                        label = { Text(label) },
                    )
                }
            }
            if (selected.value.isEmpty()) {
                Text(
                    text = stringResource(R.string.sheet_pick_one),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = { onConfirm(selected.value.map { labels[it] }) },
                enabled = selected.value.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            ) {
                Text(stringResource(R.string.sheet_confirm))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun FoodSheet(
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit,
) {
    val labels = listOf(
        stringResource(R.string.food_breakfast),
        stringResource(R.string.food_burger),
        stringResource(R.string.food_falafel),
        stringResource(R.string.food_pizza),
        stringResource(R.string.food_fish),
        stringResource(R.string.food_steak),
        stringResource(R.string.food_salad),
        stringResource(R.string.food_juice),
        stringResource(R.string.food_tea),
        stringResource(R.string.food_coffee),
    )
    val selected = remember { mutableStateOf(setOf<Int>()) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.sheet_food_title),
                style = MaterialTheme.typography.titleMedium,
            )
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                labels.forEachIndexed { index, label ->
                    FilterChip(
                        selected = index in selected.value,
                        onClick = {
                            selected.value = if (index in selected.value) {
                                selected.value - index
                            } else {
                                selected.value + index
                            }
                        },
                        label = { Text(label) },
                    )
                }
            }
            if (selected.value.isEmpty()) {
                Text(
                    text = stringResource(R.string.sheet_pick_one),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = { onConfirm(selected.value.map { labels[it] }) },
                enabled = selected.value.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            ) {
                Text(stringResource(R.string.sheet_confirm))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FaqSheet(
    onDismiss: () -> Unit,
    onQuestion: (String) -> Unit,
) {
    val questions = listOf(
        stringResource(R.string.faq_q_wifi),
        stringResource(R.string.faq_q_checkin),
        stringResource(R.string.faq_q_pool),
        stringResource(R.string.faq_q_breakfast),
        stringResource(R.string.faq_q_gym),
        stringResource(R.string.faq_q_qibla),
        stringResource(R.string.faq_q_parking),
    )
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.faq_sheet_title),
                style = MaterialTheme.typography.titleMedium,
            )
            questions.forEach { question ->
                OutlinedButton(
                    onClick = { onQuestion(question) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                ) {
                    Text(question, textAlign = TextAlign.Start)
                }
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    }
}

@Composable
private fun InvalidCodeState(roomId: String, onBack: () -> Unit) {
    val hotel = remember(roomId) {
        ServiceLocator.hotelRepository.room(roomId)?.let {
            ServiceLocator.hotelRepository.hotel(it.hotelId)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.chat_invalid_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.chat_invalid_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
                if (hotel != null) {
                    Text(
                        text = hotel.receptionPhone,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                Button(
                    onClick = onBack,
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))
