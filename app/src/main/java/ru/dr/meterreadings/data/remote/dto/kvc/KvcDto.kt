package ru.dr.meterreadings.data.remote.dto.kvc

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Регион КВЦ (район)
 */
@Serializable
data class KvcRegionDto(
    @SerialName("id")
    val id: Int,

    @SerialName("name")
    val name: String,

    @SerialName("codeRs")
    val codRs: String
)

/**
 * Конфигурация БД провайдера
 */
@Serializable
data class KvcLocationDto(
    @SerialName("server")
    val server: String,

    @SerialName("db_name")
    val dbName: String,

    @SerialName("login")
    val login: String? = null,

    @SerialName("id_user")
    val idUser: Int? = null
)

/**
 * Информация об абоненте КВЦ
 *
 * Возвращается из GetAbonentInfo
 */
@Serializable
data class KvcAccountInfoDto(
    /** ID абонента в системе КВЦ */
    @SerialName("id")
    val id: Int,

    /** Лицевой счёт */
    @SerialName("lc")
    val lc: String,

    /** Код района */
    @SerialName("cod_rs")
    val codRs: String,

    /** Дом и квартира (форматированная строка) */
    @SerialName("dom_kv")
    val domKv: String,

    /** ФИО абонента (может быть null) */
    @SerialName("fio")
    val fio: String? = null,

    /** Название населённого пункта */
    @SerialName("tn_name")
    val tnName: String,

    /** Название улицы */
    @SerialName("st_name")
    val stName: String,

    /** ID адреса в системе */
    @SerialName("id_adr")
    val idAdr: String,

    /** Пароль счётчика (может быть null) */
    @SerialName("pass_ctr")
    val passCtr: String? = null,

    /** Конфигурация БД где найден абонент */
    @SerialName("location")
    val location: KvcLocationDto,

    /** Название населённого пункта (альтернативное поле) */
    @SerialName("name_tn")
    val nameTn: String,

    /** Название улицы (альтернативное поле) */
    @SerialName("name_st")
    val nameSt: String,

    /** Номер лицевого счёта в ГИС ЖКХ (может быть null) */
    @SerialName("gis_account")
    val gisAccount: String? = null,

    /** Номер дома */
    @SerialName("dom")
    val dom: String,

    /** Буква дома */
    @SerialName("domb")
    val domb: String,

    /** Корпус */
    @SerialName("kor")
    val kor: String,

    /** Номер квартиры */
    @SerialName("kvart")
    val kvart: String,

    /** Номер комнаты */
    @SerialName("komnata")
    val komnata: String
) {
    /**
     * Полный адрес абонента (для отображения)
     */
    fun getFullAddress(): String {
        val parts = mutableListOf<String>()

        if (tnName.isNotBlank()) parts.add(tnName)
        if (stName.isNotBlank()) parts.add(stName)
        if (domKv.isNotBlank()) parts.add(domKv)

        return parts.joinToString(", ")
    }

    /**
     * Короткое описание для списка
     */
    fun getShortDescription(): String {
        return "${fio ?: "Без имени"} • ЛС: $lc"
    }
}

/**
 * Счётчик абонента КВЦ
 *
 * Возвращается из GetCntList
 */
@Serializable
data class KvcMetersDto(
    /** ID счётчика */
    @SerialName("id_cnt")
    val idCnt: Int,

    /** ID дубликата счётчика */
    @SerialName("id_dummy")
    val idDummy: Int,

    /** ID абонента */
    @SerialName("id_a")
    val idA: Int,

    /** Лицевой счёт */
    @SerialName("lc")
    val lc: String,

    /** ID типа услуги (07=ГВС, 08=ХВС, 13=Электро) */
    @SerialName("id_serv")
    val idServ: String,

    /** Номер счётчика */
    @SerialName("number")
    val number: String,

    /** Название услуги */
    @SerialName("serv_name")
    val servName: String,

    /** ID группы приборов учёта */
    @SerialName("id_gp")
    val idGp: Int,

    /** Тип тарифа (1T=однотарифный, 2T=двухтарифный) */
    @SerialName("id_ttype")
    val idTtype: String,

    /** Тип прибора */
    @SerialName("id_type")
    val idType: String,

    /** Количество тарифных зон */
    @SerialName("cnt_tarif_zone")
    val cntTarifZone: String,

    /** Дата последних показаний (текст) */
    @SerialName("dat_lst")
    val datLst: String,

    /** Последнее показание (тариф 1 / день) */
    @SerialName("c_val_lst")
    val cValLst: String,

    /** Последнее показание (тариф 2 / ночь) */
    @SerialName("c_val_lst03")
    val cValLst03: String,

    /** Последнее показание (тариф 3) */
    @SerialName("c_val_lst04")
    val cValLst04: String,

    /** Проверенное показание 1 */
    @SerialName("c_val_lst_check")
    val cValLstCheck: String,

    /** Проверенное показание 2 */
    @SerialName("c_val_lst03_check")
    val cValLst03Check: String,

    /** Проверенное показание 3 */
    @SerialName("c_val_lst04_check")
    val cValLst04Check: String,

    /** период последней передачи, важен только месяц и год, поэтому остальное (день и время) по нулям */
    @SerialName("dat_b")
    val datB: String,

    /** Дата окончания поверки */
    @SerialName("dat_sn")
    val datSn: String,

    /** Название БД */
    @SerialName("db_name")
    val dbName: String,

    /** Сервер БД */
    @SerialName("server")
    val server: String,

    /** Уведомление об истечении срока поверки (0=нет, 1=да) */
    @SerialName("expirationNotice")
    val expirationNotice: Int,

    /** Максимальная разница показаний */
    @SerialName("max_diff")
    val maxDiff: Int,

    /** Только для чтения (0=нет, 1=да) */
    @SerialName("read_only")
    val readOnly: Int,

    /** Только средние значения (0=нет, 1=да) */
    @SerialName("avg_only")
    val avgOnly: Int,

    /** Статус счётчика */
    @SerialName("status")
    val status: String? = null
) {
    /**
     * Отформатированное название для отображения
     */
    fun getDisplayName(): String {
        return "${servName.trim()} • ${number.trim()}"
    }

    /**
     * Последнее показание (день)
     */
    fun getLastReadingDay(): String {
        return cValLst.trim()
    }

    /**
     * Последнее показание (ночь) - для двухтарифных счётчиков
     */
    fun getLastReadingNight(): String? {
        return if (idTtype == "2T" && cValLst03.trim() != "0") {
            cValLst03.trim()
        } else null
    }

    /**
     * Проверка срока поверки
     * @return true если скоро истекает
     */
    fun isExpirationSoon(): Boolean {
        return expirationNotice == 1
    }

    /**
     * Можно ли редактировать показания
     */
    fun canEdit(): Boolean {
        return readOnly == 0
    }
}

/**
 * Диапазон дней для передачи показаний
 *
 * Возвращается из GetCtrDays
 */
@Serializable
data class KvcTransmissionPeriodDto(
    /** Первый день месяца (включительно) */
    @SerialName("first")
    val first: Int,

    /** Последний день месяца (включительно) */
    @SerialName("last")
    val last: Int
) {
    /**
     * Проверка: можно ли сегодня передать показания
     *
     * @param currentDay Текущий день месяца (1-31)
     * @return true если текущий день в разрешённом диапазоне
     */
    fun canSubmitToday(currentDay: Int): Boolean {
        return currentDay in first..last
    }

    /**
     * Получить текстовое описание диапазона
     */
    fun getRangeDescription(): String {
        return "с $first по $last число"
    }

    /**
     * Сколько дней осталось до начала периода (если ещё рано)
     * @return количество дней или null если период уже идёт/закончился
     */
    fun daysUntilStart(currentDay: Int): Int? {
        return if (currentDay < first) {
            first - currentDay
        } else null
    }

    /**
     * Сколько дней осталось до конца периода (если период идёт)
     * @return количество дней или null если период не начался/закончился
     */
    fun daysUntilEnd(currentDay: Int): Int? {
        return if (currentDay in first..last) {
            last - currentDay
        } else null
    }
}

/**
 * История показаний счётчика КВЦ
 *
 * Возвращается из GetCtrList (история по одному счётчику)
 */
@Serializable
data class KvcMeterHistoryDto(
    /** ID записи истории */
    @SerialName("id")
    val id: Int,

    /** ID счётчика */
    @SerialName("id_cnt")
    val idCnt: Int,

    /** Предыдущее показание данного периода */
    @SerialName("val_pr")
    val valPr: Double,

    /** Показание переданное в данный период */
    @SerialName("val_lst")
    val valLst: Double,

    /** период передачи показаний, важен только год и месяц, поэтому остальное - нули */
    @SerialName("dat_b")
    val datB: String, // "2025-11-01T00:00:00"

    /** Рассчитано автоматически? */
    @SerialName("is_calc")
    val isCalc: Boolean,

    /** Разница показаний (расход за период) */
    @SerialName("diff")
    val diff: Double,

    /** Тип счётчика */
    @SerialName("type_ctr")
    val typeCtr: String,

    /** Описание услуги */
    @SerialName("description")
    val description: String
)

/**
 * Запрос GetAbonentInfo
 */
@Serializable
data class GetAbonentInfoRequest(
    @SerialName("servDbs")
    val servDbs: List<KvcLocationDto>,

    @SerialName("lc")
    val lc: String,

    @SerialName("target")
    val target: Int = 0
)

/**
 * Запрос GetCntList - получение списка счётчиков абонента
 */
@Serializable
data class GetCntListRequest(
    @SerialName("servDb")
    val servDb: KvcLocationDto,

    @SerialName("lc")
    val lc: String,

    @SerialName("idCnt")
    val idCnt: Int? = null
)

/**
 * Счётчик для отправки показаний
 */
@Serializable
data class CounterForInsertDto(
    /** ID счётчика */
    @SerialName("idCnt")
    val idCnt: Int,

    /** Сервер БД */
    @SerialName("server")
    val server: String,

    /** Название БД */
    @SerialName("db_name")
    val dbName: String,

    /** ID абонента */
    @SerialName("idA")
    val idA: Int,

    /** Значение показания */
    @SerialName("val")
    val `val`: String,

    /** Тип прибора */
    @SerialName("idType")
    val idType: String,

    /** Дата и время отправки (ISO 8601 с Z) */
    @SerialName("date")
    val date: String,

    /** Дата начала действия счётчика */
    @SerialName("datB")
    val datB: String
)

/**
 * Запрос InsertCtr - отправка показаний счётчика
 */
@Serializable
data class InsertCtrRequest(
    /** Конфигурация БД провайдера */
    @SerialName("servDb")
    val servDb: KvcLocationDto,

    /** Список счётчиков с показаниями для отправки */
    @SerialName("ctrForInsert")
    val ctrForInsert: List<CounterForInsertDto>,

    /** Комментарий к передаче */
    @SerialName("notes")
    val notes: String = "Передано через приложение",

    /** Категория (0 = обычная передача) */
    @SerialName("category")
    val category: Int = 0
)

/**
 * Запрос GetCtrDays - получение разрешённых дней передачи показаний
 */
@Serializable
data class GetTransmissionPeriodRequestDto(
    @SerialName("servDb")
    val servDb: KvcLocationDto,

    @SerialName("lc")
    val lc: String
)

/**
 * Запрос GetCtrList - получение истории показаний счётчика
 */
@Serializable
data class GetMetersRequestDto(
    @SerialName("servDb")
    val servDb: KvcLocationDto,

    @SerialName("lc")
    val lc: String,

    @SerialName("idCnt")
    val idCnt: Int
)