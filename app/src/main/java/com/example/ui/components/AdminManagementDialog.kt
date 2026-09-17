package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storefront
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.TournamentEntity
import com.example.data.local.UserEntity
import com.example.data.local.MatchEntity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.filled.Upload
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberCardElevated
import com.example.ui.theme.CyberCardSurface
import com.example.ui.theme.DigiCyan
import com.example.ui.theme.DigiGold
import com.example.ui.theme.TextMuted

@Composable
fun AdminManagementDialog(
    users: List<UserEntity>,
    tournaments: List<TournamentEntity>,
    onDismiss: () -> Unit,
    onCreateOrganizer: (username: String, pass: String, fullName: String, affiliation: String) -> Unit,
    onUpdateUser: (UserEntity) -> Unit,
    onDeleteUser: (Long) -> Unit,
    onDeleteTournament: (Long) -> Unit,
    onExportTournament: (TournamentEntity) -> Unit,
    onImportTournament: ((TournamentEntity, List<MatchEntity>) -> Unit)? = null
) {
    val context = LocalContext.current
    var activeTab by remember { mutableStateOf("USERS") } // "USERS" or "TOURNAMENTS"
    var showCreateOrganizerDialog by remember { mutableStateOf(false) }
    var editingUser by remember { mutableStateOf<UserEntity?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
                .testTag("admin_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CyberCardSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DigiGold.copy(alpha = 0.6f))
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AdminPanelSettings,
                                contentDescription = null,
                                tint = DigiGold,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Panel Master Admin",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Hak Akses Tertinggi: Kelola Akun & Turnamen",
                                    color = DigiGold,
                                    fontSize = 10.sp
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.Gray)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Tab Switcher: Kelola Akun vs Kelola Turnamen
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberCardElevated)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (activeTab == "USERS") DigiGold else Color.Transparent)
                                .clickable { activeTab = "USERS" }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Kelola Akun (${users.size})",
                                fontSize = 11.sp,
                                fontWeight = if (activeTab == "USERS") FontWeight.Bold else FontWeight.Normal,
                                color = if (activeTab == "USERS") Color(0xFF0F172A) else Color.White
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (activeTab == "TOURNAMENTS") DigiGold else Color.Transparent)
                                .clickable { activeTab = "TOURNAMENTS" }
                                .padding(vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Histori Turnamen (${tournaments.size})",
                                fontSize = 11.sp,
                                fontWeight = if (activeTab == "TOURNAMENTS") FontWeight.Bold else FontWeight.Normal,
                                color = if (activeTab == "TOURNAMENTS") Color(0xFF0F172A) else Color.White
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                if (activeTab == "USERS") {
                    // Button to create Organizer (Only Admin can create)
                    item {
                        Button(
                            onClick = { showCreateOrganizerDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_add_organizer_by_admin"),
                            colors = ButtonDefaults.buttonColors(containerColor = DigiCyan)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Buat Akun Penyelenggara Baru",
                                color = Color(0xFF0F172A),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // List of Users
                    items(users) { user ->
                        val roleColor = when (user.role.uppercase()) {
                            "ADMIN" -> DigiGold
                            "ORGANIZER" -> DigiCyan
                            else -> Color(0xFFA855F7)
                        }
                        val roleIcon = when (user.role.uppercase()) {
                            "ADMIN" -> Icons.Default.AdminPanelSettings
                            "ORGANIZER" -> Icons.Default.Storefront
                            else -> Icons.Default.Person
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyberCardElevated)
                                .border(1.dp, CyberCardBorder, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(roleIcon, contentDescription = null, tint = roleColor, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = user.fullName,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(roleColor.copy(alpha = 0.2f))
                                                    .border(0.5.dp, roleColor, RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = user.role,
                                                    fontSize = 8.sp,
                                                    color = roleColor,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Text(
                                            text = "Username: ${user.username} • Sandi: ${user.password}",
                                            color = TextMuted,
                                            fontSize = 11.sp
                                        )
                                        if (user.affiliation.isNotBlank()) {
                                            Text(
                                                text = "Afiliasi/Toko: ${user.affiliation}",
                                                color = DigiCyan,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }

                                Row {
                                    IconButton(
                                        onClick = { editingUser = user },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = DigiCyan, modifier = Modifier.size(16.dp))
                                    }
                                    if (user.username != "masteradmin") {
                                        IconButton(
                                            onClick = { onDeleteUser(user.id) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // === TOURNAMENTS & HISTORI TAB ===
                    item {
                        val importTourneyLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.OpenDocument(),
                            onResult = { uri ->
                                uri?.let {
                                    val imported = com.example.util.CsvHelper.importTournamentFromCsv(context, it)
                                    if (imported != null) {
                                        onImportTournament?.invoke(imported.first, imported.second)
                                    }
                                }
                            }
                        )
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.End) {
                            Button(
                                onClick = { importTourneyLauncher.launch(arrayOf("text/*", "text/csv", "application/csv")) },
                                colors = ButtonDefaults.buttonColors(containerColor = DigiCyan),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF0F172A))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Import Turnamen (CSV)", color = Color(0xFF0F172A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    items(tournaments) { tourney ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyberCardElevated)
                                .border(1.dp, CyberCardBorder, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = DigiGold, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = tourney.name,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "${tourney.formatDisplayName} • Ronde ${tourney.currentRound}/${tourney.totalRounds} • ${tourney.status}",
                                            color = DigiCyan,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = "Penyelenggara: ${tourney.organizerName} (${tourney.location})",
                                            color = TextMuted,
                                            fontSize = 10.sp
                                        )
                                    }
                                }

                            Row {
                                IconButton(
                                    onClick = { onExportTournament(tourney) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Export CSV", tint = DigiCyan, modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = { onDeleteTournament(tourney.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                }
                            }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Create Organizer Dialog
    if (showCreateOrganizerDialog) {
        var orgUser by remember { mutableStateOf("") }
        var orgPass by remember { mutableStateOf("") }
        var orgName by remember { mutableStateOf("") }
        var orgAffiliation by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showCreateOrganizerDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CyberCardSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DigiCyan)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Buat Akun Penyelenggara Baru",
                        color = DigiCyan,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Akun ini dibuat oleh Admin untuk pihak Toko/EO.",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = orgUser,
                        onValueChange = { orgUser = it },
                        label = { Text("Username") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = orgPass,
                        onValueChange = { orgPass = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = orgName,
                        onValueChange = { orgName = it },
                        label = { Text("Nama Penanggung Jawab") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = orgAffiliation,
                        onValueChange = { orgAffiliation = it },
                        label = { Text("Nama Toko / EO (contoh: Toko C - DigiCenter)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showCreateOrganizerDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Batal")
                        }
                        Button(
                            onClick = {
                                if (orgUser.isNotBlank() && orgPass.isNotBlank() && orgName.isNotBlank()) {
                                    onCreateOrganizer(
                                        orgUser,
                                        orgPass,
                                        orgName,
                                        orgAffiliation.ifBlank { orgName }
                                    )
                                    showCreateOrganizerDialog = false
                                }
                            },
                            modifier = Modifier.weight(1.5f),
                            colors = ButtonDefaults.buttonColors(containerColor = DigiCyan)
                        ) {
                            Text("Simpan Akun", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Modal Edit User Dialog
    editingUser?.let { targetUser ->
        var editFullName by remember { mutableStateOf(targetUser.fullName) }
        var editPass by remember { mutableStateOf(targetUser.password) }
        var editAffiliation by remember { mutableStateOf(targetUser.affiliation) }

        Dialog(onDismissRequest = { editingUser = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = CyberCardSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, DigiGold)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Edit Akun: ${targetUser.username}",
                        color = DigiGold,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = editFullName,
                        onValueChange = { editFullName = it },
                        label = { Text("Nama Lengkap") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = editPass,
                        onValueChange = { editPass = it },
                        label = { Text("Password Baru") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = editAffiliation,
                        onValueChange = { editAffiliation = it },
                        label = { Text("Afiliasi / Toko") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { editingUser = null },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Batal")
                        }
                        Button(
                            onClick = {
                                onUpdateUser(
                                    targetUser.copy(
                                        fullName = editFullName,
                                        password = editPass,
                                        affiliation = editAffiliation
                                    )
                                )
                                editingUser = null
                            },
                            modifier = Modifier.weight(1.5f),
                            colors = ButtonDefaults.buttonColors(containerColor = DigiGold)
                        ) {
                            Text("Simpan Perubahan", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
