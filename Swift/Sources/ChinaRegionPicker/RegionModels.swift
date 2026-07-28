import Foundation

public enum RegionLevel: Int, CaseIterable, Codable, Sendable {
    case province
    case city
    case district
    case street

    public var placeholder: String { "请选择" }

    public var title: String {
        switch self {
        case .province: return "省份"
        case .city: return "城市"
        case .district: return "区县"
        case .street: return "街道"
        }
    }

    var next: RegionLevel? { RegionLevel(rawValue: rawValue + 1) }
}

public struct Region: Codable, Equatable, Hashable, Sendable {
    public let code: String
    public let name: String

    public init(code: String, name: String) {
        self.code = code
        self.name = name
    }
}

public struct RegionSelection: Codable, Equatable, Sendable {
    public var province: Region?
    public var city: Region?
    public var district: Region?
    public var street: Region?

    public init(
        province: Region? = nil,
        city: Region? = nil,
        district: Region? = nil,
        street: Region? = nil
    ) {
        self.province = province
        self.city = city
        self.district = district
        self.street = street
    }

    public var isComplete: Bool {
        province != nil && city != nil && district != nil && street != nil
    }

    public var displayName: String {
        [province, city, district, street].compactMap { $0?.name }.joined()
    }

    public subscript(level: RegionLevel) -> Region? {
        get {
            switch level {
            case .province: return province
            case .city: return city
            case .district: return district
            case .street: return street
            }
        }
        set {
            switch level {
            case .province: province = newValue
            case .city: city = newValue
            case .district: district = newValue
            case .street: street = newValue
            }
        }
    }
}
