package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.MatchEntity
import com.example.data.local.PlayerEntity
import com.example.data.local.TournamentEntity
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object CsvHelper {

    // ---------- penulisan ----------

    /**
     * PERBAIKAN UTAMA. Versi lama menyambung nilai dengan "$a,$b" tanpa escape,
     * sehingga nama seperti `Budi, Jr.` atau archetype `Red/Black "Aggro"`
     * menggeser seluruh kolom dan merusak file saat diimpor kembali.
     */
    private fun esc(value: Any?): String {
        val s = value?.toString() ?: ""
        val needsQuote = s.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        return if (needsQuote) "\"" + s.replace("\"", "\"\"") + "\"" else s
    }

    private fun StringBuilder.row(vararg cells: Any?) {
        append(cells.joinToString(",") { esc(it) }).append("\r\n")
    }

    fun cleanBandaiUid(raw: String?): String {
        if (raw.isNullOrBlank()) return "-"
        var s = raw.trim()
        if (s == "-" || s.equals("null", ignoreCase = true)) return "-"

        // Bersihkan format formula Excel: ="0123456789"
        if (s.startsWith("=")) {
            s = s.removePrefix("=").trim()
        }
        // Bersihkan tanda kutip ganda, tunggal, atau apostrof penanda teks Excel
        s = s.removeSurrounding("\"").removeSurrounding("'").trim()
        s = s.replace("\"", "").trim()
        s = s.removePrefix("'").trim()

        if (s.isBlank() || s == "-") return "-"

        // Jika UID numerik sempat terpangkas angka 0 depannya oleh Excel (misal 9 digit menjadi angka),
        // kembalikan padding 0 di depannya menjadi 10 digit karakter.
        if (s.all { it.isDigit() } && s.length in 1..9) {
            s = s.padStart(10, '0')
        }

        return s
    }

    fun formatUidForCsv(uid: String): String {
        val clean = uid.trim()
        if (clean.isBlank() || clean == "-") return "-"
        // Format ="VALUE" memaksa Excel/Spreadsheet memperlakukan kolom sebagai karakter teks,
        // sehingga angka '0' di awal UID TIDAK AKAN PERNAH terhapus atau diubah menjadi integer.
        return "=\"$clean\""
    }

    fun exportPlayersToCsv(players: List<PlayerEntity>): String = buildString {
        append("\uFEFF") // UTF-8 BOM untuk kompatibilitas Excel Windows
        row("ID", "Nama", "Handle", "UID Bandai", "Archetype", "Warna Deck")
        players.forEach { p ->
            row(p.id, p.name, p.handle, formatUidForCsv(p.bandaiUid), p.deckArchetype, p.deckColor)
        }
    }

    fun exportTournamentToCsv(tournament: TournamentEntity, matches: List<MatchEntity>): String =
        buildString {
            row("Data Turnamen")
            row("Nama Turnamen", tournament.name)
            row("Tanggal", tournament.dateText)
            row("Format", tournament.matchFormat)
            row("Status", tournament.status)
            append("\r\n")
            row("Histori Pertandingan")
            // Kolom isBye ditambahkan: tanpa ini, match BYE hasil impor kehilangan
            // penandanya dan dihitung ulang secara berbeda oleh mesin klasemen.
            row(
                "Ronde", "Meja", "Pemain 1 ID", "Pemain 2 ID", "Skor P1", "Skor P2",
                "Draw", "Pemenang ID", "Durasi (Menit)", "Bye"
            )
            matches.forEach { m ->
                row(
                    m.roundNumber, m.tableNumber, m.player1Id, m.player2Id ?: "",
                    m.p1Score, m.p2Score, m.isDraw, m.winnerId ?: "",
                    m.matchDurationMinutes, m.isBye
                )
            }
        }

    // ---------- pembacaan ----------

    /** Parser CSV yang memahami tanda kutip, koma di dalam sel, dan escape `""`. */
    private fun parseLine(line: String): List<String> {
        val out = mutableListOf<String>()
        val cur = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                inQuotes && c == '"' && i + 1 < line.length && line[i + 1] == '"' -> {
                    cur.append('"'); i++
                }
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    out.add(cur.toString().trim()); cur.setLength(0)
                }
                else -> cur.append(c)
            }
            i++
        }
        out.add(cur.toString().trim())
        return out
    }

    fun importPlayersFromCsv(context: Context, uri: Uri): List<PlayerEntity> {
        val players = mutableListOf<PlayerEntity>()
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { reader ->
                    var nameColIdx = 1
                    var handleColIdx = 2
                    var uidColIdx = 3
                    var archetypeColIdx = 4
                    var colorColIdx = 5

                    reader.readLines().forEachIndexed { idx, raw ->
                        val line = raw.trim().removePrefix("\uFEFF")   // buang BOM Excel
                        if (line.isBlank()) return@forEachIndexed
                        val t = parseLine(line)
                        if (t.isEmpty()) return@forEachIndexed

                        // Deteksi otomatis posisi kolom header di baris pertama
                        if (idx == 0) {
                            val lower = t.map { it.lowercase().trim() }
                            val hasName = lower.any { it.contains("nama") || it.contains("name") }
                            if (hasName) {
                                nameColIdx = lower.indexOfFirst { it.contains("nama") || it.contains("name") }.coerceAtLeast(0)
                                val hIdx = lower.indexOfFirst { it.contains("handle") || it.contains("username") }
                                if (hIdx != -1) handleColIdx = hIdx
                                val uIdx = lower.indexOfFirst { it.contains("uid") || it.contains("bandai") }
                                if (uIdx != -1) uidColIdx = uIdx
                                val aIdx = lower.indexOfFirst { it.contains("archetype") || it.contains("deck") }
                                if (aIdx != -1) archetypeColIdx = aIdx
                                val cIdx = lower.indexOfFirst { it.contains("warna") || it.contains("color") }
                                if (cIdx != -1) colorColIdx = cIdx
                                return@forEachIndexed
                            }
                        }

                        val name = t.getOrNull(nameColIdx).orEmpty().trim()
                        if (name.isBlank() || name.equals("nama", ignoreCase = true) || name.equals("name", ignoreCase = true)) {
                            return@forEachIndexed // baris header atau baris tanpa nama dilewati
                        }

                        val rawHandle = t.getOrNull(handleColIdx).orEmpty().trim()
                        val cleanHandle = if (rawHandle.isBlank() || rawHandle == "-") "-" else if (rawHandle.startsWith("@")) rawHandle else "@$rawHandle"
                        val cleanUid = cleanBandaiUid(t.getOrNull(uidColIdx))
                        val archetype = t.getOrNull(archetypeColIdx).orEmpty().trim().ifBlank { "Unknown" }
                        val color = t.getOrNull(colorColIdx).orEmpty().trim().ifBlank { "UNKNOWN" }

                        players.add(
                            PlayerEntity(
                                name = name,
                                handle = cleanHandle,
                                bandaiUid = cleanUid,
                                deckArchetype = archetype,
                                deckColor = color,
                                avatarId = (1..8).random()
                            )
                        )
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
            context.contentResolver.openInputStream(uri)?.use { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).use { reader ->
                    var name = "Imported Tournament"
                    var date = "Unknown"
                    var format = "BO3"
                    var status = "COMPLETED"
                    val matches = mutableListOf<MatchEntity>()
                    var parsingMatches = false

                    for (raw in reader.readLines()) {
                        val line = raw.trim().removePrefix("\uFEFF")
                        if (line.isBlank()) continue
                        val t = parseLine(line)
                        val head = t.firstOrNull().orEmpty()

                        when {
                            head == "Nama Turnamen" -> name = t.getOrNull(1) ?: name
                            head == "Tanggal" -> date = t.getOrNull(1) ?: date
                            head == "Format" -> format = t.getOrNull(1) ?: format
                            head == "Status" -> status = t.getOrNull(1) ?: status
                            head == "Histori Pertandingan" -> parsingMatches = true
                            head == "Ronde" -> Unit                 // baris header
                            parsingMatches && t.size >= 8 -> {
                                val p1Id = t[2].toLongOrNull() ?: continue
                                val p2Id = t[3].toLongOrNull()
                                val isBye = t.getOrNull(9)?.toBooleanStrictOrNull() ?: (p2Id == null)
                                matches.add(
                                    MatchEntity(
                                        tournamentId = 0L,
                                        roundNumber = t[0].toIntOrNull() ?: 1,
                                        tableNumber = t[1].toIntOrNull() ?: 1,
                                        player1Id = p1Id,
                                        player2Id = p2Id,
                                        // skor dibatasi 0..2 agar data rusak tidak
                                        // meracuni perhitungan GW%/OGW%
                                        p1Score = (t[4].toIntOrNull() ?: 0).coerceIn(0, 2),
                                        p2Score = (t[5].toIntOrNull() ?: 0).coerceIn(0, 2),
                                        isDraw = t[6].toBooleanStrictOrNull() ?: false,
                                        winnerId = t[7].toLongOrNull(),
                                        matchDurationMinutes = (t.getOrNull(8)?.toIntOrNull() ?: 0).coerceIn(0, 600),
                                        isBye = isBye,
                                        isReported = true
                                    )
                                )
                            }
                        }
                    }

                    if (matches.isEmpty()) return null
                    val maxRound = matches.maxOf { it.roundNumber }
                    return TournamentEntity(
                        name = name,
                        dateText = date,
                        matchFormat = if (format.equals("BO1", true)) "BO1" else "BO3",
                        status = status,
                        totalRounds = maxRound,
                        currentRound = maxRound
                    ) to matches
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    // ---------- berbagi ----------

    /**
     * Perbaikan: nama file dibersihkan, dan FLAG_ACTIVITY_NEW_TASK ditambahkan supaya
     * tidak crash kalau dipanggil dari Context non-Activity (mis. applicationContext).
     * Panggil dari coroutine Dispatchers.IO karena ada penulisan file.
     */
    fun shareCsv(context: Context, csvData: String, fileName: String) {
        try {
            val safeName = fileName.replace(Regex("[^A-Za-z0-9._-]"), "_").take(80)
                .ifBlank { "digiswiss_export" }
            val file = File(context.cacheDir, "$safeName.csv")
            file.writeText(csvData, Charsets.UTF_8)

            val uri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file
            )

            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(send, "Bagikan CSV via")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
