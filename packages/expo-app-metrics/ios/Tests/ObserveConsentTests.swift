import Foundation
import Testing

@testable import ExpoAppMetrics

@Suite("ObserveConsent")
struct ObserveConsentTests {
  private func appConfig(consent: Any) -> [String: Any] {
    return ["extra": ["eas": ["observe": ["consent": consent]]]]
  }

  @Test
  func `app config without the consent key grants consent`() {
    #expect(ObserveConsent.isGranted(appConfig: nil))
    #expect(ObserveConsent.isGranted(appConfig: ["extra": ["eas": ["projectId": "id"]]]))
  }

  @Test
  func `consent key granted grants consent`() {
    #expect(ObserveConsent.isGranted(appConfig: appConfig(consent: "granted")))
  }

  @Test
  func `consent key not-granted denies consent`() {
    #expect(!ObserveConsent.isGranted(appConfig: appConfig(consent: "not-granted")))
  }

  @Test
  func `unknown consent value denies consent`() {
    #expect(!ObserveConsent.isGranted(appConfig: appConfig(consent: "pending")))
    #expect(!ObserveConsent.isGranted(appConfig: appConfig(consent: false)))
  }
}

@Suite("AppMetrics.deleteStoredData", .serialized)
struct DeleteStoredDataTests {
  @Test
  func `deletes the database and pending-error files and changes the reset token`() throws {
    try withTemporaryDirectory { directory in
      let databaseDirectory = directory.appendingPathComponent("ExpoAppMetrics")
      let pendingErrorsDirectory = directory.appendingPathComponent("pending-errors")
      try FileManager.default.createDirectory(at: databaseDirectory, withIntermediateDirectories: true)
      try FileManager.default.createDirectory(at: pendingErrorsDirectory, withIntermediateDirectories: true)
      let files = [
        databaseDirectory.appendingPathComponent("metrics.db"),
        databaseDirectory.appendingPathComponent("metrics.db-wal"),
        pendingErrorsDirectory.appendingPathComponent("error.json"),
      ]
      for file in files {
        try Data("x".utf8).write(to: file)
      }
      let tokenBefore = AppMetricsUserDefaults.storageResetToken

      AppMetrics.deleteStoredData(databaseDirectory: databaseDirectory, pendingErrorsDirectory: pendingErrorsDirectory)

      for file in files {
        #expect(!FileManager.default.fileExists(atPath: file.path))
      }
      #expect(AppMetricsUserDefaults.storageResetToken != nil)
      #expect(AppMetricsUserDefaults.storageResetToken != tokenBefore)
    }
  }

  @Test
  func `keeps the reset token when there is no database`() throws {
    try withTemporaryDirectory { directory in
      let tokenBefore = AppMetricsUserDefaults.storageResetToken

      AppMetrics.deleteStoredData(databaseDirectory: directory, pendingErrorsDirectory: directory)

      #expect(AppMetricsUserDefaults.storageResetToken == tokenBefore)
    }
  }
}

private func withTemporaryDirectory(_ body: (URL) throws -> Void) throws {
  let directory = FileManager.default.temporaryDirectory
    .appendingPathComponent("ObserveConsentTests-\(UUID().uuidString)")
  try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
  defer {
    try? FileManager.default.removeItem(at: directory)
  }
  try body(directory)
}
