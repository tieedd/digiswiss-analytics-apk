package com.example.util

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * FILE BARU.
 *
 * Sebelumnya password disimpan apa adanya di tabel `users` dan dibandingkan dengan
 * `user.password == password`. Karena `android:allowBackup="true"`, isi database
 * ikut terbawa ke backup, jadi seluruh password peserta bisa terbaca.
 *
 * PBKDF2-HMAC-SHA256 tersedia sejak API 19 dan tidak butuh dependency tambahan.
 */
object PasswordHasher {

    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH = 256
    private const val SALT_BYTES = 16
    private const val PREFIX = "pbkdf2\$sha256\$"

    fun hash(password: String): String {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val hash = derive(password, salt)
        return PREFIX + ITERATIONS + "$" + b64(salt) + "$" + b64(hash)
    }

    /**
     * Menerima hash baru maupun password lama berbentuk teks polos, supaya akun
     * yang sudah terlanjur dibuat tetap bisa login setelah update.
     */
    fun verify(password: String, stored: String): Boolean {
        if (!stored.startsWith(PREFIX)) {
            return constantTimeEquals(password.toByteArray(), stored.toByteArray())
        }
        val parts = stored.removePrefix(PREFIX).split("$")
        if (parts.size != 3) return false
        val iterations = parts[0].toIntOrNull() ?: return false
        val salt = unb64(parts[1]) ?: return false
        val expected = unb64(parts[2]) ?: return false
        return constantTimeEquals(derive(password, salt, iterations), expected)
    }

    /** True kalau hash tersimpan masih format lama dan perlu di-upgrade saat login. */
    fun needsRehash(stored: String): Boolean = !stored.startsWith(PREFIX)

    private fun derive(password: String, salt: ByteArray, iterations: Int = ITERATIONS): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, KEY_LENGTH)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    private fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].toInt() xor b[i].toInt())
        return diff == 0
    }

    private fun b64(bytes: ByteArray): String =
        Base64.encodeToString(bytes, Base64.NO_WRAP or Base64.NO_PADDING)

    private fun unb64(s: String): ByteArray? =
        runCatching { Base64.decode(s, Base64.NO_WRAP or Base64.NO_PADDING) }.getOrNull()
}
