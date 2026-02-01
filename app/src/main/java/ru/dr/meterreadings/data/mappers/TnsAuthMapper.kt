package ru.dr.meterreadings.data.mappers

import ru.dr.meterreadings.data.remote.dto.tns.TnsUserAuthResponse
import ru.dr.meterreadings.domain.connector.UserAuth

object TnsAuthMapper {

    /**
     * Маппинг DTO авторизации ТНС → Domain Model
     */
    fun TnsUserAuthResponse.toDomain(): UserAuth.UserAuthData {
        // Проверяем успешность авторизации
        val isSuccess = this.result == true &&
                this.statusCode == 200 &&
                this.data != null

        return UserAuth.UserAuthData(
            authSuccess = isSuccess,
            accessToken = this.data?.accessToken,
            refreshToken = this.data?.refreshToken,
            accessTokenExpires = this.data?.accessTokenExpires,
            refreshTokenExpires = this.data?.refreshTokenExpires
        )
    }
}
