import CSQLite
import Foundation

public final class SQLiteRegionStore: RegionStore {
    private let databaseURL: URL

    public init(databaseURL: URL) {
        self.databaseURL = databaseURL
    }

    public static func bundled() throws -> SQLiteRegionStore {
        guard let url = Bundle.module.url(
            forResource: "province",
            withExtension: "sqlite",
            subdirectory: "Resources"
        ) else {
            throw RegionStoreError.databaseNotFound
        }
        return SQLiteRegionStore(databaseURL: url)
    }

    public func regions(at level: RegionLevel, parentCode: String?) throws -> [Region] {
        let query: String
        switch level {
        case .province:
            query = "SELECT code, name FROM province ORDER BY rowid"
        case .city:
            query = "SELECT code, name FROM city WHERE provinceCode = ? ORDER BY rowid"
        case .district:
            query = "SELECT code, name FROM area WHERE cityCode = ? ORDER BY rowid"
        case .street:
            query = "SELECT code, name FROM street WHERE areaCode = ? ORDER BY rowid"
        }

        if level != .province, parentCode == nil {
            throw RegionStoreError.parentRequired(level)
        }
        return try execute(query, parameter: parentCode)
    }

    private func execute(_ sql: String, parameter: String?) throws -> [Region] {
        var database: OpaquePointer?
        let openResult = sqlite3_open_v2(databaseURL.path, &database, SQLITE_OPEN_READONLY, nil)
        guard openResult == SQLITE_OK, let database else {
            let message = database.map { String(cString: sqlite3_errmsg($0)) } ?? "无法打开数据库"
            if let database { sqlite3_close(database) }
            throw RegionStoreError.database(message)
        }
        defer { sqlite3_close(database) }

        var statement: OpaquePointer?
        guard sqlite3_prepare_v2(database, sql, -1, &statement, nil) == SQLITE_OK,
              let statement else {
            throw RegionStoreError.database(String(cString: sqlite3_errmsg(database)))
        }
        defer { sqlite3_finalize(statement) }

        if let parameter {
            guard sqlite3_bind_text(statement, 1, parameter, -1, SQLITE_TRANSIENT) == SQLITE_OK else {
                throw RegionStoreError.database(String(cString: sqlite3_errmsg(database)))
            }
        }

        var result: [Region] = []
        while true {
            switch sqlite3_step(statement) {
            case SQLITE_ROW:
                guard let codeText = sqlite3_column_text(statement, 0),
                      let nameText = sqlite3_column_text(statement, 1) else { continue }
                result.append(Region(
                    code: String(cString: codeText),
                    name: String(cString: nameText)
                ))
            case SQLITE_DONE:
                return result
            default:
                throw RegionStoreError.database(String(cString: sqlite3_errmsg(database)))
            }
        }
    }
}

private let SQLITE_TRANSIENT = unsafeBitCast(-1, to: sqlite3_destructor_type.self)
