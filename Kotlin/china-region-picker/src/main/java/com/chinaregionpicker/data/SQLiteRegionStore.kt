package com.chinaregionpicker.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.chinaregionpicker.core.Region
import com.chinaregionpicker.core.RegionLevel
import com.chinaregionpicker.core.RegionStore
import java.io.Closeable
import java.io.File

class SQLiteRegionStore private constructor(
    private val database: SQLiteDatabase,
) : RegionStore, Closeable {

    override fun regions(level: RegionLevel, parentCode: String?): List<Region> {
        val (table, parentColumn) = when (level) {
            RegionLevel.PROVINCE -> "province" to null
            RegionLevel.CITY -> "city" to "provinceCode"
            RegionLevel.DISTRICT -> "area" to "cityCode"
            RegionLevel.STREET -> "street" to "areaCode"
        }
        require(parentColumn == null || parentCode != null) {
            "查询${level.title}时缺少上级行政区编码"
        }

        val selection = parentColumn?.let { "$it = ?" }
        val selectionArgs = parentCode?.let { arrayOf(it) }
        return database.query(
            table,
            arrayOf("code", "name"),
            selection,
            selectionArgs,
            null,
            null,
            "rowid",
        ).use { cursor ->
            buildList {
                val codeIndex = cursor.getColumnIndexOrThrow("code")
                val nameIndex = cursor.getColumnIndexOrThrow("name")
                while (cursor.moveToNext()) {
                    add(Region(cursor.getString(codeIndex), cursor.getString(nameIndex)))
                }
            }
        }
    }

    override fun close() {
        database.close()
    }

    companion object {
        private const val DEFAULT_ASSET_NAME = "province.sqlite"

        fun fromAssets(
            context: Context,
            assetName: String = DEFAULT_ASSET_NAME,
        ): SQLiteRegionStore {
            val appContext = context.applicationContext
            val directory = File(appContext.noBackupFilesDir, "china-region-picker").apply { mkdirs() }
            val databaseFile = File(directory, assetName)
            if (!databaseFile.exists()) {
                val temporaryFile = File(directory, "$assetName.tmp")
                appContext.assets.open(assetName).use { input ->
                    temporaryFile.outputStream().use(input::copyTo)
                }
                check(temporaryFile.renameTo(databaseFile)) { "无法安装行政区划数据库" }
            }
            val database = SQLiteDatabase.openDatabase(
                databaseFile.path,
                null,
                SQLiteDatabase.OPEN_READONLY,
            )
            return SQLiteRegionStore(database)
        }
    }
}
