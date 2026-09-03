package com.otimiza.delivery.domain.usecase

import com.otimiza.delivery.domain.model.DeliveryStop
import com.otimiza.delivery.domain.model.NativeStopId
import com.otimiza.delivery.domain.model.Platform
import com.otimiza.delivery.domain.model.PlatformId
import java.io.InputStream
import java.time.Instant

class DocumentParserUseCase {

    suspend operator fun invoke(
        inputStream: InputStream,
        platform: Platform,
        sessionId: String,
        fileType: FileType
    ): Result<List<DeliveryStop>> = runCatching {
        val rows = when (fileType) {
            FileType.CSV -> parseDelimited(inputStream)
            FileType.EXCEL -> parseSpreadsheet(inputStream)
            FileType.PDF -> parsePdfRomaneio(inputStream)
        }

        rows.mapIndexed { index, row ->
            val nativeId = row["native_stop_id"]
                ?: throw IllegalArgumentException("ID nativo ausente na linha $index")
            DeliveryStop(
                id = NativeStopId(nativeId),
                platform = platform,
                externalRef = PlatformId(row["external_ref"] ?: nativeId),
                address = row["address"] ?: "",
                latitude = row["latitude"]?.toDoubleOrNull() ?: 0.0,
                longitude = row["longitude"]?.toDoubleOrNull() ?: 0.0,
                sequence = index + 1,
                routeId = sessionId,
                createdAt = Instant.now()
            )
        }
    }

    private fun parseDelimited(input: InputStream): List<Map<String, String>> = emptyList()

    private fun parseSpreadsheet(input: InputStream): List<Map<String, String>> = emptyList()

    private fun parsePdfRomaneio(input: InputStream): List<Map<String, String>> = emptyList()
}

enum class FileType {
    CSV, EXCEL, PDF
}
