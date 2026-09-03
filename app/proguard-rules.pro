# =============================================================================
# Otimiza AI — ProGuard / R8 rules
# Foco: preservar imutabilidade dos IDs nativos, contratos Retrofit, reflexão
# Hilt/Room/ML Kit e o ciclo de vida do MapLibre/Mapbox SDK.
# =============================================================================

# ---- Kotlin / Coroutines ----------------------------------------------------
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-dontwarn kotlinx.coroutines.**
-keep class kotlin.Metadata { *; }

# ---- Value classes (Diretriz Crítica #1) ------------------------------------
# Preserva a imutabilidade e o unwrap correto de NativeStopId / PlatformId.
-keep class com.otimiza.delivery.domain.model.NativeStopId { *; }
-keep class com.otimiza.delivery.domain.model.PlatformId { *; }
-keep class com.otimiza.delivery.domain.model.Platform { *; }
-keep class com.otimiza.delivery.domain.model.DeliveryStop { *; }
-keep class com.otimiza.delivery.domain.model.RouteFinancialMetrics { *; }

# ---- Retrofit / OkHttp ------------------------------------------------------
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# Gson: preserva o contrato dos DTOs serializados.
-keep class com.otimiza.delivery.data.remote.** { *; }
-keepclassmembers class com.otimiza.delivery.data.remote.** {
    <init>(...);
    <fields>;
}

# ---- Room -------------------------------------------------------------------
-keep class androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# Mantém o nome das entidades (PK composta referenciada por nome de coluna).
-keep class com.otimiza.delivery.data.local.entity.** { *; }
-keep class com.otimiza.delivery.data.local.converter.** { *; }

# ---- Hilt / Dagger ----------------------------------------------------------
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper
-keep @dagger.hilt.android.lifecycle.HiltViewModel class *
-keepclasseswithmembers class * {
    @dagger.* <methods>;
}

# ---- ML Kit (Text Recognition) — reflexão nativa ----------------------------
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.vision.** { *; }
-dontwarn com.google.mlkit.**

# ---- CameraX ----------------------------------------------------------------
-keep class androidx.camera.** { *; }
-dontwarn androidx.camera.**

# ---- Mapbox / MapLibre Android SDK ------------------------------------------
-keep class com.mapbox.mapboxsdk.** { *; }
-keep interface com.mapbox.mapboxsdk.** { *; }
-dontwarn com.mapbox.mapboxsdk.**

# ---- AccessibilityService (config XML referencia o FQCN) --------------------
-keep class com.otimiza.delivery.service.DeliveryAccessibilityService { *; }

# ---- Enums (Platform) — usado em match/when -------------------------------
-keepclassmembers enum com.otimiza.delivery.domain.model.Platform {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ---- Native methods (caso libs nativas presentes) --------------------------
-keepclasseswithmembernames class * {
    native <methods>;
}

# ---- Paranoia geral ---------------------------------------------------------
-allowaccessmodification
-repackageclasses ''
-keepattributes SourceFile,LineNumberTable