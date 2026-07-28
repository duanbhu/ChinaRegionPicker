package com.chinaregionpicker.core

class RegionPickerModel(
    private val store: RegionStore,
    selection: RegionSelection = RegionSelection(),
) {
    var selection: RegionSelection = selection
        private set

    var level: RegionLevel = firstIncompleteLevel(selection)
        private set

    fun items(): List<Region> = store.regions(level, parentCode(level))

    fun navigateTo(level: RegionLevel): Boolean {
        if (!canNavigateTo(level)) return false
        this.level = level
        return true
    }

    fun select(region: Region): RegionSelection? {
        val changed = selection[level]?.code != region.code
        selection = when (level) {
            RegionLevel.PROVINCE -> selection.copy(
                province = region,
                city = if (changed) null else selection.city,
                district = if (changed) null else selection.district,
                street = if (changed) null else selection.street,
            )
            RegionLevel.CITY -> selection.copy(
                city = region,
                district = if (changed) null else selection.district,
                street = if (changed) null else selection.street,
            )
            RegionLevel.DISTRICT -> selection.copy(
                district = region,
                street = if (changed) null else selection.street,
            )
            RegionLevel.STREET -> selection.copy(street = region)
        }

        val next = level.next
        if (next != null) {
            level = next
            return null
        }
        return selection.takeIf { it.isComplete }
    }

    fun title(level: RegionLevel): String {
        return selection[level]?.name ?: if (this.level == level) "请选择" else ""
    }

    fun canNavigateTo(level: RegionLevel): Boolean = when (level) {
        RegionLevel.PROVINCE -> true
        RegionLevel.CITY -> selection.province != null
        RegionLevel.DISTRICT -> selection.city != null
        RegionLevel.STREET -> selection.district != null
    }

    private fun parentCode(level: RegionLevel): String? = when (level) {
        RegionLevel.PROVINCE -> null
        RegionLevel.CITY -> selection.province?.code
        RegionLevel.DISTRICT -> selection.city?.code
        RegionLevel.STREET -> selection.district?.code
    }

    private companion object {
        fun firstIncompleteLevel(selection: RegionSelection): RegionLevel {
            return RegionLevel.entries.firstOrNull { selection[it] == null } ?: RegionLevel.STREET
        }
    }
}
