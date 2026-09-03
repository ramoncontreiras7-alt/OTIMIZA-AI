package com.otimiza.delivery.data.local

import androidx.room.TypeConverter
import com.otimiza.delivery.domain.model.Platform

class PlatformConverter {

    @TypeConverter
    fun fromPlatform(platform: Platform): String = platform.name

    @TypeConverter
    fun toPlatform(value: String): Platform = Platform.valueOf(value)
}
