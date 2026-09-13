package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.example.ui.components.SidebarDrawerContent
import com.example.ui.components.AdminManagementDialog
import com.example.ui.components.InAppNotificationBanner
import com.example.ui.components.GameHistoryDialog
import com.example.ui.components.LoginAndRegisterDialog
import com.example.ui.components.NewTournamentDialog
import com.example.ui.components.AddPlayerDialog
import com.example.ui.components.PlayerProfileDialog
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.PlayersScreen
import com.example.ui.screens.StandingsScreen
import com.example.ui.screens.TournamentScreen
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.CyberCardElevated
import com.example.ui.theme.CyberCardSurface
import com.example.ui.theme.CyberNavyBg
import com.example.ui.theme.DigiCyan
import com.example.ui.theme.DigiGold
import com.example.ui.theme.DigiGreen
import com.example.ui.theme.DigiSwissTheme
import com.example.ui.theme.TextMuted

import com.example.ui.viewmodel.TournamentViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DigiSwissTheme {
                DigiSwissApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DigiSwissApp(
    viewModel: TournamentViewModel = viewModel()
) {
    val context = LocalContext.current
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    var currentNavIndex by remember { mutableIntStateOf(0) }
    var showNewTournamentDialog by remember { mutableStateOf(false) }
    var showAddPlayerDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showLoginRegisterDialog by remember { mutableStateOf(false) }
    var showAdminManagementDialog by remember { mutableStateOf(false) }


    val currentUser by viewModel.currentUser.collectAsState()
    val allUsers by viewModel.allUsersFlow.collectAsState()
    val activeTournament by viewModel.activeTournamentFlow.collectAsState()
    val allTournaments by viewModel.allTournamentsFlow.collectAsState()
    val players by viewModel.allPlayersFlow.collectAsState()
    val matches by viewModel.matchesFlow.collectAsState()
    val standings by viewModel.standingsFlow.collectAsState()
    val selectedRound by viewModel.selectedRound.collectAsState()
    val timerSeconds by viewModel.timerSeconds.collectAsState()
    val isTimerRunning by viewModel.isTimerRunning.collectAsState()
    val inAppNotification by viewModel.inAppNotification.collectAsState()
    val selectedPlayer by viewModel.selectedPlayer.collectAsState()





    if (showHistoryDialog) {
        GameHistoryDialog(
            tournaments = allTournaments,
            onDismiss = { showHistoryDialog = false }
        )
    }

    if (showLoginRegisterDialog) {
        LoginAndRegisterDialog(
            onDismiss = { showLoginRegisterDialog = false },
            onLogin = { username, pass, onError ->
                viewModel.login(username, pass) { success, errorMsg ->
                    if (success) {
                        showLoginRegisterDialog = false
                    } else {
                        onError(errorMsg ?: "Gagal masuk")
                    }
                }
            }
        )
    }

    if (showAdminManagementDialog) {
        AdminManagementDialog(
            users = allUsers,
            tournaments = allTournaments,
            onDismiss = { showAdminManagementDialog = false },
            onCreateOrganizer = { u, p, fn, aff ->
                viewModel.createOrganizerAccount(u, p, fn, aff) { _, _ -> }
            },
            onUpdateUser = { user ->
                viewModel.updateUser(user)
            },
            onDeleteUser = { userId ->
                viewModel.deleteUser(userId)
            },
            onDeleteTournament = { tourneyId ->
                viewModel.adminDeleteTournament(tourneyId)
            }
        )
    }

    if (showNewTournamentDialog) {
        NewTournamentDialog(
            allPlayers = players,
            defaultOrganizer = if (currentUser?.isOrganizer == true) currentUser?.fullName ?: "DigiSwiss Organizer" else "DigiSwiss Organizer",
            onDismiss = { showNewTournamentDialog = false },
            onAddNewPlayerClick = { showAddPlayerDialog = true },
            onConfirmCreate = { name, dateText, totalRounds, durationMins, location, matchFormat, selectedPlayerIds ->
                viewModel.createNewTournament(
                    name = name,
                    dateText = dateText,
                    totalRounds = totalRounds,
                    durationMins = durationMins,
                    location = location,
                    matchFormat = matchFormat,
                    selectedPlayerIds = selectedPlayerIds
                )
                showNewTournamentDialog = false
            }
        )
    }

    if (showAddPlayerDialog) {
        AddPlayerDialog(
            onDismiss = { showAddPlayerDialog = false },
            onConfirmAdd = { name, handle, bandaiUid ->
                viewModel.addNewPlayer(name, handle, bandaiUid)
                showAddPlayerDialog = false
            }
        )
    }

    if (selectedPlayer != null) {
        PlayerProfileDialog(
            player = selectedPlayer!!,
            allPlayers = players,
            matches = matches,
            onDismiss = { viewModel.selectPlayerForDetails(null) }
        )
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            SidebarDrawerContent(
                currentUser = currentUser,
                onOpenAuth = { 
                    scope.launch { drawerState.close() }
                    showLoginRegisterDialog = true 
                },
                onLogout = { 
                    scope.launch { drawerState.close() }
                    viewModel.logout() 
                },
                onOpenAdminConfig = { 
                    scope.launch { drawerState.close() }
                    showAdminManagementDialog = true 
                },
                onOpenHistory = {
                    scope.launch { drawerState.close() }
                    showHistoryDialog = true
                }
            )
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = CyberNavyBg,
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                        }
                    },
                    title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(DigiCyan.copy(alpha = 0.18f))
                                .border(1.dp, DigiCyan, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "DG",
                                color = DigiCyan,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "DIGISWISS",
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(DigiGreen)
                                )
                            }
                            Text(
                                text = "Digimon TCG Swiss System",
                                color = DigiCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                actions = {
                    // Admin Master quick button
                    if (currentUser?.isAdmin == true) {
                        IconButton(
                            onClick = { showAdminManagementDialog = true },
                            modifier = Modifier.testTag("admin_panel_button")
                        ) {
                            Icon(
                                Icons.Default.AdminPanelSettings,
                                contentDescription = "Panel Admin",
                                tint = DigiGold
                            )
                        }
                    }

                    // Reset Tournament button (for organizer or admin)
                    if (currentUser?.isAdmin == true || currentUser?.isOrganizer == true) {
                        IconButton(
                            onClick = { viewModel.resetTournament() },
                            modifier = Modifier.testTag("reset_tournament_button")
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Reset Turnamen",
                                tint = TextMuted
                            )
                        }

                        // Create tournament button (Penyelenggara / Admin)
                        IconButton(
                            onClick = { showNewTournamentDialog = true },
                            modifier = Modifier.testTag("new_tournament_button")
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Turnamen Baru",
                                tint = DigiCyan
                            )
                        }
                    }




                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CyberCardSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = CyberCardSurface,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .border(1.dp, CyberCardBorder)
                    .testTag("bottom_nav_bar")
            ) {
                val navItems = listOf(
                    Triple("Pairing", Icons.Default.SportsEsports, "nav_pairing"),
                    Triple("Klasemen", Icons.Default.EmojiEvents, "nav_standings"),
                    Triple("Analisa", Icons.Default.BarChart, "nav_analytics"),
                    Triple("Peserta", Icons.Default.Groups, "nav_players")
                )

                navItems.forEachIndexed { index, (label, icon, testTag) ->
                    val isSelected = currentNavIndex == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentNavIndex = index },
                        icon = {
                            Icon(
                                icon,
                                contentDescription = label,
                                tint = if (isSelected) DigiCyan else TextMuted
                            )
                        },
                        label = {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) DigiCyan else TextMuted
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = DigiCyan.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag(testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Screen Content
            Crossfade(targetState = currentNavIndex, label = "screen_crossfade") { screenIndex ->
                when (screenIndex) {
                    0 -> TournamentScreen(
                        viewModel = viewModel,
                        tournament = activeTournament,
                        players = players,
                        matches = matches,
                        selectedRound = selectedRound,
                        timerSeconds = timerSeconds,
                        isTimerRunning = isTimerRunning,
                        currentUser = currentUser,
                        onOpenAuthDialog = { showLoginRegisterDialog = true },
                        onOpenAdminDialog = { showAdminManagementDialog = true },
                        onNavigateToStandings = { currentNavIndex = 1 }
                    )
                    1 -> StandingsScreen(
                        standings = standings,
                        tournamentName = activeTournament?.name ?: "Digimon Tournament",
                        currentRound = activeTournament?.currentRound ?: 1,
                        totalRounds = activeTournament?.totalRounds ?: 3,
                        matchFormat = activeTournament?.matchFormat ?: "BO3",
                        onPlayerClick = { player ->
                            viewModel.selectPlayerForDetails(player)
                        }
                    )
                    2 -> AnalyticsScreen(
                        players = players,
                        matches = matches,
                        allMatches = matches,
                        allTournaments = allTournaments,
                        activeTournament = activeTournament
                    )
                    3 -> PlayersScreen(
                        players = players,
                        currentUser = currentUser,
                        onPlayerClick = { player ->
                            viewModel.selectPlayerForDetails(player)
                        },
                        onAddNewPlayer = { name, handle, bandaiUid ->
                            viewModel.addNewPlayer(name, handle, bandaiUid)
                        }
                    )
                }
            }

            // In-App Notification Push Alert Floating Banner
            InAppNotificationBanner(
                alert = inAppNotification,
                onDismiss = { viewModel.dismissInAppNotification() },
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
    }
}
