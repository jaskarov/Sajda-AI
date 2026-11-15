package com.example.domain.engine

import com.example.domain.model.UzbekCity
import com.example.domain.model.lText
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

object UzbekistanCities {
    const val MAKKAH_LAT = 21.4225
    const val MAKKAH_LON = 39.8262

    val all: List<UzbekCity> = listOf(
        UzbekCity(
            id = "fargona",
            name = lText("Fargʻona", "Фарғона"),
            region = lText("Fargʻona viloyati", "Фарғона вилояти"),
            latitude = 40.3864,
            longitude = 71.7864
        ),
        UzbekCity(
            id = "toshkent",
            name = lText("Toshkent", "Тошкент"),
            region = lText("Toshkent shahri", "Тошкент шаҳри"),
            latitude = 41.2995,
            longitude = 69.2401
        ),
        UzbekCity(
            id = "andijon",
            name = lText("Andijon", "Андижон"),
            region = lText("Andijon viloyati", "Андижон вилояти"),
            latitude = 40.7821,
            longitude = 72.3442
        ),
        UzbekCity(
            id = "namangan",
            name = lText("Namangan", "Наманган"),
            region = lText("Namangan viloyati", "Наманган вилояти"),
            latitude = 41.0011,
            longitude = 71.6683
        ),
        UzbekCity(
            id = "qoqon",
            name = lText("Qoʻqon", "Қўқон"),
            region = lText("Fargʻona viloyati", "Фарғона вилояти"),
            latitude = 40.5286,
            longitude = 70.9425
        ),
        UzbekCity(
            id = "samarqand",
            name = lText("Samarqand", "Самарқанд"),
            region = lText("Samarqand viloyati", "Самарқанд вилояти"),
            latitude = 39.6542,
            longitude = 66.9597
        ),
        UzbekCity(
            id = "buxoro",
            name = lText("Buxoro", "Бухоро"),
            region = lText("Buxoro viloyati", "Бухоро вилояти"),
            latitude = 39.7747,
            longitude = 64.4286
        ),
        UzbekCity(
            id = "qarshi",
            name = lText("Qarshi", "Қарши"),
            region = lText("Qashqadaryo viloyati", "Қашқадарё вилояти"),
            latitude = 38.8606,
            longitude = 65.7891
        ),
        UzbekCity(
            id = "termiz",
            name = lText("Termiz", "Термиз"),
            region = lText("Surxondaryo viloyati", "Сурхондарё вилояти"),
            latitude = 37.2242,
            longitude = 67.2783
        ),
        UzbekCity(
            id = "navoiy",
            name = lText("Navoiy", "Навоий"),
            region = lText("Navoiy viloyati", "Навоий вилояти"),
            latitude = 40.1039,
            longitude = 65.3688
        ),
        UzbekCity(
            id = "jizzax",
            name = lText("Jizzax", "Жиззах"),
            region = lText("Jizzax viloyati", "Жиззах вилояти"),
            latitude = 40.1158,
            longitude = 67.8422
        ),
        UzbekCity(
            id = "guliston",
            name = lText("Guliston", "Гулистон"),
            region = lText("Sirdaryo viloyati", "Сирдарё вилояти"),
            latitude = 40.4897,
            longitude = 68.7842
        ),
        UzbekCity(
            id = "urganch",
            name = lText("Urganch / Xiva", "Урганч / Хива"),
            region = lText("Xorazm viloyati", "Хоразм вилояти"),
            latitude = 41.5500,
            longitude = 60.6333
        ),
        UzbekCity(
            id = "nukus",
            name = lText("Nukus", "Нукус"),
            region = lText("Qoraqalpogʻiston", "Қорақалпоғистон"),
            latitude = 42.4531,
            longitude = 59.6103
        )
    )

    fun findById(id: String?): UzbekCity =
        all.firstOrNull { it.id == id } ?: all.first()

    fun findNearest(lat: Double, lon: Double): UzbekCity {
        return all.minByOrNull { city ->
            val dLat = city.latitude - lat
            val dLon = city.longitude - lon
            dLat * dLat + dLon * dLon
        } ?: all.first()
    }

    fun calculateQiblaBearing(latitude: Double, longitude: Double): Double {
        val lat1 = Math.toRadians(latitude)
        val lon1 = Math.toRadians(longitude)
        val lat2 = Math.toRadians(MAKKAH_LAT)
        val lon2 = Math.toRadians(MAKKAH_LON)
        val dLon = lon2 - lon1
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        val bearing = Math.toDegrees(atan2(y, x))
        return (bearing + 360.0) % 360.0
    }
}
