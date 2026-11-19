package com.khadmati.app.ui.screens.host

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.khadmati.app.R
import com.khadmati.app.data.HotelRepository
import com.khadmati.app.data.KnowledgeRepository
import com.khadmati.app.data.QrRepository
import com.khadmati.app.data.ServiceRequestRepository
import com.khadmati.app.data.EscalationRepository
import com.khadmati.app.data.qr.QrGenerator
import com.khadmati.app.di.ServiceLocator
import com.khadmati.app.domain.model.EscalationStatus
import com.khadmati.app.domain.model.Hotel
import com.khadmati.app.domain.model.HotelRoom
import com.khadmati.app.domain.model.KnowledgeItem
import com.khadmati.app.domain.model.RequestStatus
import com.khadmati.app.domain.model.RequestType
import com.khadmati.app.domain.model.ServiceRequest
import com.khadmati.app.domain.model.tagAr
import com.khadmati.app.domain.model.tagEn
import com.khadmati.app.ui.theme.isArabicLocale
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HostHomeViewModel(
    private val hotelRepository: HotelRepository = ServiceLocator.hotelRepository,
    private val qrRepository: QrRepository = ServiceLocator.qrRepository,
    private val serviceRequestRepository: ServiceRequestRepository = ServiceLocator.serviceRequestRepository,
    private val knowledgeRepository: KnowledgeRepository = ServiceLocator.knowledgeRepository,
    private val escalationRepository: EscalationRepository = ServiceLocator.escalationRepository,
) : ViewModel() {

    val hotels = hotelRepository.hotels
    val rooms = hotelRepository.roomsFlow
    val qrTokens = qrRepository.activeTokens
    val requests = serviceRequestRepository.requests
    val kbItems = knowledgeRepository.allItems
    val escalations = escalationRepository.escalations

    private val _selectedHotelId = mutableStateOf(hotelRepository.hotels.value.first().id)
    val selectedHotelId: androidx.compose.runtime.State<String> = _selectedHotelId

    fun selectHotel(hotelId: String) {
        _selectedHotelId.value = hotelId
    }

    fun roomsFor(hotelId: String): List<HotelRoom> =
        rooms.value.filter { it.hotelId == hotelId }

    fun room(roomId: String): HotelRoom? = hotelRepository.room(roomId)

    /** Creates (or rotates) the room's printed QR token. */
    fun generateQr(roomId: String): String = qrRepository.generate(roomId)

    fun deepLink(roomId: String, token: String): String =
        "https://khadmati.app/r/$roomId?t=$token"

    /** Saves a print-ready PNG into Pictures/Khadmati (API 29+). */
    fun saveQrPng(context: Context, roomId: String, token: String): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val bitmap = QrGenerator.bitmap(deepLink(roomId, token))
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "khadmati-qr-$roomId.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Khadmati")
        }
        return runCatching {
            val uri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values,
            ) ?: return null
            context.contentResolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            uri.toString()
        }.getOrNull()
    }

    fun updateRequestStatus(requestId: String, status: RequestStatus) =
        serviceRequestRepository.updateStatus(requestId, status)

    fun addKnowledge(hotelId: String, keywordsRaw: String, answerAr: String, answerEn: String) {
        val keywords = keywordsRaw.split(',', '،').map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        if (keywords.isEmpty() || answerAr.isBlank()) return
        knowledgeRepository.add(
            KnowledgeItem(
                id = "kb_${System.currentTimeMillis()}",
                hotelId = hotelId,
                keywords = keywords,
                answerAr = answerAr,
                answerEn = answerEn.ifBlank { answerAr },
            ),
        )
    }

    fun removeKnowledge(item: KnowledgeItem) = knowledgeRepository.remove(item)

    fun resolveEscalation(id: String) = escalationRepository.resolve(id)

    fun addRoom(hotelId: String, number: String, floor: Int, type: String) {
        if (number.isBlank()) return
        hotelRepository.addRoom(
            HotelRoom(
                id = "room_${number}_${System.currentTimeMillis()}",
                hotelId = hotelId,
                number = number.trim(),
                floor = floor,
                typeAr = type.ifBlank { number },
                typeEn = type.ifBlank { number },
            ),
        )
    }
}

/** Owner console: rooms & QR, service requests, knowledge base, escalations. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HostHomeScreen() {
    val viewModel: HostHomeViewModel = viewModel(
        factory = viewModelFactory { initializer { HostHomeViewModel() } },
    )
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        stringResource(R.string.tab_rooms),
        stringResource(R.string.tab_requests),
        stringResource(R.string.tab_kb),
        stringResource(R.string.tab_escalations),
    )

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.app_name)) })
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            TabRow(selectedTabIndex = tab) {
                tabs.forEachIndexed { index, label ->
                    Tab(
                        selected = tab == index,
                        onClick = { tab = index },
                        text = {
                            Text(label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        },
                    )
                }
            }
            when (tab) {
                0 -> RoomsQrTab(viewModel)
                1 -> RequestsTab(viewModel)
                2 -> KnowledgeTab(viewModel)
                else -> EscalationsTab(viewModel)
            }
        }
    }
}

@Composable
private fun HotelSelector(viewModel: HostHomeViewModel) {
    val hotels by viewModel.hotels.collectAsStateWithLifecycle()
    val selectedId by viewModel.selectedHotelId
    val isArabic = isArabicLocale()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        hotels.forEach { hotel: Hotel ->
            FilterChip(
                selected = hotel.id == selectedId,
                onClick = { viewModel.selectHotel(hotel.id) },
                label = { Text(if (isArabic) hotel.nameAr else hotel.nameEn) },
            )
        }
    }
}

// ---------- Tab 1: Rooms & QR ----------

@Composable
private fun RoomsQrTab(viewModel: HostHomeViewModel) {
    val isArabic = isArabicLocale()
    val context = LocalContext.current
    val rooms by viewModel.rooms.collectAsStateWithLifecycle()
    val qrTokens by viewModel.qrTokens.collectAsStateWithLifecycle()
    val selectedId by viewModel.selectedHotelId

    var qrRoomId by remember { mutableStateOf<String?>(null) }
    var showAddRoom by remember { mutableStateOf(false) }
    val localRooms = rooms.filter { it.hotelId == selectedId }

    Column(modifier = Modifier.fillMaxSize()) {
        HotelSelector(viewModel)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (isArabic) "غرف الفندق (${localRooms.size})" else "Rooms (${localRooms.size})",
                style = MaterialTheme.typography.titleMedium,
            )
            Button(onClick = { showAddRoom = true }) {
                Text(stringResource(R.string.rooms_add))
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            localRooms.forEach { room ->
                val token = qrTokens[room.id]
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isArabic) "غرفة ${room.number}" else "Room ${room.number}",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = " • ${if (isArabic) room.typeAr else room.typeEn} • ${if (isArabic) "طابق" else "floor"} ${room.floor}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = token?.let { stringResource(R.string.room_rotate) + " • " + it.take(10) + "…" }
                                ?: stringResource(R.string.room_no_qr),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Row(
                            modifier = Modifier.padding(top = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(onClick = {
                                viewModel.generateQr(room.id)
                                qrRoomId = room.id
                            }) {
                                Text(stringResource(if (token == null) R.string.room_generate else R.string.room_rotate))
                            }
                            if (token != null) {
                                OutlinedButton(onClick = { viewModel.saveQrPng(context, room.id, token) }) {
                                    Text(stringResource(R.string.room_save_png))
                                }
                                OutlinedButton(onClick = { qrRoomId = room.id }) {
                                    Text(stringResource(R.string.qr_link_label))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    qrRoomId?.let { roomId ->
        val token = remember(roomId, qrTokens) { viewModel.qrTokens.value[roomId] }
        if (token != null) {
            QrDialog(
                roomId = roomId,
                token = token,
                viewModel = viewModel,
                isArabic = isArabic,
                onDismiss = { qrRoomId = null },
            )
        }
    }

    if (showAddRoom) {
        AddRoomDialog(
            isArabic = isArabic,
            onDismiss = { showAddRoom = false },
            onAdd = { number, floor, type ->
                viewModel.addRoom(selectedId, number, floor, type)
                showAddRoom = false
            },
        )
    }
}

@Composable
private fun QrDialog(
    roomId: String,
    token: String,
    viewModel: HostHomeViewModel,
    isArabic: Boolean,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val room = viewModel.room(roomId)
    val link = viewModel.deepLink(roomId, token)
    val bitmap = remember(token) { QrGenerator.bitmap(link).asImageBitmap() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isArabic) "QR غرفة ${room?.number.orEmpty()}" else "QR room ${room?.number.orEmpty()}",
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    modifier = Modifier
                        .width(240.dp)
                        .height(240.dp)
                        .background(androidx.compose.ui.graphics.Color.White),
                )
                Text(
                    text = stringResource(R.string.qr_link_label),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    text = link,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = if (isArabic) "اطبع الـ PNG وعلّقه على باب/منضدة الغرفة" else "Print the PNG and place it on the room door/desk",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Text(
                    text = stringResource(R.string.qr_fallback_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                viewModel.saveQrPng(context, roomId, token)
                onDismiss()
            }) {
                Text(stringResource(R.string.room_save_png))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun AddRoomDialog(
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onAdd: (number: String, floor: Int, type: String) -> Unit,
) {
    var number by remember { mutableStateOf("") }
    var floor by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rooms_add)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text(stringResource(R.string.add_room_number)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = floor,
                    onValueChange = { floor = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.add_room_floor)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = type,
                    onValueChange = { type = it },
                    label = { Text(stringResource(R.string.add_room_type)) },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(number, floor.toIntOrNull() ?: 1, type) },
                enabled = number.isNotBlank(),
            ) {
                Text(stringResource(R.string.add_room_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

// ---------- Tab 2: Requests ----------

@Composable
private fun RequestsTab(viewModel: HostHomeViewModel) {
    val isArabic = isArabicLocale()
    val requests by viewModel.requests.collectAsStateWithLifecycle()
    if (requests.isEmpty()) {
        EmptyNote(stringResource(R.string.req_none))
        return
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        requests.forEach { request ->
            val room = viewModel.room(request.roomId)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusChip(request.status)
                        Text(
                            text = " • ${if (isArabic) request.type.tagAr else request.type.tagEn}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = " • ${formatTime(request.createdAtMillis)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        text = if (isArabic) {
                            "${stringResource(R.string.req_room_prefix)} ${room?.number.orEmpty()} • ${request.items.joinToString("، ")}"
                        } else {
                            "${stringResource(R.string.req_room_prefix)} ${room?.number.orEmpty()} • ${request.items.joinToString(", ")}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        when (request.status) {
                            RequestStatus.OPEN -> {
                                OutlinedButton(onClick = {
                                    viewModel.updateRequestStatus(request.id, RequestStatus.IN_PROGRESS)
                                }) { Text(stringResource(R.string.req_mark_progress)) }
                                Button(onClick = {
                                    viewModel.updateRequestStatus(request.id, RequestStatus.DONE)
                                }) { Text(stringResource(R.string.req_mark_done)) }
                            }
                            RequestStatus.IN_PROGRESS -> {
                                Button(onClick = {
                                    viewModel.updateRequestStatus(request.id, RequestStatus.DONE)
                                }) { Text(stringResource(R.string.req_mark_done)) }
                            }
                            RequestStatus.DONE -> Unit
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusChip(status: RequestStatus) {
    val isArabic = isArabicLocale()
    val (label, container, content) = when (status) {
        RequestStatus.OPEN -> Triple(
            stringResource(R.string.req_open),
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
        )
        RequestStatus.IN_PROGRESS -> Triple(
            stringResource(R.string.req_in_progress),
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
        )
        RequestStatus.DONE -> Triple(
            stringResource(R.string.req_done),
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
    Surface(color = container, shape = MaterialTheme.shapes.small) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = content,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

// ---------- Tab 3: Knowledge base ----------

@Composable
private fun KnowledgeTab(viewModel: HostHomeViewModel) {
    val isArabic = isArabicLocale()
    val kbItems by viewModel.kbItems.collectAsStateWithLifecycle()
    val selectedId by viewModel.selectedHotelId
    var showAdd by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        HotelSelector(viewModel)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Button(onClick = { showAdd = true }) { Text(stringResource(R.string.kb_add)) }
        }
        val items = kbItems.filter { it.hotelId == selectedId }
        if (items.isEmpty()) {
            EmptyNote(stringResource(R.string.kb_none))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items.forEach { item ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = item.keywords.joinToString(" · "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = if (isArabic) item.answerAr else item.answerEn,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                            TextButton(
                                onClick = { viewModel.removeKnowledge(item) },
                                modifier = Modifier.align(Alignment.End),
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = null)
                                Text(stringResource(R.string.cancel))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddKnowledgeDialog(
            isArabic = isArabic,
            onDismiss = { showAdd = false },
            onSave = { keywords, ar, en ->
                viewModel.addKnowledge(selectedId, keywords, ar, en)
                showAdd = false
            },
        )
    }
}

@Composable
private fun AddKnowledgeDialog(
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onSave: (keywords: String, answerAr: String, answerEn: String) -> Unit,
) {
    var keywords by remember { mutableStateOf("") }
    var answerAr by remember { mutableStateOf("") }
    var answerEn by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.kb_add)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = keywords,
                    onValueChange = { keywords = it },
                    label = { Text(stringResource(R.string.kb_keywords)) },
                )
                OutlinedTextField(
                    value = answerAr,
                    onValueChange = { answerAr = it },
                    label = { Text(stringResource(R.string.kb_answer_ar)) },
                )
                OutlinedTextField(
                    value = answerEn,
                    onValueChange = { answerEn = it },
                    label = { Text(stringResource(R.string.kb_answer_en)) },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(keywords, answerAr, answerEn) },
                enabled = keywords.isNotBlank() && answerAr.isNotBlank(),
            ) { Text(stringResource(R.string.kb_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

// ---------- Tab 4: Escalations ----------

@Composable
private fun EscalationsTab(viewModel: HostHomeViewModel) {
    val isArabic = isArabicLocale()
    val escalations by viewModel.escalations.collectAsStateWithLifecycle()
    val open = escalations.filter { it.status == EscalationStatus.OPEN }
    if (open.isEmpty()) {
        EmptyNote(stringResource(R.string.esc_none))
        return
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        open.forEach { escalation ->
            val room = viewModel.room(escalation.roomId)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isArabic) {
                            "${stringResource(R.string.req_room_prefix)} ${room?.number.orEmpty()} • ${formatTime(escalation.createdAtMillis)}"
                        } else {
                            "${stringResource(R.string.req_room_prefix)} ${room?.number.orEmpty()} • ${formatTime(escalation.createdAtMillis)}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = escalation.guestMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                    Button(
                        onClick = { viewModel.resolveEscalation(escalation.id) },
                        modifier = Modifier.padding(top = 8.dp),
                    ) { Text(stringResource(R.string.esc_resolve)) }
                }
            }
        }
    }
}

@Composable
private fun EmptyNote(text: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun formatTime(millis: Long): String =
    SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(millis))
