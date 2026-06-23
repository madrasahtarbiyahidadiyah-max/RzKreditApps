package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import coil.request.ImageRequest
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.content.ContentValues
import android.provider.MediaStore
import android.os.Build
import android.os.Environment
import java.io.OutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.net.HttpURLConnection

// Core Data Model for Borrower
data class Peminjam(
    val nama: String,
    val nominal: Long,
    val tenor: Int,
    val angsuranPerBulan: Long,
    val totalTagihan: Long,
    val totalSetor: Long,
    val sisaTagihan: Long,
    val sisaTenor: Int
)

// Helper for currency formatting
fun formatRupiah(value: Long): String {
    val str = value.toString().reversed()
    val sb = StringBuilder()
    for (i in str.indices) {
        if (i > 0 && i % 3 == 0) {
            sb.append('.')
        }
        sb.append(str[i])
    }
    return "Rp " + sb.reverse().toString()
}

// Logic matches code.gs hitungAngsuran
fun hitungAngsuran(nominal: Long): Long {
    return when (nominal) {
        1000000L -> 110000L
        1500000L -> 170000L
        2000000L -> 220000L
        2500000L -> 270000L
        3000000L -> 320000L
        3500000L -> 370000L
        4000000L -> 420000L
        4500000L -> 470000L
        5000000L -> 530000L
        else -> 0L
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onLogout: () -> Unit,
    isDarkMode: Boolean,
    onToggleTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Shared state matching simulated google sheets data
    var listNasabah by remember {
        mutableStateOf(
            listOf(
                Peminjam("Lutfia Karim", 2500000, 12, 270000, 270000 * 12, 270000, (270000 * 12) - 270000, 11),
                Peminjam("Ahmad Fauzi", 1500000, 12, 170000, 170000 * 12, 0, 170000 * 12, 12),
                Peminjam("Siti Aminah", 5000000, 24, 530000, 530000 * 24, 1060000, (530000 * 24) - 1060000, 22)
            )
        )
    }

    // Modal state for calculator
    var showCalcDialog by remember { mutableStateOf(false) }

    // Active actions state
    var selectedReceiptPeminjam by remember { mutableStateOf<Peminjam?>(null) }
    var selectedDeletePeminjam by remember { mutableStateOf<Peminjam?>(null) }

    // Forms inputs state
    var namaBaru by remember { mutableStateOf("") }
    var nominalBaru by remember { mutableStateOf(1000000L) }
    var tenorBaru by remember { mutableStateOf(12) }

    var selectedNasabahForSetoran by remember { mutableStateOf("") }
    var jumlahSetoran by remember { mutableStateOf("") }

    // Toast/Feedback state
    var feedbackMessage by remember { mutableStateOf("") }
    var isSyncing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Calculations based on listNasabah
    val totalNasabah = listNasabah.size
    val totalUangDihutang = listNasabah.sumOf { it.nominal }
    val totalUangMasuk = listNasabah.sumOf { it.totalSetor }
    val totalPiutang = listNasabah.sumOf { it.sisaTagihan }

    val context = LocalContext.current

    // Colors matching obsidian style (with dynamic light/dark mode variants)
    val obsidianDark = if (isDarkMode) Color(0xFF060913) else Color(0xFFF1F5F9)
    val panelBg = if (isDarkMode) Color(0xFF0D1322) else Color(0xFFFFFFFF)
    val emeraldAccent = if (isDarkMode) Color(0xFF10B981) else Color(0xFF059669)
    val infoBlue = if (isDarkMode) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val warningGold = if (isDarkMode) Color(0xFFFBBF24) else Color(0xFFD97706)
    val dangerRose = if (isDarkMode) Color(0xFFF43F5E) else Color(0xFFDC2626)
    val textMain = if (isDarkMode) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSub = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF475569)
    val borderTone = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFE2E8F0)
    val gridBorderTone = if (isDarkMode) Color(0xFF334155) else Color(0xFFCBD5E1)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data("https://i.postimg.cc/3x9HWhJ2/Whats-App-Image-2026-06-02-at-10-33-41.jpg")
                                .placeholder(R.drawable.app_logo)
                                .error(R.drawable.app_logo)
                                .fallback(R.drawable.app_logo)
                                .crossfade(true)
                                .build(),
                            contentDescription = "RzKredit Logo",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, emeraldAccent, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Column {
                            Text(
                                text = "RzKredit Mobile",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = textMain
                                )
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(emeraldAccent, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Online Admin Panel",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 10.sp,
                                        color = textSub,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onToggleTheme,
                        modifier = Modifier.testTag("theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.WbSunny else Icons.Default.NightsStay,
                            contentDescription = if (isDarkMode) "Mode Terang" else "Mode Gelap",
                            tint = if (isDarkMode) Color(0xFFFBBF24) else Color(0xFF475569)
                        )
                    }
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier.testTag("logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Log Keluar",
                            tint = dangerRose
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = panelBg
                )
            )
        },
        containerColor = obsidianDark,
        modifier = modifier.testTag("dashboard_screen")
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                // Greeting and Quick Calculator Access
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = panelBg),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, borderTone, RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Halo, RzKarim",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Black,
                                        color = textMain,
                                        letterSpacing = (-0.5).sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Selamat bertugas kembali di portal administrator.",
                                    style = MaterialTheme.typography.bodyMedium.copy(color = textSub)
                                )
                            }
                        }
                        
                        HorizontalDivider(color = borderTone, thickness = 1.dp)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(
                                onClick = { showCalcDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = infoBlue),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("calc_simulasi_button")
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = Color.Black)
                                    Text(
                                        text = "Simulasi Kalkulator Kredit",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }
                }

                // Grid of statistics
                item {
                    Text(
                        text = "METRIK KASIR",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = textSub,
                            letterSpacing = 1.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "Debitur",
                            value = "$totalNasabah",
                            color = infoBlue,
                            icon = Icons.Default.Person,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Akumulasi Piutang",
                            value = formatRupiah(totalPiutang),
                            color = dangerRose,
                            icon = Icons.Default.Warning,
                            modifier = Modifier.weight(1.2f)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "Uang Masuk",
                            value = formatRupiah(totalUangMasuk),
                            color = emeraldAccent,
                            icon = Icons.Default.Check,
                            modifier = Modifier.weight(1.2f)
                        )
                        StatCard(
                            title = "Total Pembiayaan",
                            value = formatRupiah(totalUangDihutang),
                            color = warningGold,
                            icon = Icons.Default.Home,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Registers & Installments Inputs
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. New Borrower Column
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, borderTone, RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = panelBg),
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "1. Pendaftaran Baru",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = infoBlue
                                    )
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = namaBaru,
                                    onValueChange = { namaBaru = it },
                                    label = { Text("Nama Lengkap") },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = textMain,
                                        unfocusedTextColor = textMain,
                                        focusedBorderColor = infoBlue,
                                        unfocusedBorderColor = gridBorderTone,
                                        focusedLabelColor = infoBlue,
                                        unfocusedLabelColor = textSub
                                    ),
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_nama_baru")
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                var showNomSelection by remember { mutableStateOf(false) }
                                val nominals = listOf(1000000L, 1500000L, 2000000L, 2500000L, 3000000L, 3500000L, 4000000L, 4500000L, 5000000L)

                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = formatRupiah(nominalBaru),
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Plafon Pinjaman") },
                                        trailingIcon = {
                                            IconButton(onClick = { showNomSelection = true }) {
                                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = textSub)
                                            }
                                        },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = textMain,
                                            unfocusedTextColor = textMain,
                                            focusedBorderColor = infoBlue,
                                            unfocusedBorderColor = gridBorderTone,
                                            focusedLabelColor = infoBlue,
                                            unfocusedLabelColor = textSub
                                        ),
                                        textStyle = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showNomSelection = true }
                                    )
                                    DropdownMenu(
                                        expanded = showNomSelection,
                                        onDismissRequest = { showNomSelection = false },
                                        modifier = Modifier.background(panelBg).border(1.dp, gridBorderTone)
                                    ) {
                                        nominals.forEach { nom ->
                                            DropdownMenuItem(
                                                text = { Text(formatRupiah(nom), color = textMain, fontWeight = FontWeight.Bold) },
                                                onClick = {
                                                    nominalBaru = nom
                                                    showNomSelection = false
                                                }
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                var showTenSelection by remember { mutableStateOf(false) }
                                OutlinedTextField(
                                    value = "$tenorBaru Bulan",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Tenor Kontrak") },
                                    trailingIcon = {
                                        IconButton(onClick = { showTenSelection = true }) {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = textSub)
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = textMain,
                                        unfocusedTextColor = textMain,
                                        focusedBorderColor = infoBlue,
                                        unfocusedBorderColor = gridBorderTone,
                                        focusedLabelColor = infoBlue,
                                        unfocusedLabelColor = textSub
                                    ),
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showTenSelection = true }
                                )
                                DropdownMenu(
                                    expanded = showTenSelection,
                                    onDismissRequest = { showTenSelection = false },
                                    modifier = Modifier.background(panelBg).border(1.dp, gridBorderTone)
                                ) {
                                    listOf(12, 24).forEach { t ->
                                        DropdownMenuItem(
                                            text = { Text("$t Bulan", color = textMain, fontWeight = FontWeight.Bold) },
                                            onClick = {
                                                tenorBaru = t
                                                showTenSelection = false
                                            }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        if (namaBaru.trim().isEmpty()) {
                                            feedbackMessage = "Isi nama terlebih dahulu!"
                                        } else {
                                            val angsuranVal = hitungAngsuran(nominalBaru)
                                            val totalTagihanVal = angsuranVal * tenorBaru
                                            val newBorrower = Peminjam(
                                                nama = namaBaru,
                                                nominal = nominalBaru,
                                                tenor = tenorBaru,
                                                angsuranPerBulan = angsuranVal,
                                                totalTagihan = totalTagihanVal,
                                                totalSetor = 0L,
                                                sisaTagihan = totalTagihanVal,
                                                sisaTenor = tenorBaru
                                            )
                                            listNasabah = listNasabah + newBorrower
                                            feedbackMessage = "Kontrak Baru Berhasil Disimpan!"
                                            namaBaru = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = infoBlue),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("button_simpan_kontrak")
                                ) {
                                    Text("Simpan Kontrak", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }

                        // 2. Setoran Installments Column
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, borderTone, RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = panelBg),
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "2. Input Setoran",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = emeraldAccent
                                    )
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                // Select Borrower Name Dropdown
                                var showSetorPicker by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = selectedNasabahForSetoran.ifEmpty { "Pilih Nasabah" },
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Debitur") },
                                        trailingIcon = {
                                            IconButton(onClick = { showSetorPicker = true }) {
                                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = textSub)
                                            }
                                        },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = textMain,
                                            unfocusedTextColor = textMain,
                                            focusedBorderColor = emeraldAccent,
                                            unfocusedBorderColor = gridBorderTone,
                                            focusedLabelColor = emeraldAccent,
                                            unfocusedLabelColor = textSub
                                        ),
                                        textStyle = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { showSetorPicker = true }
                                    )
                                    DropdownMenu(
                                        expanded = showSetorPicker,
                                        onDismissRequest = { showSetorPicker = false },
                                        modifier = Modifier.background(panelBg).border(1.dp, gridBorderTone)
                                    ) {
                                        listNasabah.forEach { nasabah ->
                                            DropdownMenuItem(
                                                text = { Text(nasabah.nama, color = textMain, fontWeight = FontWeight.Bold) },
                                                onClick = {
                                                    selectedNasabahForSetoran = nasabah.nama
                                                    jumlahSetoran = nasabah.angsuranPerBulan.toString()
                                                    showSetorPicker = false
                                                }
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = jumlahSetoran,
                                    onValueChange = { jumlahSetoran = it },
                                    label = { Text("Jumlah Setoran (Rp)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = textMain,
                                        unfocusedTextColor = textMain,
                                        focusedBorderColor = emeraldAccent,
                                        unfocusedBorderColor = gridBorderTone,
                                        focusedLabelColor = emeraldAccent,
                                        unfocusedLabelColor = textSub
                                    ),
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_jumlah_setoran")
                                )

                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        val amount = jumlahSetoran.toLongOrNull() ?: 0L
                                        val debtorIdx = listNasabah.indexOfFirst { it.nama == selectedNasabahForSetoran }
                                        if (debtorIdx == -1) {
                                            feedbackMessage = "Pilih nasabah terlebih dahulu!"
                                        } else if (amount <= 0) {
                                            feedbackMessage = "Jumlah setoran tidak valid!"
                                        } else {
                                            val current = listNasabah[debtorIdx]
                                            val newSetorValue = current.totalSetor + amount
                                            val newSisaTagihan = current.totalTagihan - newSetorValue
                                            var newSisaTenor = current.sisaTenor
                                            if (amount >= current.angsuranPerBulan && newSisaTenor > 0) {
                                                newSisaTenor -= 1
                                            }
                                            val updatedList = listNasabah.toMutableList()
                                            updatedList[debtorIdx] = current.copy(
                                                totalSetor = newSetorValue,
                                                sisaTagihan = newSisaTagihan,
                                                sisaTenor = newSisaTenor
                                            )
                                            listNasabah = updatedList
                                            feedbackMessage = "Setoran Berhasil Disimpan!"
                                            jumlahSetoran = ""
                                            selectedNasabahForSetoran = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = emeraldAccent),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("button_simpan_setoran")
                                ) {
                                    Text("Simpan Setor", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // Database Table List
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 0.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "DATABASE NASABAH AKTIF",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = textSub,
                                        letterSpacing = 1.sp
                                    )
                                )
                                Box(
                                    modifier = Modifier
                                        .background(infoBlue.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${listNasabah.size} Aktif",
                                        color = infoBlue,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                TextButton(
                                    onClick = {
                                        if (!isSyncing) {
                                            isSyncing = true
                                            coroutineScope.launch {
                                                try {
                                                    val data = fetchGoogleSheetNasabah()
                                                    if (data.isNotEmpty()) {
                                                        listNasabah = data
                                                        feedbackMessage = "Sinkronisasi Google Sheet Berhasil! ${data.size} data termuat."
                                                    } else {
                                                        feedbackMessage = "Data Google Sheet kosong / format tidak cocok!"
                                                    }
                                                } catch (e: Exception) {
                                                    feedbackMessage = "Gagal Sinkron: ${e.localizedMessage ?: "Kesalahan Jaringan"}"
                                                    e.printStackTrace()
                                                } finally {
                                                    isSyncing = false
                                                }
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.textButtonColors(
                                        containerColor = infoBlue.copy(alpha = 0.12f),
                                        contentColor = infoBlue
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .testTag("sync_sheet_button")
                                        .height(28.dp)
                                ) {
                                    if (isSyncing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(12.dp),
                                            color = infoBlue,
                                            strokeWidth = 1.5.dp
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "MEMUAT...",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Sinkron Sheet",
                                            modifier = Modifier.size(13.dp),
                                            tint = infoBlue
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "SINKRON",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }

                                TextButton(
                                    onClick = {
                                        val ok = exportNasabahToExcel(context, listNasabah)
                                        feedbackMessage = if (ok) {
                                            "Berhasil diekspor ke folder Downloads!"
                                        } else {
                                            "Gagal mengekspor data!"
                                        }
                                    },
                                    colors = ButtonDefaults.textButtonColors(
                                        containerColor = emeraldAccent.copy(alpha = 0.12f),
                                        contentColor = emeraldAccent
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .testTag("export_excel_button")
                                        .height(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "Ekspor Excel",
                                        modifier = Modifier.size(13.dp),
                                        tint = emeraldAccent
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "EXCEL",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }

                items(listNasabah) { n ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = panelBg),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, borderTone, RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = n.nama,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = textMain
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Plafon: ${formatRupiah(n.nominal)}  •  ${n.tenor} bln  •  Sisa: ${n.sisaTenor} bln",
                                        color = textSub,
                                        fontSize = 11.sp
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = formatRupiah(n.sisaTagihan),
                                        fontWeight = FontWeight.Black,
                                        color = dangerRose,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Sisa Tagihan",
                                        color = textSub,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = borderTone, thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Angsuran: ${formatRupiah(n.angsuranPerBulan)}/bln",
                                    color = emeraldAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = { selectedReceiptPeminjam = n },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("receipt_${n.nama.replace(" ", "_")}_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Cetak Struk",
                                            tint = emeraldAccent,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { selectedDeletePeminjam = n },
                                        modifier = Modifier
                                            .size(32.dp)
                                            .testTag("delete_${n.nama.replace(" ", "_")}_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Hapus",
                                            tint = dangerRose,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Quick pop-up action confirmation toast
            AnimatedVisibility(
                visible = feedbackMessage.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Color(0xFF0F172A) else Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.border(2.dp, emeraldAccent, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = emeraldAccent)
                        Text(
                            text = feedbackMessage,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    }
                }
                LaunchedEffect(feedbackMessage) {
                    kotlinx.coroutines.delay(2500)
                    feedbackMessage = ""
                }
            }
        }
    }

    // Calculator Dialog popup
    if (showCalcDialog) {
        Dialog(onDismissRequest = { showCalcDialog = false }) {
            var calcNominal by remember { mutableStateOf(1000000L) }
            var calcTenor by remember { mutableStateOf(12) }

            val angsuranBulan = hitungAngsuran(calcNominal)
            val totalTagihanCalc = angsuranBulan * calcTenor

            Card(
                colors = CardDefaults.cardColors(containerColor = panelBg),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, gridBorderTone, RoundedCornerShape(20.dp))
                    .padding(2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Kalkulator Kredit RzKredit",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                color = textMain
                            )
                        )
                        IconButton(onClick = { showCalcDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = textSub)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Nominal chooser
                    Text(
                        text = "PILIH NOMINAL PINJAMAN",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = textSub),
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val options = listOf(1000000L, 1500000L, 2000000L, 2500000L, 3000000L, 3500000L, 4000000L, 4500000L, 5000000L)
                    var expandedNomChooser by remember { mutableStateOf(false) }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = formatRupiah(calcNominal),
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { expandedNomChooser = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = textSub)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = textMain,
                                unfocusedTextColor = textMain,
                                focusedBorderColor = infoBlue,
                                unfocusedBorderColor = gridBorderTone
                            ),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedNomChooser = true }
                        )
                        DropdownMenu(
                            expanded = expandedNomChooser,
                            onDismissRequest = { expandedNomChooser = false },
                            modifier = Modifier.background(panelBg).border(1.dp, gridBorderTone)
                        ) {
                            options.forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(formatRupiah(opt), color = textMain, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        calcNominal = opt
                                        expandedNomChooser = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Tenor segments
                    Text(
                        text = "TENOR KONTRAK",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = textSub),
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { calcTenor = 12 },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (calcTenor == 12) infoBlue else borderTone
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "12 Bulan",
                                color = if (calcTenor == 12) Color.Black else textMain,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = { calcTenor = 24 },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (calcTenor == 24) infoBlue else borderTone
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "24 Bulan",
                                color = if (calcTenor == 24) Color.Black else textMain,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Results view screen
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDarkMode) Color(0xFF03050A) else obsidianDark)
                            .border(1.5.dp, emeraldAccent, RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "ANGSURAN / BULAN",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = emeraldAccent
                                )
                                Text(
                                    text = formatRupiah(angsuranBulan),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = emeraldAccent,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            }
                            HorizontalDivider(color = borderTone)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "ESTIMASI TOTAL TAGIHAN",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = textSub
                                )
                                Text(
                                    text = formatRupiah(totalTagihanCalc),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = textMain,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    Button(
                        onClick = { showCalcDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = warningGold),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Tutup Simulasi", color = Color.Black, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    selectedDeletePeminjam?.let { borrowerToDelete ->
        var deletePasswordInput by remember(borrowerToDelete) { mutableStateOf("") }
        var deletePasswordVisible by remember(borrowerToDelete) { mutableStateOf(false) }
        var deletePasswordErrorMsg by remember(borrowerToDelete) { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { selectedDeletePeminjam = null },
            title = {
                Text(
                    text = "Konfirmasi Hapus Data",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = textMain
                    )
                )
            },
            text = {
                Column {
                    Text(
                        text = "Apakah Anda yakin ingin menghapus '${borrowerToDelete.nama}' dari database aktif RzKredit secara permanen?",
                        color = textSub,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Masukkan sandi otorisasi untuk menghapus:",
                        color = textMain,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = deletePasswordInput,
                        onValueChange = {
                            deletePasswordInput = it
                            deletePasswordErrorMsg = ""
                        },
                        label = { Text("Sandi") },
                        placeholder = { Text("Masukkan Sandi") },
                        visualTransformation = if (deletePasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textMain,
                            unfocusedTextColor = textMain,
                            focusedBorderColor = dangerRose,
                            unfocusedBorderColor = gridBorderTone,
                            focusedLabelColor = dangerRose,
                            unfocusedLabelColor = textSub
                        ),
                        singleLine = true,
                        trailingIcon = {
                            TextButton(onClick = { deletePasswordVisible = !deletePasswordVisible }) {
                                Text(
                                    text = if (deletePasswordVisible) "SEMBUNYI" else "LIHAT",
                                    color = dangerRose,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("delete_password_input")
                    )
                    if (deletePasswordErrorMsg.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = deletePasswordErrorMsg,
                            color = dangerRose,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.testTag("delete_password_error")
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (deletePasswordInput == "rzkarim123") {
                            listNasabah = listNasabah.filterNot { it.nama == borrowerToDelete.nama }
                            feedbackMessage = "Nasabah '${borrowerToDelete.nama}' berhasil dihapus!"
                            selectedDeletePeminjam = null
                        } else {
                            deletePasswordErrorMsg = "Sandi otorisasi salah!"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = dangerRose),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Ya, Hapus", color = textMain, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedDeletePeminjam = null }) {
                    Text("Batal", color = textSub)
                }
            },
            containerColor = panelBg,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.border(1.dp, borderTone, RoundedCornerShape(16.dp))
        )
    }

    // Receipt Visualizer & Downloader Dialog
    selectedReceiptPeminjam?.let { receiptBorrower ->
        Dialog(onDismissRequest = { selectedReceiptPeminjam = null }) {
            val context = LocalContext.current
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)), // polished light slate color for previewing receipt
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, emeraldAccent, RoundedCornerShape(20.dp))
                    .padding(4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PREVIEW STRUK RESMI",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF0F172A)
                            )
                        )
                        IconButton(onClick = { selectedReceiptPeminjam = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color(0xFF475569))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Simulated Printed Paper Card with high aesthetic POS styling
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "RZKREDIT PREMIUM",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = Color.Black
                            )
                            Text(
                                text = "Ultimate Ledger System",
                                fontSize = 11.sp,
                                color = Color(0xFF475569)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "TGL CETAK: 2026-06-23 06:47",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B),
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                            
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(10.dp))

                            // Details block
                            val fields = listOf(
                                "Nama Debitur" to receiptBorrower.nama,
                                "Plafon Pokok" to formatRupiah(receiptBorrower.nominal),
                                "Tenor Kredit" to "${receiptBorrower.tenor} Bulan",
                                "Sisa Tenor" to "${receiptBorrower.sisaTenor} Bulan",
                                "Angsuran/Bulan" to formatRupiah(receiptBorrower.angsuranPerBulan),
                                "Total Terbayar" to formatRupiah(receiptBorrower.totalSetor),
                                "Sisa Tagihan" to formatRupiah(receiptBorrower.sisaTagihan)
                            )

                            fields.forEach { (label, value) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        color = Color(0xFF334155),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = value,
                                        fontSize = 11.sp,
                                        color = Color.Black,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "BUKTI PEMBAYARAN ELEKTRONIK SAH",
                                fontSize = 9.sp,
                                color = Color(0xFF64748B),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Sponsor Resmi RzKarim",
                                fontSize = 9.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            val bitmap = generateReceiptBitmap(receiptBorrower)
                            val filename = "RzKredit_Struk_${receiptBorrower.nama.replace(" ", "_")}_" + System.currentTimeMillis()
                            val isSaved = saveBitmapToGallery(context, bitmap, filename)
                            if (isSaved) {
                                feedbackMessage = "Struk JPG Berhasil di-download ke Downloads!"
                            } else {
                                feedbackMessage = "Gagal memproses struk JPG!"
                            }
                            selectedReceiptPeminjam = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("download_jpg_button")
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = emeraldAccent)
                            Text("DOWNLOAD STRUK (JPG)", color = textMain, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

// Helper to generate elegant high-res white-paper Receipt Bitmap
fun generateReceiptBitmap(peminjam: Peminjam): Bitmap {
    val width = 500
    val height = 660
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    val bgPaint = Paint().apply {
        color = 0xFFFFFFFF.toInt()
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
    
    val borderPaint = Paint().apply {
        color = 0xFFE2E8F0.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    canvas.drawRect(3f, 3f, (width - 3).toFloat(), (height - 3).toFloat(), borderPaint)

    val titlePaint = Paint().apply {
        color = 0xFF0F172A.toInt()
        textSize = 24f
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }
    
    val subtitlePaint = Paint().apply {
        color = 0xFF475569.toInt()
        textSize = 14f
        textAlign = Paint.Align.CENTER
    }
    
    val labelPaint = Paint().apply {
        color = 0xFF334155.toInt()
        textSize = 15f
        isFakeBoldText = true
    }
    
    val valuePaint = Paint().apply {
        color = 0xFF000000.toInt()
        textSize = 15f
        textAlign = Paint.Align.RIGHT
        isFakeBoldText = true
    }
    
    val linePaint = Paint().apply {
        color = 0xFFCBD5E1.toInt()
        strokeWidth = 2f
    }

    var y = 60f
    
    canvas.drawText("RZKREDIT PREMIUM", (width / 2).toFloat(), y, titlePaint)
    y += 26f
    canvas.drawText("Ultimate Ledger System", (width / 2).toFloat(), y, subtitlePaint)
    y += 24f
    canvas.drawText("TGL CETAK: 2026-06-23 06:47", (width / 2).toFloat(), y, subtitlePaint)
    y += 24f
    
    canvas.drawLine(20f, y, (width - 20).toFloat(), y, linePaint)
    y += 35f
    
    val details = listOf(
        "Nama Debitur" to peminjam.nama,
        "Plafon Kredit" to formatRupiah(peminjam.nominal),
        "Tenor Kredit" to "${peminjam.tenor} Bulan",
        "Sisa Tenor" to "${peminjam.sisaTenor} Bulan",
        "Angsuran/Bulan" to formatRupiah(peminjam.angsuranPerBulan),
        "Total Terbayar" to formatRupiah(peminjam.totalSetor),
        "Sisa Tagihan" to formatRupiah(peminjam.sisaTagihan)
    )
    
    for ((lbl, value) in details) {
        canvas.drawText(lbl, 30f, y, labelPaint)
        canvas.drawText(value, (width - 30).toFloat(), y, valuePaint)
        y += 42f
    }
    
    y += 10f
    canvas.drawLine(20f, y, (width - 20).toFloat(), y, linePaint)
    y += 40f
    
    val footerPaint1 = Paint().apply {
        color = 0xFF0F172A.toInt()
        textSize = 14f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    val footerPaint2 = Paint().apply {
        color = 0xFF64748B.toInt()
        textSize = 12f
        textAlign = Paint.Align.CENTER
    }
    canvas.drawText("BUKTI PEMBAYARAN ELEKTRONIK SAH", (width / 2).toFloat(), y, footerPaint1)
    y += 26f
    canvas.drawText("Sponsor Resmi RzKarim", (width / 2).toFloat(), y, footerPaint2)
    y += 20f
    canvas.drawText("Sistem Keuangan RzKredit Premium", (width / 2).toFloat(), y, footerPaint2)
    
    return bitmap
}

// MediaStore helper to save Bitmap safely into device public Downloads directory
fun saveBitmapToGallery(context: android.content.Context, bitmap: Bitmap, filename: String): Boolean {
    val resolver = context.contentResolver
    val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Downloads.EXTERNAL_CONTENT_URI
    } else {
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }

    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, "$filename.jpg")
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
    }

    var stream: OutputStream? = null
    var uri: android.net.Uri? = null

    return try {
        uri = resolver.insert(imageCollection, contentValues)
        if (uri != null) {
            stream = resolver.openOutputStream(uri)
            if (stream != null) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)
                true
            } else {
                false
            }
        } else {
            false
        }
    } catch (e: Exception) {
        e.printStackTrace()
        false
    } finally {
        stream?.close()
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    color: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val panelBg = Color(0xFF0D1322)
    val textSub = Color(0xFF94A3B8)

    Card(
        colors = CardDefaults.cardColors(containerColor = panelBg),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = textSub,
                        letterSpacing = 0.5.sp
                    )
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(color.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                        .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                fontWeight = FontWeight.Black,
                fontSize = 16.sp,
                color = color,
                maxLines = 1
            )
        }
    }
}

// Helper to export active customer data to Excel-compatible CSV format and trigger share Sheet
fun exportNasabahToExcel(context: android.content.Context, listNasabah: List<Peminjam>): Boolean {
    val bom = "\uFEFF"
    val header = "No;Nama Nasabah;Pinjaman Pokok;Tenor;Kewajiban Angsuran/Bulan;Total Tagihan;Total Setor;Sisa Tagihan (Piutang);Sisa Tenor (Angsuran)"
    val rows = listNasabah.mapIndexed { index, nasabah ->
        "${index + 1};${nasabah.nama};${nasabah.nominal};${nasabah.tenor};${nasabah.angsuranPerBulan};${nasabah.totalTagihan};${nasabah.totalSetor};${nasabah.sisaTagihan};${nasabah.sisaTenor}"
    }
    val csvContent = bom + (listOf(header) + rows).joinToString("\n")

    val filename = "RzKredit_Data_Nasabah_Aktif_${System.currentTimeMillis()}"
    
    // 1. Try writing to Downloads via MediaStore (to let user keep a copy in Downloads folder)
    val resolver = context.contentResolver
    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Downloads.EXTERNAL_CONTENT_URI
    } else {
        android.net.Uri.parse("content://media/external/file")
    }

    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, "$filename.csv")
        put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
    }

    var savedSuccessfully = false
    try {
        val uri = resolver.insert(collection, contentValues)
        if (uri != null) {
            resolver.openOutputStream(uri)?.use { stream ->
                stream.write(csvContent.toByteArray(Charsets.UTF_8))
                savedSuccessfully = true
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    // Fallback if MediaStore fails below Android Q
    if (!savedSuccessfully && Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            val file = java.io.File(downloadsDir, "$filename.csv")
            file.writeText(csvContent, Charsets.UTF_8)
            savedSuccessfully = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 2. Share via Intent using Cache File to be absolutely certain they can send it to Sheets/WhatsApp
    try {
        val cacheFile = java.io.File(context.cacheDir, "Data_Nasabah_Aktif.csv")
        cacheFile.writeText(csvContent, Charsets.UTF_8)
        
        val contentUri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "com.example.fileprovider",
            cacheFile
        )
        
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(android.content.Intent.EXTRA_STREAM, contentUri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Ekspor Data Nasabah Aktif RzKredit")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(android.content.Intent.createChooser(shareIntent, "Bagikan / Ekspor ke Excel"))
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return savedSuccessfully
}

// Function to fetch active customer data directly from public Google Sheets link
suspend fun fetchGoogleSheetNasabah(): List<Peminjam> = withContext(Dispatchers.IO) {
    val urlConnection = URL("https://docs.google.com/spreadsheets/d/1fEwG17Ii7GLqDIvY2X5D8TQ5p5wq8-0qHJrWX6vnVVU/export?format=csv").openConnection() as HttpURLConnection
    urlConnection.requestMethod = "GET"
    urlConnection.connectTimeout = 8000
    urlConnection.readTimeout = 8000
    urlConnection.doInput = true
    
    val result = mutableListOf<Peminjam>()
    urlConnection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
        val lines = reader.readLines()
        if (lines.isNotEmpty()) {
            // First row is the header, parse rest
            for (i in 1 until lines.size) {
                val line = lines[i].trim()
                if (line.isEmpty()) continue
                
                val parts = parseCsvLine(line)
                if (parts.size >= 8) {
                    val nama = parts[0]
                    val nominal = parts[1].toLongOrNull() ?: 0L
                    val tenor = parts[2].toIntOrNull() ?: 0
                    val angsuranPerBulan = parts[3].toLongOrNull() ?: 0L
                    val totalTagihan = parts[4].toLongOrNull() ?: 0L
                    val totalSetor = parts[5].toLongOrNull() ?: 0L
                    val sisaTagihan = parts[6].toLongOrNull() ?: 0L
                    val sisaTenor = parts[7].toIntOrNull() ?: 0
                    
                    result.add(
                        Peminjam(
                            nama = nama,
                            nominal = nominal,
                            tenor = tenor,
                            angsuranPerBulan = angsuranPerBulan,
                            totalTagihan = totalTagihan,
                            totalSetor = totalSetor,
                            sisaTagihan = sisaTagihan,
                            sisaTenor = sisaTenor
                        )
                    )
                }
            }
        }
    }
    result
}

// Custom CSV Parser to handle possible quotes in full names
fun parseCsvLine(line: String): List<String> {
    val result = mutableListOf<String>()
    var current = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < line.length) {
        val c = line[i]
        if (c == '\"') {
            inQuotes = !inQuotes
        } else if (c == ',' && !inQuotes) {
            result.add(current.toString().trim())
            current = StringBuilder()
        } else {
            current.append(c)
        }
        i++
    }
    result.add(current.toString().trim())
    
    return result.map { 
        var s = it
        if (s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length - 1)
        }
        s.replace("\"\"", "\"")
    }
}


