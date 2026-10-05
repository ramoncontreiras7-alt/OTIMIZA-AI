import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Módulo de domínio: Kotlin PURO. Não conhece Android, Room, Retrofit nem telas.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    // Flow (lista viva) é Kotlin puro, não Android.
    api(libs.kotlinx.coroutines.core)
    // Só a anotação @Inject (padrão Java), para os casos de uso do Passo 4.
    implementation(libs.javax.inject)

    testImplementation(platform(libs.junit5.bom))
    testImplementation(libs.junit5.jupiter)
    testRuntimeOnly(libs.junit5.launcher)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.test {
    useJUnitPlatform()
}
