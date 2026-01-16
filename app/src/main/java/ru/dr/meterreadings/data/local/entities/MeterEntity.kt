package ru.dr.meterreadings.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "meters",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("accountId")]
)
data class MeterEntity(
    @PrimaryKey
    val id: String,                     // "accountId_counterId"
    val accountId: String,
    val apiCounterId: Int,              // idCnt из API
    val type: String,                   // ✅ ДОБАВЛЕНО: "Холодная вода", "Электричество"
    val serialNumber: String,           // ✅ ДОБАВЛЕНО: "12345678"
    val lastSubmissionDate: String?     // "dd.MM.yyyy" или null
)
