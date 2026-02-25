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
    val id: String,
    val apiId: String,
    val accountId: String,
    val apiAccountId: String,
    val apiMeterId: Int,              // idCnt из API
    val type: String,                   // ✅ ДОБАВЛЕНО: "Холодная вода", "Электричество"
    val number: String,           // ✅ ДОБАВЛЕНО: "12345678"
    val lastSubmissionDate: Long?,
    val createAt: Long,
    val updateAt: Long
)
