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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.DigimonColor
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberCardElevated
import com.example.ui.theme.CyberCardSurface
import com.example.ui.theme.DigiCyan
import com.example.ui.theme.TextMuted

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddPlayerDialog(
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
                    label = { Text("Nama Lengkap / Nickname", color = TextMuted) },
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

                // Handle input
                OutlinedTextField(
                    value = handle,
                    onValueChange = { handle = it },
                    label = { Text("Username Bandai+", color = TextMuted) },
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

                // UID input
                OutlinedTextField(
                    value = bandaiUid,
                    onValueChange = { bandaiUid = it; errorMessage = null },
                    label = { Text("UID Bandai+ (10 digit angka)", color = TextMuted) },
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
                        color = Color.Red,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

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
                            if (bandaiUid.isBlank()) {
                                errorMessage = "UID Bandai+ tidak boleh kosong."
                                return@Button
                            }
                            if (bandaiUid.length != 10 || !bandaiUid.all { it.isDigit() }) {
                                errorMessage = "UID Bandai+ harus berupa 10 digit angka."
                                return@Button
                            }
                            val finalHandle = if (handle.isBlank()) "@${name.lowercase().replace(" ", "")}" else handle
                            onConfirmAdd(name, finalHandle, bandaiUid)
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
