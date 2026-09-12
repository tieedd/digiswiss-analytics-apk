package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.UserEntity
import com.example.ui.theme.CyberCardElevated
import com.example.ui.theme.CyberNavyBg
import com.example.ui.theme.DigiCyan
import com.example.ui.theme.DigiGold

@Composable
fun SidebarDrawerContent(
    currentUser: UserEntity?,
    onOpenAuth: () -> Unit,
    onLogout: () -> Unit,
    onOpenAdminConfig: () -> Unit,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(
        drawerContainerColor = CyberNavyBg,
        modifier = modifier.width(280.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxHeight()) {
            Text(
                text = "DigiSwiss & Analytics",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp, top = 16.dp)
            )

            if (currentUser == null) {
                NavigationDrawerItem(
                    label = { Text("Masuk / Daftar") },
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    selected = false,
                    onClick = onOpenAuth,
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent, unselectedTextColor = Color.White, unselectedIconColor = DigiCyan)
                )
            } else {
                Box(modifier = Modifier.fillMaxWidth().background(CyberCardElevated, shape = MaterialTheme.shapes.medium).padding(16.dp)) {
                    Column {
                        Text("Informasi Akun", color = DigiCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(currentUser.fullName, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(currentUser.role, color = Color.Gray, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                NavigationDrawerItem(
                    label = { Text("Konfigurasi Akun") },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    selected = false,
                    onClick = { /* TODO */ },
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent, unselectedTextColor = Color.White, unselectedIconColor = DigiCyan)
                )

                NavigationDrawerItem(
                    label = { Text("Histori Permainan") },
                    icon = { Icon(Icons.Default.History, contentDescription = null) },
                    selected = false,
                    onClick = onOpenHistory,
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent, unselectedTextColor = Color.White, unselectedIconColor = DigiCyan)
                )

                if (currentUser.isAdmin || currentUser.isOrganizer) {
                    NavigationDrawerItem(
                        label = { Text("Pengaturan Penyelenggara") },
                        icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null) },
                        selected = false,
                        onClick = onOpenAdminConfig,
                        colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent, unselectedTextColor = Color.White, unselectedIconColor = DigiGold)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                NavigationDrawerItem(
                    label = { Text("Logout") },
                    icon = { Icon(Icons.Default.Logout, contentDescription = null) },
                    selected = false,
                    onClick = onLogout,
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent, unselectedTextColor = Color(0xFFEF4444), unselectedIconColor = Color(0xFFEF4444))
                )
            }
        }
    }
}
