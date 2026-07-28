# ChinaRegionPicker

中国省、市、区县、街道四级级联选择器，分别提供 Swift/UIKit 与 Kotlin/Android 原生实现。

## 功能

- 省、市、区县、街道四级级联
- 支持传入已有地址并回显
- 修改任意上级后自动清空其下级选择
- 选择街道后返回名称与行政区编码
- 选中项使用参考工程的 `icon_pcat_city_right` 图标
- 内置 SQLite 数据，也可通过 `RegionStore` 接入自有数据源
- Swift 版无第三方依赖，支持 iOS 13+
- Kotlin 版无运行时第三方依赖，支持 Android API 21+

## Swift

在 Xcode 中选择 **File > Add Package Dependencies**，输入：

```text
https://github.com/duanbhu/ChinaRegionPicker.git
```

依赖规则选择 **Up to Next Major Version**，最低版本填写 `0.1.0`。选择 `ChinaRegionPicker` 产品并添加到 App Target，然后：

```swift
import ChinaRegionPicker

let store = try SQLiteRegionStore.bundled()
ChinaRegionPickerViewController.present(from: self, store: store) { selection in
    print(selection.displayName)
    print(selection.province?.code ?? "")
    print(selection.city?.code ?? "")
    print(selection.district?.code ?? "")
    print(selection.street?.code ?? "")
}
```

回显已有选择：

```swift
let selection = RegionSelection(
    province: Region(code: "11", name: "北京市"),
    city: Region(code: "1101", name: "市辖区"),
    district: Region(code: "110101", name: "东城区"),
    street: Region(code: "110101001", name: "东华门街道")
)

ChinaRegionPickerViewController.present(
    from: self,
    store: try SQLiteRegionStore.bundled(),
    selection: selection
) { result in
    print(result.displayName)
}
```

## Kotlin

将 `Kotlin/china-region-picker` 模块引入 Android 工程，然后：

```kotlin
import com.chinaregionpicker.data.SQLiteRegionStore
import com.chinaregionpicker.ui.ChinaRegionPickerDialog

val store = SQLiteRegionStore.fromAssets(this)
ChinaRegionPickerDialog(this, store) { selection ->
    println(selection.displayName)
    println(selection.province?.code.orEmpty())
    println(selection.city?.code.orEmpty())
    println(selection.district?.code.orEmpty())
    println(selection.street?.code.orEmpty())
}.show()
```

`SQLiteRegionStore` 实现了 `Closeable`。页面不再使用它时应调用 `close()`；如果它由依赖注入容器统一管理，则在对应作用域结束时关闭。

## 自定义数据源

两个平台都公开了 `RegionStore`。自定义实现只需根据当前层级和上级编码返回直接下级：

| 层级 | 上级编码 | SQLite 表 |
| --- | --- | --- |
| 省份 | 无 | `province` |
| 城市 | 省份编码 | `city.provinceCode` |
| 区县 | 城市编码 | `area.cityCode` |
| 街道 | 区县编码 | `street.areaCode` |

## 内置数据

当前数据库来自参考工程的 `province.sqlite` 快照，包含 31 个省级、342 个市级、2,990 个区县级和 41,356 个街道级记录。行政区划会调整，生产使用前应确认数据日期与业务要求；更新时需同时替换两个平台资源目录中的数据库。

## 验证

```bash
swift test

cd Kotlin
./gradlew :china-region-picker:testDebugUnitTest
```
