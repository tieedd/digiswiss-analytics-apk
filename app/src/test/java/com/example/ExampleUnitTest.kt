package com.example

import com.example.data.local.PlayerEntity
import com.example.util.SwissPairingEngine
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun round1_pairing_is_randomized_and_pairs_all_players() {
    val players = (1..8).map { id ->
      PlayerEntity(
        id = id.toLong(),
        name = "Player $id",
        handle = "@player$id",
        bandaiUid = "000000000$id",
        deckArchetype = "Deck $id",
        deckColor = "RED"
      )
    }

    val pairings1 = SwissPairingEngine.generateNextRoundPairings(
      tournamentId = 1L,
      roundNumber = 1,
      players = players,
      pastMatches = emptyList()
    )

    // With 8 players, should generate 4 matches
    assertEquals(4, pairings1.size)
    val pairedPlayerIds = pairings1.flatMap { listOfNotNull(it.player1Id, it.player2Id) }.toSet()
    assertEquals(8, pairedPlayerIds.size)
  }

  @Test
  fun round1_with_odd_players_assigns_single_bye() {
    val players = (1..7).map { id ->
      PlayerEntity(
        id = id.toLong(),
        name = "Player $id",
        handle = "@player$id",
        bandaiUid = "000000000$id",
        deckArchetype = "Deck $id",
        deckColor = "BLUE"
      )
    }

    val pairings = SwissPairingEngine.generateNextRoundPairings(
      tournamentId = 1L,
      roundNumber = 1,
      players = players,
      pastMatches = emptyList()
    )

    assertEquals(4, pairings.size) // 3 normal matches + 1 BYE
    val byeMatch = pairings.find { it.isBye }
    assertNotNull(byeMatch)
    assertNull(byeMatch!!.player2Id)
    assertEquals(2, byeMatch.p1Score)
  }
}
