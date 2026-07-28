import Foundation

public protocol RegionStore {
    func regions(at level: RegionLevel, parentCode: String?) throws -> [Region]
}

public enum RegionStoreError: LocalizedError, Equatable {
    case databaseNotFound
    case parentRequired(RegionLevel)
    case database(String)

    public var errorDescription: String? {
        switch self {
        case .databaseNotFound:
            return "找不到行政区划数据库"
        case let .parentRequired(level):
            return "查询\(level.title)时缺少上级行政区编码"
        case let .database(message):
            return "数据库错误：\(message)"
        }
    }
}
