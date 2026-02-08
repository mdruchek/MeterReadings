package ru.dr.meterreadings.data.remote.dto.kvc

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ==================== РЕГИОНЫ ====================

@Serializable
data class KvcRegionDto(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
    @SerialName("codeRs") val codRs: String
)

@Serializable
data class KvcLocationDto(
    @SerialName("server") val server: String,
    @SerialName("dbname") val dbName: String,
    @SerialName("login") val login: String? = null,
    @SerialName("iduser") val idUser: Int? = null
)

// ==================== GetAbonentInfo ====================

// ✅ ИСПРАВЛЕНО: точное соответствие HAR
@Serializable
data class GetAbonentInfoRequest(
    @SerialName("account") val account: String,
    @SerialName("region") val region: Int,
    @SerialName("captchaToken") val captchaToken: String,
    @SerialName("password") val password: String?
)

// ✅ ДОБАВЛЕНО: для обработки ошибок капчи
@Serializable
data class CaptchaErrorDto(
    @SerialName("error") val error: String
)

// ✅ ИСПРАВЛЕНО: поля из успешного ответа HAR
@Serializable
data class KvcAccountInfoDto(
    @SerialName("id") val id: String,
    @SerialName("address") val address: String,
    @SerialName("account") val account: String,
    @SerialName("first") val first: Int,
    @SerialName("last") val last: Int,
    @SerialName("messages") val messages: List<String> = emptyList()
) {
}

// ==================== GetCntList ====================

// ❌ УДАЛЕНО: GetCntListRequest (теперь GET запрос)

@Serializable
data class KvcMetersDto(
    @SerialName("idcnt") val idCnt: Int,
    @SerialName("iddummy") val idDummy: Int,
    @SerialName("ida") val idA: Int,
    @SerialName("lc") val lc: String,
    @SerialName("idserv") val idServ: String,
    @SerialName("number") val number: String,
    @SerialName("servname") val servName: String,
    @SerialName("idgp") val idGp: Int,
    @SerialName("idttype") val idTtype: String,
    @SerialName("idtype") val idType: String,
    @SerialName("cnttarifzone") val cntTarifZone: String,
    @SerialName("datlst") val datLst: String,
    @SerialName("cvallst") val cValLst: String,
    @SerialName("cvallst03") val cValLst03: String,
    @SerialName("cvallst04") val cValLst04: String,
    @SerialName("cvallstcheck") val cValLstCheck: String,
    @SerialName("cvallst03check") val cValLst03Check: String,
    @SerialName("cvallst04check") val cValLst04Check: String,
    @SerialName("datb") val datB: String,
    @SerialName("datsn") val datSn: String,
    @SerialName("dbname") val dbName: String,
    @SerialName("server") val server: String,
    @SerialName("expirationNotice") val expirationNotice: Int,
    @SerialName("maxdiff") val maxDiff: Int,
    @SerialName("readonly") val readOnly: Int,
    @SerialName("avgonly") val avgOnly: Int,
    @SerialName("status") val status: String? = null
) {
    fun getDisplayName(): String = "${servName.trim()} №${number.trim()}"
    fun getLastReadingDay(): String = cValLst.trim()
    fun getLastReadingNight(): String? = if (idTtype == "2T") cValLst03.trim().takeIf { it != "0" } else null
    fun isExpirationSoon(): Boolean = expirationNotice == 1
    fun canEdit(): Boolean = readOnly == 0
}

// ==================== ОСТАЛЬНЫЕ DTO ====================


@Serializable
data class KvcMeterHistoryDto(
    @SerialName("period") val period: String,           // "2026-01-01"
    @SerialName("lastValue") val lastValue: Double,     // 498.0
    @SerialName("prevValue") val prevValue: Double,     // 492.0
    @SerialName("diff") val diff: Double,               // 6.0
    @SerialName("indicationType") val indicationType: String, // "Общий"
    @SerialName("note") val note: String                // "Интернет"
)

@Serializable
data class CounterForInsertDto(
    @SerialName("idCnt") val idCnt: Int,
    @SerialName("server") val server: String,
    @SerialName("dbname") val dbName: String,
    @SerialName("idA") val idA: Int,
    @SerialName("val") val `val`: String,
    @SerialName("idType") val idType: String,
    @SerialName("date") val date: String,
    @SerialName("datB") val datB: String
)

@Serializable
data class InsertCtrRequest(
    @SerialName("servDb") val servDb: KvcLocationDto,
    @SerialName("ctrForInsert") val ctrForInsert: List<CounterForInsertDto>,
    @SerialName("notes") val notes: String = "",
    @SerialName("category") val category: Int = 0
)

@Serializable
data class GetTransmissionPeriodRequestDto(
    @SerialName("servDb") val servDb: KvcLocationDto,
    @SerialName("lc") val lc: String
)

@Serializable
data class GetMetersRequestDto(
    @SerialName("servDb") val servDb: KvcLocationDto,
    @SerialName("lc") val lc: String,
    @SerialName("idCnt") val idCnt: Int
)

// ==================== EXCEPTION ====================

class CaptchaRequiredException(message: String) : Exception(message)
