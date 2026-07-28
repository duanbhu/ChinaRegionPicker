import Foundation

public final class RegionPickerModel {
    public private(set) var selection: RegionSelection
    public private(set) var level: RegionLevel
    private let store: RegionStore

    public init(store: RegionStore, selection: RegionSelection = .init()) {
        self.store = store
        self.selection = selection
        self.level = Self.firstIncompleteLevel(in: selection)
    }

    public func items() throws -> [Region] {
        try store.regions(at: level, parentCode: parentCode(for: level))
    }

    @discardableResult
    public func navigate(to level: RegionLevel) -> Bool {
        guard canNavigate(to: level) else { return false }
        self.level = level
        return true
    }

    @discardableResult
    public func select(_ region: Region) -> RegionSelection? {
        let changed = selection[level]?.code != region.code
        selection[level] = region
        if changed { clearDescendants(after: level) }

        if let next = level.next {
            level = next
            return nil
        }
        return selection.isComplete ? selection : nil
    }

    public func title(for level: RegionLevel) -> String {
        if let name = selection[level]?.name { return name }
        return self.level == level ? level.placeholder : ""
    }

    public func canNavigate(to level: RegionLevel) -> Bool {
        switch level {
        case .province: return true
        case .city: return selection.province != nil
        case .district: return selection.city != nil
        case .street: return selection.district != nil
        }
    }

    private func parentCode(for level: RegionLevel) -> String? {
        switch level {
        case .province: return nil
        case .city: return selection.province?.code
        case .district: return selection.city?.code
        case .street: return selection.district?.code
        }
    }

    private func clearDescendants(after level: RegionLevel) {
        RegionLevel.allCases
            .filter { $0.rawValue > level.rawValue }
            .forEach { selection[$0] = nil }
    }

    private static func firstIncompleteLevel(in selection: RegionSelection) -> RegionLevel {
        RegionLevel.allCases.first { selection[$0] == nil } ?? .street
    }
}
