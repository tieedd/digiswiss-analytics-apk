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
    assertEquals("DigiSwiss & Analytics", appName)
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
  fun `swiss pairing round 1 creates valid pairings for all players`() {
    val players = (1..6).map { id ->
      PlayerEntity(id = id.toLong(), name = "Player $id", handle = "@p$id", deckArchetype = "Archetype $id")
    }

    val matches = com.example.util.SwissPairingEngine.generateNextRoundPairings(
      tournamentId = 10,
      roundNumber = 1,
      players = players,
      pastMatches = emptyList()
    ).matches

    assertEquals(3, matches.size)
    val pairedIds = matches.flatMap { listOfNotNull(it.player1Id, it.player2Id) }
    assertEquals(6, pairedIds.distinct().size)
  }

  @Test
  fun `verify match reporting and advance round`() = kotlinx.coroutines.test.runTest {
    val context = ApplicationProvider.getApplicationContext<android.app.Application>()
    val repository = com.example.data.repository.TournamentRepository(context)
    val db = com.example.data.local.DigiSwissDatabase.getDatabase(context)

    val playerIds = (1..16).map { i ->
      db.playerDao().insertPlayer(
        PlayerEntity(id = 0, name = "Player $i", handle = "@p$i", bandaiUid = "000$i", deckArchetype = "Deck", deckColor = "RED")
      )
    }

    val tourneyIdResult = repository.createNewTournament(
      name = "Tamer Battle",
      dateText = "2026-09-18",
      rounds = 4,
      roundDuration = 30,
      location = "Store A",
      matchFormat = "BO1",
      selectedPlayerIds = playerIds
    )
    val tourneyId = tourneyIdResult.getOrThrow()

    val matches = db.matchDao().getMatchesForRound(tourneyId, 1)
    assertEquals(8, matches.size)

    for (m in matches) {
      val res = repository.reportMatchScore(
        matchId = m.id,
        p1Score = 1,
        p2Score = 0,
        isDraw = false,
        winnerId = m.player1Id,
        durationMinutes = 15,
        firstTurnPlayerId = m.player1Id
      )
      assertTrue(res.isSuccess)
    }

    val updatedMatches = db.matchDao().getMatchesForRound(tourneyId, 1)
    assertEquals(8, updatedMatches.size)
    assertTrue(updatedMatches.all { it.isReported })

    val advanceResult = repository.advanceToNextRound(tourneyId, 1)
    assertTrue(advanceResult is com.example.data.repository.AdvanceResult.Success)

    val round2Matches = db.matchDao().getMatchesForRound(tourneyId, 2)
    assertEquals(8, round2Matches.size)
  }
}

