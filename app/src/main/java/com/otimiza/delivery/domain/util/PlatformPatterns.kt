package com.otimiza.delivery.domain.util

import com.otimiza.delivery.domain.model.Platform
import java.util.concurrent.ConcurrentHashMap

object PlatformPatterns {

    val IFOOD_REGEX: Regex = Regex("""(?i)\b(IF-\d{6,}|IFOOD-\d{6,})\b""")
    val ML_REGEX: Regex = Regex("""(?i)\b(MLB\d{8,}|ML-\d{8,})\b""")
    val LALAMOVE_REGEX: Regex = Regex("""(?i)\b(LALA-\d{6,})\b""")

    private val scannedCache = ConcurrentHashMap<String, Long>()
    private const val DEDUPLICATION_WINDOW_MS: Long = 30_000L

    fun identifyPlatformAndId(rawText: String): Pair<Platform, String>? {
        val match: Pair<Platform, String>? =
            IFOOD_REGEX.find(rawText)?.value?.let { Platform.IFOOD to it }
                ?: ML_REGEX.find(rawText)?.value?.let { Platform.MERCADO_LIVRE to it }
                ?: LALAMOVE_REGEX.find(rawText)?.value?.let { Platform.LALAMOVE to it }
        return match
    }

    fun isDuplicate(nativeStopId: String, currentTime: Long = System.currentTimeMillis()): Boolean {
        val lastSeen = scannedCache[nativeStopId]
        val isDup = lastSeen != null && (currentTime - lastSeen) < DEDUPLICATION_WINDOW_MS
        if (!isDup) scannedCache[nativeStopId] = currentTime
        return isDup
    }

    fun clearCache() {
        scannedCache.clear()
    }
}