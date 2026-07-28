package com.chinaregionpicker.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RegionPickerModelTest {
    @Test
    fun selectsFourLevelsAndReturnsCompleteSelection() {
        val model = RegionPickerModel(MockStore)

        assertNull(model.select(Region("11", "北京市")))
        assertNull(model.select(Region("1101", "市辖区")))
        assertNull(model.select(Region("110101", "东城区")))
        val result = model.select(Region("110101001", "东华门街道"))

        assertTrue(result?.isComplete == true)
        assertEquals("北京市市辖区东城区东华门街道", result.displayName)
    }

    @Test
    fun changingParentClearsDescendants() {
        val initial = RegionSelection(
            province = Region("11", "北京市"),
            city = Region("1101", "市辖区"),
            district = Region("110101", "东城区"),
            street = Region("110101001", "东华门街道"),
        )
        val model = RegionPickerModel(MockStore, initial)
        assertTrue(model.navigateTo(RegionLevel.PROVINCE))

        model.select(Region("12", "天津市"))

        assertEquals("12", model.selection.province?.code)
        assertNull(model.selection.city)
        assertNull(model.selection.district)
        assertNull(model.selection.street)
    }

    @Test
    fun cannotNavigateWithoutParent() {
        val model = RegionPickerModel(MockStore)
        assertFalse(model.navigateTo(RegionLevel.DISTRICT))
        assertEquals(RegionLevel.PROVINCE, model.level)
    }

    private object MockStore : RegionStore {
        override fun regions(level: RegionLevel, parentCode: String?): List<Region> {
            return listOf(Region("11", "北京市"))
        }
    }
}
