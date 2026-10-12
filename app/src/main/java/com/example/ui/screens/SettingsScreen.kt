package com.example.ui.screens

import android.app.TimePickerDialog
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.ThemeMode
import com.example.data.repository.UserPreferences
import com.example.receiver.DailyReminderReceiver
import com.example.util.DateUtils
import com.example.util.debouncedClickable

@Composable
fun SettingsScreen(
    userPreferences: UserPreferences,
    onThemeChanged: (ThemeMode) -> Unit,
    onBiometricToggled: (Boolean) -> Unit,
    onPinChanged: (String) -> Unit,
    onReminderChanged: (enabled: Boolean, hour: Int, minute: Int) -> Unit,
    onExportReport: (format: String) -> Unit,
    onBackupClick: () -> Unit,
    onRestoreSelected: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPinDialogVisible by remember { mutableStateOf(false) }
    var pinInputValue by remember { mutableStateOf("") }

    // File picker launcher for restore
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { onRestoreSelected(it) }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)
    ) {
        // Title
        item {
            Text(
                text = "Pengaturan & Keamanan",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Sesuaikan tampilan, proteksi data, laporan, dan jadwal pengingat",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.height(18.dp))
        }

        // Section 1: Tema & Mode Gelap
        item {
            SettingsCard(
                title = "Tema & Mode Gelap",
                icon = Icons.Default.DarkMode
            ) {
                Text(
                    text = "Pilih skema warna untuk kenyamanan visual matamu",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val modes = listOf(
                        Triple(ThemeMode.DARK, "Mode Gelap", "Hemat Baterai"),
                        Triple(ThemeMode.LIGHT, "Mode Terang", "Cerah"),
                        Triple(ThemeMode.SYSTEM, "Ikuti Sistem", "Otomatis")
                    )

                    modes.forEach { (mode, label, desc) ->
                        val isSelected = userPreferences.themeMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                    else MaterialTheme.colorScheme.surface
                                )
                                .then(
                                    if (isSelected) Modifier.border(
                                        1.5.dp,
                                        MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(12.dp)
                                    )
                                    else Modifier
                                )
                                .debouncedClickable { onThemeChanged(mode) }
                                .padding(vertical = 10.dp, horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Text(
                                    text = desc,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Section 2: Keamanan & Biometrik
        item {
            SettingsCard(
                title = "Proteksi Biometrik & PIN",
                icon = Icons.Default.Fingerprint
            ) {
                // Biometric Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Kunci Sidik Jari / Wajah",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = "Minta verifikasi biometrik saat membuka aplikasi",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    Switch(
                        checked = userPreferences.biometricEnabled,
                        onCheckedChange = onBiometricToggled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.testTag("biometric_toggle")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // PIN Setup Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Kunci Kode PIN Cadangan",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = if (userPreferences.pinCode.isNotBlank()) "PIN 4-digit aktif"
                            else "Atur PIN sebagai alternatif pembuka kunci",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            pinInputValue = userPreferences.pinCode
                            isPinDialogVisible = true
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("set_pin_button")
                    ) {
                        Text(if (userPreferences.pinCode.isNotBlank()) "Ubah PIN" else "Pasang PIN")
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Section 3: Pengingat Harian
        item {
            SettingsCard(
                title = "Pengingat Harian",
                icon = Icons.Default.Notifications
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Notifikasi Rutin Setiap Hari",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = "Mengingatkan untuk mencatat pengeluaran sebelum tidur",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    Switch(
                        checked = userPreferences.reminderEnabled,
                        onCheckedChange = { isEnabled ->
                            onReminderChanged(isEnabled, userPreferences.reminderHour, userPreferences.reminderMinute)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.testTag("reminder_toggle")
                    )
                }

                if (userPreferences.reminderEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Waktu: ${String.format("%02d:%02d", userPreferences.reminderHour, userPreferences.reminderMinute)} WIB",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        onReminderChanged(true, hourOfDay, minute)
                                    },
                                    userPreferences.reminderHour,
                                    userPreferences.reminderMinute,
                                    true
                                ).show()
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Ubah Jam")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { DailyReminderReceiver.showReminderNotification(context) }
                        ) {
                            Text("Kirim Notifikasi Uji Coba Sekarang 🔔", fontSize = 12.sp)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Section 4: Ekspor Laporan
        item {
            SettingsCard(
                title = "Ekspor Laporan Keuangan",
                icon = Icons.Default.Description
            ) {
                Text(
                    text = "Ekspor catatan transaksi bulan terpilih untuk arsip pribadi atau dibagikan",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Export CSV
                    Button(
                        onClick = { onExportReport("CSV") },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("export_csv_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Ekspor CSV",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    // Export PDF
                    Button(
                        onClick = { onExportReport("PDF") },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("export_pdf_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Ekspor PDF",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Section 5: Cadangkan & Pulihkan (Cloud & Antar Perangkat)
        item {
            SettingsCard(
                title = "Sinkronisasi & Cadangan Data",
                icon = Icons.Default.CloudUpload
            ) {
                Text(
                    text = "Cadangkan seluruh database CatatKas ke file JSON untuk dipindahkan atau disinkronkan ke Google Drive dan perangkat lain.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                if (userPreferences.lastBackupTimestamp > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Cadangan Terakhir: ${DateUtils.formatDateShort(userPreferences.lastBackupTimestamp)} • ${DateUtils.formatTime(userPreferences.lastBackupTimestamp)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onBackupClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("backup_data_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Cadangkan Data", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = { filePickerLauncher.launch("application/json") },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("restore_data_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pulihkan File", fontSize = 12.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Section 6: About & Efficiency
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "CatatKas v1.0 • Offline-First & Hemat Daya",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        text = "Data disimpan aman di perangkatmu dengan arsitektur Room Database",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }

    // Set PIN Dialog
    if (isPinDialogVisible) {
        AlertDialog(
            onDismissRequest = { isPinDialogVisible = false },
            title = {
                Text("Atur 4-Digit Kode PIN", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "Masukkan 4 angka rahasia untuk membuka aplikasi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pinInputValue,
                        onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) pinInputValue = it },
                        placeholder = { Text("1234") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("pin_code_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinInputValue.length == 4) {
                            onPinChanged(pinInputValue)
                            isPinDialogVisible = false
                        }
                    },
                    enabled = pinInputValue.length == 4
                ) {
                    Text("Simpan PIN")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (userPreferences.pinCode.isNotBlank()) {
                            onPinChanged("") // Clear PIN
                        }
                        isPinDialogVisible = false
                    }
                ) {
                    Text(if (userPreferences.pinCode.isNotBlank()) "Hapus PIN" else "Batal")
                }
            }
        )
    }
}

@Composable
fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            content()
        }
    }
}
