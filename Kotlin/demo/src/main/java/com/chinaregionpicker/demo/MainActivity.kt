package com.chinaregionpicker.demo

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.chinaregionpicker.data.SQLiteRegionStore
import com.chinaregionpicker.ui.ChinaRegionPickerDialog

class MainActivity : Activity() {
    private lateinit var store: SQLiteRegionStore
    private lateinit var resultView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = SQLiteRegionStore.fromAssets(this)

        resultView = TextView(this).apply {
            text = "尚未选择地址"
            textSize = 18f
            setPadding(24, 24, 24, 24)
        }
        val openButton = Button(this).apply {
            text = "选择所在地址"
            setOnClickListener { showPicker() }
        }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(32, 80, 32, 32)
            addView(resultView)
            addView(openButton)
        })
    }

    private fun showPicker() {
        ChinaRegionPickerDialog(this, store) { selection ->
            resultView.text = "已选择：${selection.displayName}\n\n编码：${selection.street?.code}"
        }.show()
    }

    override fun onDestroy() {
        store.close()
        super.onDestroy()
    }
}
