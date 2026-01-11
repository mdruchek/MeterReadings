package ru.dr.meterreadings.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout                    // ← ДОБАВИТЬ
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object KtorModule {

    @Provides
    @Singleton
    fun provideHttpClient(): HttpClient {
        return HttpClient(Android) {
            // JSON сериализация
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    prettyPrint = true
                })
            }

            // Таймауты
            install(HttpTimeout) {
                requestTimeoutMillis = 15_000  // 15 секунд - общий таймаут запроса
                connectTimeoutMillis = 10_000  // 10 секунд - подключение к серверу
                socketTimeoutMillis = 10_000   // 10 секунд - чтение данных
            }

            // Логирование
            install(Logging) {
                level = LogLevel.ALL
                logger = object : Logger {
                    override fun log(message: String) {
                        println("🌐 [HTTP] $message")
                    }
                }
            }
        }
    }
}
