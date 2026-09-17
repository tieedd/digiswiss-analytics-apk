package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.PlayerEntity
import com.example.data.local.TournamentEntity
import com.example.data.local.MatchEntity
import java.io.BufferedReader
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader

object CsvHelper {

    fun exportPlayersToCsv(players: List<PlayerEntity>): String {
        val sb = StringBuilder()
        sb.append("ID,Nama,Handle,UID Bandai,Archetype,Warna Deck\n")
        players.forEach { p ->
            sb.append("${p.id},${p.name},${p.handle},${p.bandaiUid},${p.deckArchetype},${p.deckColor}\n")
        }
        return sb.toString()
    }

    fun exportTournamentToCsv(tournament: TournamentEntity, matches: List<MatchEntity>): String {
        val sb = StringBuilder()
        sb.append("Data Turnamen\n")
        sb.append("Nama Turnamen,${tournament.name}\n")
        sb.append("Tanggal,${tournament.dateText}\n")
        sb.append("Format,${tournament.matchFormat}\n")
        sb.append("Status,${tournament.status}\n\n")

        sb.append("Histori Pertandingan\n")
        sb.append("Ronde,Meja,Pemain 1 ID,Pemain 2 ID,Skor P1,Skor P2,Draw,Pemenang ID,Durasi (Menit)\n")
        matches.forEach { m ->
            sb.append("${m.roundNumber},${m.tableNumber},${m.player1Id},${m.player2Id},${m.p1Score},${m.p2Score},${m.isDraw},${m.winnerId},${m.matchDurationMinutes}\n")
        }
        return sb.toString()
    }

    fun importPlayersFromCsv(context: Context, uri: Uri): List<PlayerEntity> {
        val players = mutableListOf<PlayerEntity>()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    val lines = reader.readLines()
                    if (lines.size > 1) {
                        for (i in 1 until lines.size) {
                            val line = lines[i].trim()
                            if (line.isBlank() || line.startsWith("ID")) continue
                            val tokens = line.split(",")
                            if (tokens.size >= 5) {
                                players.add(
                                    PlayerEntity(
                                        name = tokens[1],
                                        handle = tokens[2],
                                        bandaiUid = tokens[3],
                                        deckArchetype = tokens.getOrNull(4) ?: "",
                                        deckColor = tokens.getOrNull(5) ?: ""
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return players
    }

    fun importTournamentFromCsv(context: Context, uri: Uri): Pair<TournamentEntity, List<MatchEntity>>? {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    val lines = reader.readLines()
                    var name = "Imported Tournament"
                    var date = "Unknown"
                    var format = "Swiss"
                    var status = "COMPLETED"
                    
                    val matches = mutableListOf<MatchEntity>()
                    var isParsingMatches = false
                    
                    for (line in lines) {
                        val trimmed = line.trim()
                        if (trimmed.isBlank()) continue
                        
                        if (trimmed.startsWith("Nama Turnamen,")) name = trimmed.split(",").getOrNull(1) ?: name
                        else if (trimmed.startsWith("Tanggal,")) date = trimmed.split(",").getOrNull(1) ?: date
                        else if (trimmed.startsWith("Format,")) format = trimmed.split(",").getOrNull(1) ?: format
                        else if (trimmed.startsWith("Status,")) status = trimmed.split(",").getOrNull(1) ?: status
                        else if (trimmed.startsWith("Histori Pertandingan")) isParsingMatches = true
                        else if (isParsingMatches && !trimmed.startsWith("Ronde,")) {
                            val tokens = trimmed.split(",")
                            if (tokens.size >= 8) {
                                val rNumber = tokens[0].toIntOrNull() ?: 1
                                val tNumber = tokens[1].toIntOrNull() ?: 1
                                val p1Id = tokens[2].toLongOrNull() ?: 0L
                                val p2Id = tokens[3].toLongOrNull()
                                val p1Score = tokens[4].toIntOrNull() ?: 0
                                val p2Score = tokens[5].toIntOrNull() ?: 0
                                val isDraw = tokens[6].toBooleanStrictOrNull() ?: false
                                val winnerId = tokens[7].toLongOrNull()
                                val duration = tokens.getOrNull(8)?.toIntOrNull() ?: 0
                                
                                matches.add(
                                    MatchEntity(
                                        tournamentId = 0L, // will be replaced
                                        roundNumber = rNumber,
                                        tableNumber = tNumber,
                                        player1Id = p1Id,
                                        player2Id = p2Id,
                                        p1Score = p1Score,
                                        p2Score = p2Score,
                                        isDraw = isDraw,
                                        winnerId = winnerId,
                                        matchDurationMinutes = duration,
                                        isReported = true
                                    )
                                )
                            }
                        }
                    }
                    
                    val tournament = TournamentEntity(
                        name = name,
                        dateText = date,
                        matchFormat = format,
                        status = status,
                        totalRounds = matches.maxOfOrNull { it.roundNumber } ?: 1,
                        currentRound = matches.maxOfOrNull { it.roundNumber } ?: 1
                    )
                    return Pair(tournament, matches)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun shareCsv(context: Context, csvData: String, fileName: String) {
        try {
            val file = File(context.cacheDir, "$fileName.csv")
            val writer = FileWriter(file)
            writer.write(csvData)
            writer.flush()
            writer.close()

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Bagikan CSV via"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
