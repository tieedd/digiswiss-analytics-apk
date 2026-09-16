package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.PlayerEntity
import com.example.data.model.DigimonColor
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberCardElevated
import com.example.ui.theme.CyberCardSurface
import com.example.ui.theme.DigiCyan
import com.example.ui.theme.DigiGold
import com.example.ui.theme.TextMuted

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddPlayerDialog(
    existingPlayers: List<PlayerEntity> = emptyList(),
    onDismiss: () -> Unit,
    onConfirmAdd: (name: String, handle: String, bandaiUid: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var handle by remember { mutableStateOf("") }
    var bandaiUid by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("add_player_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CyberCardSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = DigiCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Pendaftaran Peserta",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Name input
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMessage = null },
                    label = { Text("Nama Lengkap / Nickname *", color = TextMuted) },
                    placeholder = { Text("Contoh: Tai Kamiya", color = TextMuted.copy(alpha = 0.5f)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("player_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DigiCyan,
                        unfocusedBorderColor = CyberCardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Handle input (Optional)
                OutlinedTextField(
                    value = handle,
                    onValueChange = { handle = it },
                    label = { Text("Username Bandai+ (Opsional)", color = TextMuted) },
                    placeholder = { Text("Contoh: @taikamiya atau kosongkan", color = TextMuted.copy(alpha = 0.5f)) },
                    supportingText = {
                        Text(
                            text = if (handle.isBlank()) "Opsional (tersimpan sebagai '-')" else "Username Bandai+",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("player_handle_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DigiCyan,
                        unfocusedBorderColor = CyberCardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // UID input (Optional, exactly 10 digits, allows starting with 0)
                OutlinedTextField(
                    value = bandaiUid,
                    onValueChange = { input ->
                        // Only allow digits and maximum 10 digits
                        if (input.length <= 10 && input.all { it.isDigit() }) {
                            bandaiUid = input
                            errorMessage = null
                        }
                    },
                    label = { Text("UID Bandai+ (Opsional - Tepat 10 Digit)", color = TextMuted) },
                    placeholder = { Text("Contoh: 0698987569 atau kosongkan", color = TextMuted.copy(alpha = 0.5f)) },
                    supportingText = {
                        Text(
                            text = if (bandaiUid.isBlank()) {
                                "Opsional (tersimpan sebagai '-')"
                            } else {
                                "${bandaiUid.length}/10 digit ${if (bandaiUid.length == 10) "✓ (Tepat)" else "(Wajib tepat 10 digit)"}"
                            },
                            color = if (bandaiUid.isNotBlank() && bandaiUid.length < 10) DigiGold else if (bandaiUid.length == 10) DigiCyan else TextMuted,
                            fontSize = 11.sp
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("player_uid_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DigiCyan,
                        unfocusedBorderColor = CyberCardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = Color(0xFFF87171),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCardBorder)
                    ) {
                        Text("Batal", color = Color.Gray)
                    }

                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                errorMessage = "Nama pemain tidak boleh kosong."
                                return@Button
                            }
                            val trimmedUid = bandaiUid.trim()
                            if (trimmedUid.isNotEmpty() && trimmedUid != "-") {
                                if (trimmedUid.length != 10) {
                                    errorMessage = "UID Bandai+ harus tepat 10 karakter angka (tidak boleh kurang atau lebih)."
                                    return@Button
                                }
                            }
                            val rawHandle = handle.trim()
                            val finalHandle = if (rawHandle.isBlank() || rawHandle == "-") "-" else if (rawHandle.startsWith("@")) rawHandle else "@$rawHandle"
                            val finalUid = if (trimmedUid.isBlank() || trimmedUid == "-") "-" else trimmedUid

                            // Duplicate validation (exempting "-" or blank)
                            if (finalHandle != "-") {
                                val cleanCheckHandle = finalHandle.removePrefix("@")
                                val duplicateUsername = existingPlayers.any { p ->
                                    val existingClean = p.handle.trim().removePrefix("@")
                                    existingClean != "-" && existingClean.equals(cleanCheckHandle, ignoreCase = true)
                                }
                                if (duplicateUsername) {
                                    errorMessage = "Username Bandai+ '$finalHandle' sudah digunakan pemain lain. Gunakan username berbeda."
                                    return@Button
                                }
                            }

                            if (finalUid != "-") {
                                val duplicateUid = existingPlayers.any { p ->
                                    val existingCleanUid = p.bandaiUid.trim()
                                    existingCleanUid != "-" && existingCleanUid.equals(finalUid, ignoreCase = true)
                                }
                                if (duplicateUid) {
                                    errorMessage = "UID Bandai+ '$finalUid' sudah terdaftar pada pemain lain. Periksa kembali."
                                    return@Button
                                }
                            }

                            onConfirmAdd(name.trim(), finalHandle, finalUid)
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("submit_add_player_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = DigiCyan)
                    ) {
                        Text(
                            "Daftarkan Peserta",
                            color = Color(0xFF0F172A),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
