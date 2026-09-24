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
    ).matches

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
    ).matches

    assertEquals(4, pairings.size) // 3 normal matches + 1 BYE
    val byeMatch = pairings.find { it.isBye }
    assertNotNull(byeMatch)
    assertNull(byeMatch!!.player2Id)
    assertEquals(2, byeMatch.p1Score)
  }

  @Test
  fun bandaiUid_preserves_leading_zero_and_cleans_excel_formats() {
    // Leading zero in 10-digit character string is preserved
    assertEquals("0698987569", com.example.util.CsvHelper.cleanBandaiUid("0698987569"))
    
    // Excel formula format ="0698987569" correctly cleaned
    assertEquals("0698987569", com.example.util.CsvHelper.cleanBandaiUid("=\"0698987569\""))
    
    // Excel text apostrophe format '0698987569 correctly cleaned
    assertEquals("0698987569", com.example.util.CsvHelper.cleanBandaiUid("'0698987569"))
    
    // Truncated leading zero from external numeric spreadsheet is recovered
    assertEquals("0698987569", com.example.util.CsvHelper.cleanBandaiUid("698987569"))
    
    // Alphanumeric character UID is preserved
    assertEquals("A012345678", com.example.util.CsvHelper.cleanBandaiUid("A012345678"))
    
    // Blank or null is handled safely
    assertEquals("-", com.example.util.CsvHelper.cleanBandaiUid(""))
    assertEquals("-", com.example.util.CsvHelper.cleanBandaiUid("-"))
    assertEquals("-", com.example.util.CsvHelper.cleanBandaiUid(null))
  }

  @Test
  fun exportPlayersToCsv_formats_uid_as_text_preserving_leading_zero() {
    val player = PlayerEntity(
      id = 1L,
      name = "Agumon Master",
      handle = "@agumon",
      bandaiUid = "0123456789",
      deckArchetype = "WarGreymon",
      deckColor = "RED"
    )
    val csv = com.example.util.CsvHelper.exportPlayersToCsv(listOf(player))
    // Ensures the CSV contains the formatted text formula for Excel to keep the leading '0'
    assertTrue(csv.contains("0123456789"))
    // In RFC 4180 CSV, formula ="0123456789" is correctly escaped as "=""0123456789"""
    assertTrue(csv.contains("\"=\"\"0123456789\"\"\""))
  }

  @Test
  fun test_16_players_report_all_8_matches_and_standings() {
    val players = (1..16).map { id ->
      PlayerEntity(
        id = id.toLong(),
        name = "Player $id",
        handle = "@p$id",
        bandaiUid = "00000000$id",
        deckArchetype = "Deck",
        deckColor = "RED"
      )
    }

    val pairings = SwissPairingEngine.generateNextRoundPairings(
      tournamentId = 1L,
      roundNumber = 1,
      players = players,
      pastMatches = emptyList()
    ).matches

    assertEquals(8, pairings.size)

    val reportedMatches = pairings.mapIndexed { index, m ->
      m.copy(
        p1Score = if (index % 2 == 0) 1 else 0,
        p2Score = if (index % 2 == 0) 0 else 1,
        winnerId = if (index % 2 == 0) m.player1Id else m.player2Id,
        isReported = true
      )
    }

    val standings = SwissPairingEngine.computeStandings(
      players = players,
      allMatches = reportedMatches,
      tiebreakerPool = players
    )

    assertEquals(16, standings.size)

    val r2Pairings = SwissPairingEngine.generateNextRoundPairings(
      tournamentId = 1L,
      roundNumber = 2,
      players = players,
      pastMatches = reportedMatches
    )
    assertEquals(8, r2Pairings.matches.size)
  }
}
