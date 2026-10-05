package com.chinaregionpicker.ui

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.AbsListView
import android.widget.BaseAdapter
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import com.chinaregionpicker.R
import com.chinaregionpicker.core.Region
import com.chinaregionpicker.core.RegionLevel
import com.chinaregionpicker.core.RegionPickerModel
import com.chinaregionpicker.core.RegionSelection
import com.chinaregionpicker.core.RegionStore
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

class ChinaRegionPickerDialog(
    context: Context,
    store: RegionStore,
    selection: RegionSelection = RegionSelection(),
    private val onSelected: (RegionSelection) -> Unit,
) : Dialog(context) {
    private val model = RegionPickerModel(store, selection)
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val requestVersion = AtomicInteger()

    private lateinit var tabContainer: LinearLayout
    private lateinit var listView: ListView
    private lateinit var adapter: RegionAdapter
    private lateinit var emptyTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(createContentView())
        reload()
    }

    override fun show() {
        super.show()
        window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, screenHeight() * 7 / 10)
            setGravity(Gravity.BOTTOM)
            attributes = attributes.apply { dimAmount = 0.45f }
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }
    }

    override fun dismiss() {
        requestVersion.incrementAndGet()
        executor.shutdownNow()
        super.dismiss()
    }

    private fun createContentView(): View {
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(ColorState.WHITE)
                cornerRadius = dp(10).toFloat()
            }
        }

        val header = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48))
        }
        header.addView(TextView(context).apply {
            text = "请选择所在地址"
            textSize = 18f
            gravity = Gravity.CENTER
            setTypeface(typeface, Typeface.BOLD)
        }, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        header.addView(ImageButton(context).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            imageTintList = ColorStateList.valueOf(ColorState.TEXT)
            contentDescription = "关闭"
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { dismiss() }
        }, FrameLayout.LayoutParams(dp(48), dp(48), Gravity.END))
        content.addView(header)

        tabContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), 0, dp(16), 0)
        }
        content.addView(HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            addView(tabContainer)
        }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(44)))

        emptyTextView = TextView(context).apply {
            text = "加载中..."
            textSize = 16f
            gravity = Gravity.CENTER
        }
        adapter = RegionAdapter()
        listView = ListView(context).apply {
            dividerHeight = 0
            this.adapter = this@ChinaRegionPickerDialog.adapter
            setOnItemClickListener { _, _, position, _ ->
                val result = model.select(this@ChinaRegionPickerDialog.adapter.getItem(position))
                if (result != null) {
                    onSelected(result)
                    dismiss()
                } else {
                    reload()
                }
            }
        }
        val listContainer = FrameLayout(context)
        listContainer.addView(
            listView,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
        listContainer.addView(
            emptyTextView,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
        )
        content.addView(
            listContainer,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f),
        )
        return content
    }

    private fun updateTabs() {
        tabContainer.removeAllViews()
        RegionLevel.entries.forEach { level ->
            val title = model.title(level)
            if (title.isNotEmpty()) {
                tabContainer.addView(TextView(context).apply {
                    text = title
                    textSize = 16f
                    gravity = Gravity.CENTER
                    setTextColor(if (model.level == level) ColorState.RED else ColorState.TEXT)
                    setTypeface(typeface, Typeface.BOLD)
                    isEnabled = model.canNavigateTo(level)
                    setPadding(0, 0, dp(18), 0)
                    setOnClickListener {
                        if (model.navigateTo(level)) reload()
                    }
                }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT))
            }
        }
    }

    private fun reload() {
        updateTabs()
        emptyTextView.text = "加载中..."
        emptyTextView.visibility = View.VISIBLE
        adapter.submit(emptyList(), null)
        val version = requestVersion.incrementAndGet()
        executor.execute {
            runCatching { model.items() }
                .onSuccess { regions ->
                    mainHandler.post {
                        if (version == requestVersion.get()) {
                            if (regions.isEmpty()) {
                                emptyTextView.text = "暂无数据"
                                emptyTextView.visibility = View.VISIBLE
                            } else {
                                emptyTextView.visibility = View.GONE
                            }
                            adapter.submit(regions, model.selection[model.level]?.code)
                            scrollToSelected(regions)
                        }
                    }
                }
                .onFailure { error ->
                    mainHandler.post {
                        if (version == requestVersion.get()) {
                            emptyTextView.text = "加载失败"
                            emptyTextView.visibility = View.VISIBLE
                            Toast.makeText(context, error.message ?: "行政区划加载失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
        }
    }

    private fun scrollToSelected(items: List<Region>) {
        val code = model.selection[model.level]?.code ?: return
        val position = items.indexOfFirst { it.code == code }
        if (position >= 0) listView.setSelection(position)
    }

    private fun dp(value: Int): Int = (value * context.resources.displayMetrics.density).toInt()

    private fun screenHeight(): Int = context.resources.displayMetrics.heightPixels

    private inner class RegionAdapter : BaseAdapter() {
        private var items: List<Region> = emptyList()
        private var selectedCode: String? = null

        fun submit(items: List<Region>, selectedCode: String?) {
            this.items = items
            this.selectedCode = selectedCode
            notifyDataSetChanged()
        }

        override fun getCount(): Int = items.size
        override fun getItem(position: Int): Region = items[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val item = getItem(position)
            val selected = item.code == selectedCode
            return (convertView as? TextView ?: TextView(context).apply {
                textSize = 16f
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(20), 0, dp(20), 0)
                layoutParams = AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48))
                compoundDrawablePadding = dp(8)
            }).apply {
                text = item.name
                setTypeface(Typeface.DEFAULT, if (selected) Typeface.BOLD else Typeface.NORMAL)
                setCompoundDrawablesWithIntrinsicBounds(
                    if (selected) R.drawable.icon_pcat_city_right else 0,
                    0,
                    0,
                    0,
                )
            }
        }
    }

    private object ColorState {
        const val WHITE = 0xFFFFFFFF.toInt()
        const val RED = 0xFFE53935.toInt()
        const val TEXT = 0xFF202124.toInt()
    }
}
