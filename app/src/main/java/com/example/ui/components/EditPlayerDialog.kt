package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
fun EditPlayerDialog(
    player: PlayerEntity,
    onDismiss: () -> Unit,
    onConfirmSave: (name: String, handle: String, bandaiUid: String, archetype: String, color: String) -> Unit
) {
    var name by remember { mutableStateOf(player.name) }
    var handle by remember { mutableStateOf(if (player.handle == "-") "" else player.handle.removePrefix("@")) }
    var bandaiUid by remember { mutableStateOf(if (player.bandaiUid == "-") "" else player.bandaiUid) }
    var deckArchetype by remember { mutableStateOf(if (player.deckArchetype == "Unknown") "" else player.deckArchetype) }
    var selectedColor by remember { mutableStateOf(if (player.deckColor == "UNKNOWN") "RED" else player.deckColor) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("edit_player_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CyberCardSurface),
            border = BorderStroke(1.dp, CyberCardBorder)
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
                            Icons.Default.Edit,
                            contentDescription = null,
                            tint = DigiCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Edit Profil Peserta",
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
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_player_name_input"),
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
                        .testTag("edit_player_handle_input"),
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
                        .testTag("edit_player_uid_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DigiCyan,
                        unfocusedBorderColor = CyberCardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Deck Archetype
                OutlinedTextField(
                    value = deckArchetype,
                    onValueChange = { deckArchetype = it },
                    label = { Text("Archetype Deck (Opsional)", color = TextMuted) },
                    placeholder = { Text("Contoh: WarGreymon OTK", color = TextMuted.copy(alpha = 0.5f)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_player_deck_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DigiCyan,
                        unfocusedBorderColor = CyberCardBorder,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Color Selection
                Text(
                    text = "Warna Utama Deck:",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val colors = listOf(
                        "RED" to "Merah",
                        "BLUE" to "Biru",
                        "YELLOW" to "Kuning",
                        "GREEN" to "Hijau",
                        "PURPLE" to "Ungu",
                        "BLACK" to "Hitam",
                        "WHITE" to "Putih"
                    )
                    colors.forEach { (colorKey, colorLabel) ->
                        val isSel = selectedColor.contains(colorKey)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) DigiCyan.copy(alpha = 0.2f) else CyberCardElevated)
                                .border(1.dp, if (isSel) DigiCyan else CyberCardBorder, RoundedCornerShape(6.dp))
                                .clickable { selectedColor = colorKey }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = colorLabel,
                                color = if (isSel) DigiCyan else Color.LightGray,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

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
                        border = BorderStroke(1.dp, CyberCardBorder)
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
                            if (trimmedUid.isNotEmpty()) {
                                if (trimmedUid.length != 10) {
                                    errorMessage = "UID Bandai+ harus tepat 10 karakter angka (tidak boleh kurang atau lebih)."
                                    return@Button
                                }
                            }
                            val finalHandle = if (handle.isBlank()) "-" else if (handle.startsWith("@")) handle.trim() else "@${handle.trim()}"
                            val finalUid = if (trimmedUid.isBlank()) "-" else trimmedUid
                            val finalArchetype = if (deckArchetype.isBlank()) "Unknown" else deckArchetype.trim()
                            onConfirmSave(name.trim(), finalHandle, finalUid, finalArchetype, selectedColor)
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("submit_edit_player_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = DigiCyan)
                    ) {
                        Text(
                            "Simpan Perubahan",
                            color = Color(0xFF0F172A),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
