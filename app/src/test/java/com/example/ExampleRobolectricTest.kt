package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.MatchEntity
import com.example.data.local.PlayerEntity
import com.example.util.AnalyticsEngine
import com.example.util.SwissPairingEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("DigiSwiss", appName)
  }

  @Test
  fun `swiss pairing engine calculates correct standings and tiebreakers`() {
    val p1 = PlayerEntity(id = 1, name = "Player A", handle = "@a", deckArchetype = "Red Hybrid")
    val p2 = PlayerEntity(id = 2, name = "Player B", handle = "@b", deckArchetype = "Fenriloogamon")
    val p3 = PlayerEntity(id = 3, name = "Player C", handle = "@c", deckArchetype = "MirageGaogamon")

    val matches = listOf(
      MatchEntity(
        id = 1,
        tournamentId = 1,
        roundNumber = 1,
        tableNumber = 1,
        player1Id = 1,
        player2Id = 2,
        p1Score = 2,
        p2Score = 0,
        winnerId = 1,
        isReported = true,
        matchDurationMinutes = 25
      ),
      MatchEntity(
        id = 2,
        tournamentId = 1,
        roundNumber = 1,
        tableNumber = 2,
        player1Id = 3,
        player2Id = null,
        p1Score = 2,
        p2Score = 0,
        winnerId = 3,
        isReported = true,
        isBye = true,
        matchDurationMinutes = 0
      )
    )

    val standings = SwissPairingEngine.computeStandings(listOf(p1, p2, p3), matches)
    assertEquals(3, standings.size)
    assertEquals(1L, standings[0].player.id)
    assertEquals(3, standings[0].matchPoints)
    assertEquals(1, standings[0].matchesWon)
  }

  @Test
  fun `analytics engine diagnoses player weakness and deck stats`() {
    val p1 = PlayerEntity(id = 1, name = "Player A", handle = "@a", deckArchetype = "Red Hybrid", deckColor = "RED")
    val p2 = PlayerEntity(id = 2, name = "Player B", handle = "@b", deckArchetype = "Fenriloogamon", deckColor = "PURPLE")

    val matches = listOf(
      MatchEntity(
        id = 1,
        tournamentId = 1,
        roundNumber = 1,
        tableNumber = 1,
        player1Id = 1,
        player2Id = 2,
        p1Score = 2,
        p2Score = 1,
        winnerId = 1,
        isReported = true,
        matchDurationMinutes = 38,
        firstTurnPlayerId = 1
      )
    )

    val analytics = AnalyticsEngine.analyzePlayer(1L, listOf(p1, p2), matches)
    assertNotNull(analytics)
    assertEquals(100.0, analytics!!.matchWinRate, 0.01)
    assertEquals(38.0, analytics.avgDurationMinutes, 0.01)
    assertEquals(1, analytics.firstTurnMatches)
  }

  @Test
  fun `best of 1 format score calculation and standing`() {
    val p1 = PlayerEntity(id = 1, name = "Player A", handle = "@a", deckArchetype = "Red Hybrid")
    val p2 = PlayerEntity(id = 2, name = "Player B", handle = "@b", deckArchetype = "Fenriloogamon")

    val bo1Match = MatchEntity(
      id = 1,
      tournamentId = 2,
      roundNumber = 1,
      tableNumber = 1,
      player1Id = 1,
      player2Id = 2,
      p1Score = 1,
      p2Score = 0,
      winnerId = 1,
      isReported = true,
      matchDurationMinutes = 18
    )

    val standings = SwissPairingEngine.computeStandings(listOf(p1, p2), listOf(bo1Match))
    assertEquals(1L, standings[0].player.id)
    assertEquals(3, standings[0].matchPoints)
    assertEquals(1, standings[0].matchesWon)
    assertEquals(100.0, standings[0].gwPercent, 0.01)
  }

  @Test
  fun `online tournament sync payload serializes and deserializes accurately`() {
    val tournament = com.example.data.local.TournamentEntity(
      id = 10,
      name = "Bandai TCG Fest 2026",
      dateText = "2026-09-12",
      totalRounds = 4,
      currentRound = 2,
      status = "ACTIVE",
      roundDurationMinutes = 45,
      location = "Jakarta Hall",
      matchFormat = "BO3"
    )
    val p1 = PlayerEntity(id = 101, name = "Agumon Master", handle = "@agumon", deckArchetype = "WarGreymon")
    val p2 = PlayerEntity(id = 102, name = "Gabumon Tamer", handle = "@gabumon", deckArchetype = "MetalGarurumon")

    val match = MatchEntity(
      id = 501,
      tournamentId = 10,
      roundNumber = 1,
      tableNumber = 1,
      player1Id = 101,
      player2Id = 102,
      p1Score = 2,
      p2Score = 1,
      winnerId = 101,
      isReported = true
    )

    val payload = com.example.data.remote.TournamentSyncPayload(
      roomCode = "DIGI-99",
      actionType = "SCORE_UPDATE",
      senderRole = "ORGANIZER",
      senderName = "Head Judge",
      message = "Meja 1 selesai",
      tournament = tournament,
      players = listOf(p1, p2),
      matches = listOf(match)
    )

    val jsonString = payload.toJsonString()
    assertTrue(jsonString.contains("DIGI-99"))
    assertTrue(jsonString.contains("Bandai TCG Fest 2026"))

    val parsed = com.example.data.remote.TournamentSyncPayload.fromJsonString(jsonString)
    assertNotNull(parsed)
    assertEquals("DIGI-99", parsed!!.roomCode)
    assertEquals("Bandai TCG Fest 2026", parsed.tournament.name)
    assertEquals(2, parsed.players.size)
    assertEquals(1, parsed.matches.size)
    assertEquals(101L, parsed.matches[0].winnerId)
  }
}

