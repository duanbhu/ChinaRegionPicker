package com.chinaregionpicker.core

enum class RegionLevel(val title: String) {
    PROVINCE("省份"),
    CITY("城市"),
    DISTRICT("区县"),
    STREET("街道");

    val next: RegionLevel?
        get() = entries.getOrNull(ordinal + 1)
}

data class Region(
    val code: String,
    val name: String,
)

data class RegionSelection(
    val province: Region? = null,
    val city: Region? = null,
    val district: Region? = null,
    val street: Region? = null,
) {
    val isComplete: Boolean
        get() = province != null && city != null && district != null && street != null

    val displayName: String
        get() = listOfNotNull(province, city, district, street).joinToString("") { it.name }

    operator fun get(level: RegionLevel): Region? = when (level) {
        RegionLevel.PROVINCE -> province
        RegionLevel.CITY -> city
        RegionLevel.DISTRICT -> district
        RegionLevel.STREET -> street
    }
}
