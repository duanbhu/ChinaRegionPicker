package com.chinaregionpicker.core

interface RegionStore {
    fun regions(level: RegionLevel, parentCode: String? = null): List<Region>
}
