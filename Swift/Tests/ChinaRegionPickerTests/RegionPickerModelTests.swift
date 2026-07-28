import XCTest
@testable import ChinaRegionPicker

final class RegionPickerModelTests: XCTestCase {
    func testSelectsFourLevelsAndReturnsCompleteSelection() throws {
        let model = RegionPickerModel(store: MockStore())

        XCTAssertEqual(try model.items(), [.init(code: "11", name: "北京市")])
        XCTAssertNil(model.select(.init(code: "11", name: "北京市")))
        XCTAssertEqual(model.level, .city)
        XCTAssertNil(model.select(.init(code: "1101", name: "市辖区")))
        XCTAssertNil(model.select(.init(code: "110101", name: "东城区")))
        let result = model.select(.init(code: "110101001", name: "东华门街道"))

        XCTAssertTrue(result?.isComplete == true)
        XCTAssertEqual(result?.displayName, "北京市市辖区东城区东华门街道")
    }

    func testChangingParentClearsDescendants() {
        let initial = RegionSelection(
            province: .init(code: "11", name: "北京市"),
            city: .init(code: "1101", name: "市辖区"),
            district: .init(code: "110101", name: "东城区"),
            street: .init(code: "110101001", name: "东华门街道")
        )
        let model = RegionPickerModel(store: MockStore(), selection: initial)
        XCTAssertTrue(model.navigate(to: .province))

        model.select(.init(code: "12", name: "天津市"))

        XCTAssertEqual(model.selection.province?.code, "12")
        XCTAssertNil(model.selection.city)
        XCTAssertNil(model.selection.district)
        XCTAssertNil(model.selection.street)
    }

    func testCannotNavigateWithoutParent() {
        let model = RegionPickerModel(store: MockStore())
        XCTAssertFalse(model.navigate(to: .district))
        XCTAssertEqual(model.level, .province)
    }

    func testBundledSQLiteStoreLoadsRegionHierarchy() throws {
        let store = try SQLiteRegionStore.bundled()
        let provinces = try store.regions(at: .province, parentCode: nil)
        let cities = try store.regions(at: .city, parentCode: "11")
        let districts = try store.regions(at: .district, parentCode: "1101")
        let streets = try store.regions(at: .street, parentCode: "110101")

        XCTAssertEqual(provinces.first, .init(code: "11", name: "北京市"))
        XCTAssertEqual(cities.first, .init(code: "1101", name: "市辖区"))
        XCTAssertEqual(districts.first, .init(code: "110101", name: "东城区"))
        XCTAssertEqual(streets.first, .init(code: "110101001", name: "东华门街道"))
    }
}

private struct MockStore: RegionStore {
    func regions(at level: RegionLevel, parentCode: String?) throws -> [Region] {
        [.init(code: "11", name: "北京市")]
    }
}
