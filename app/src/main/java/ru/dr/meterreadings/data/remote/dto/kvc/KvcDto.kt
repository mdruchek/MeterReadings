package ru.dr.meterreadings.data.remote.dto.kvc

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ==================== РЕГИОНЫ ====================

@Serializable
data class KvcRegionResponseDto(
    @SerialName("id") val id: Int,
    @SerialName("name") val name: String,
    @SerialName("codeRs") val codRs: String
)

// ==================== КАПТЧА ====================

@Serializable
data class CaptchaErrorDto(
    @SerialName("error") val error: String
)

// ==================== АБОНЕНТ ====================

@Serializable
data class KvcAccountRequestDto(
    @SerialName("account") val account: String,
    @SerialName("region") val region: Int,
    @SerialName("captchaToken") val captchaToken: String,
    @SerialName("password") val password: String?
)

@Serializable
data class KvcAccountResponseDto(
    @SerialName("id") val id: String,
    @SerialName("address") val address: String,
    @SerialName("account") val number: String,
    @SerialName("first") val submissionStartDay: Int,
    @SerialName("last") val submissionEndDay: Int,
    @SerialName("messages") val messages: List<String> = emptyList()
)

// ==================== СЧЕТЧИКИ ====================

@Serializable
data class KvcMetersResponseDTO(
    @SerialName("id") val id: String,
    @SerialName("number") val number: String,
    @SerialName("firstValue") val lastFirstValue: Float,
    @SerialName("secondValue") val lastSecondValue: Float,
    @SerialName("thirdValue") val lastThirdValue: Float,
    @SerialName("serviceName") val type: String,
    @SerialName("uninstallDate") val verificationDate: String,
    @SerialName("tariffZone") val tariffZone: Int,
    @SerialName("expirationNotice") val expirationNotice: Boolean,
    @SerialName("readOnly") val readOnly: Boolean,
    @SerialName("maxDiff") val maxDiff: Int,
    @SerialName("avgOnly") val avgOnly: Boolean,
) {
}

@Serializable
data class KvcMeterHistoryResponseDto(
    @SerialName("period") val submissionPeriod: String,           // "2026-01-01"
    @SerialName("lastValue") val lastValue: Double,     // 498.0
    @SerialName("prevValue") val prevValue: Double,     // 492.0
    @SerialName("diff") val diff: Double,               // 6.0
    @SerialName("indicationType") val indicationType: String, // "Общий"
    @SerialName("note") val note: String                // "Интернет"
)

//@Serializable
//data class CounterForInsertDto(
//    @SerialName("idCnt") val idCnt: Int,
//    @SerialName("server") val server: String,
//    @SerialName("dbname") val dbName: String,
//    @SerialName("idA") val idA: Int,
//    @SerialName("val") val `val`: String,
//    @SerialName("idType") val idType: String,
//    @SerialName("date") val date: String,
//    @SerialName("datB") val datB: String
//)
//
//@Serializable
//data class InsertCtrRequest(
//    @SerialName("servDb") val servDb: KvcLocationDto,
//    @SerialName("ctrForInsert") val ctrForInsert: List<CounterForInsertDto>,
//    @SerialName("notes") val notes: String = "",
//    @SerialName("category") val category: Int = 0
//)
//
//@Serializable
//data class GetTransmissionPeriodRequestDto(
//    @SerialName("servDb") val servDb: KvcLocationDto,
//    @SerialName("lc") val lc: String
//)
//
//@Serializable
//data class GetMetersRequestDto(
//    @SerialName("servDb") val servDb: KvcLocationDto,
//    @SerialName("lc") val lc: String,
//    @SerialName("idCnt") val idCnt: Int
//)

@Serializable
data class KvcValidationErrorDto(
    @SerialName("fieldName") val fieldName: String,
    @SerialName("errors") val errors: List<String>
)

// ==================== EXCEPTION ====================

class CaptchaRequiredException(message: String) : Exception(message)
