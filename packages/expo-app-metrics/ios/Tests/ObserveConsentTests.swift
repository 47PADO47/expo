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

@Suite("ObserveConsent saved value")
struct ObserveConsentSavedValueTests {
  @Test
  func `no saved value falls back to the app config`() throws {
    try withTemporaryDirectory { directory in
      #expect(ObserveConsent.readSavedState(at: directory.appendingPathComponent("observe-consent")) == nil)
    }
  }

  @Test
  func `the saved value round-trips`() throws {
    try withTemporaryDirectory { directory in
      let url = directory.appendingPathComponent("ExpoAppMetrics/observe-consent")

      #expect(ObserveConsent.save(granted: false, to: url))
      #expect(ObserveConsent.readSavedState(at: url) == .notGranted)

      #expect(ObserveConsent.save(granted: true, to: url))
      #expect(ObserveConsent.readSavedState(at: url) == .granted)
    }
  }

  @Test
  func `a save to an unwritable location fails`() throws {
    try withTemporaryDirectory { directory in
      // A file in place of the parent directory makes the save fail.
      let file = directory.appendingPathComponent("ExpoAppMetrics")
      try Data("x".utf8).write(to: file)

      #expect(!ObserveConsent.save(granted: false, to: file.appendingPathComponent("observe-consent")))
    }
  }

  @Test
  func `an unreadable saved value makes consent unknown`() throws {
    try withTemporaryDirectory { directory in
      // A directory at the path fails the read with an error other than a missing file.
      #expect(ObserveConsent.readSavedState(at: directory) == .unknown)
    }
  }
}

@Suite("ObserveConsent.Gate")
struct ObserveConsentGateTests {
  // Applies each value in order and returns whether each call changed the gate.
  private func apply(_ values: [Bool], to gate: inout ObserveConsent.Gate) -> [Bool] {
    return values.map { gate.apply(granted: $0) }
  }

  @Test
  func `a call with the current value changes nothing`() {
    var granted = ObserveConsent.Gate(state: .granted)
    var notGranted = ObserveConsent.Gate(state: .notGranted)

    #expect(apply([true], to: &granted) == [false])
    #expect(apply([false], to: &notGranted) == [false])
  }

  @Test
  func `a grant after a revoke changes nothing until the next launch`() {
    var gate = ObserveConsent.Gate(state: .notGranted)

    #expect(apply([true, false, true], to: &gate) == [true, true, false])
    #expect(gate.state == .notGranted)
  }

  @Test
  func `an unknown consent changes on either value`() {
    var revoked = ObserveConsent.Gate(state: .unknown)
    var granted = ObserveConsent.Gate(state: .unknown)

    #expect(apply([false], to: &revoked) == [true])
    #expect(revoked.state == .notGranted)
    #expect(apply([true], to: &granted) == [true])
    #expect(granted.state == .granted)
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

      AppMetrics.deleteStoredData(
        consent: .notGranted,
        databaseDirectory: databaseDirectory,
        pendingErrorsDirectory: pendingErrorsDirectory
      )

      for file in files {
        #expect(!FileManager.default.fileExists(atPath: file.path))
      }
      #expect(AppMetricsUserDefaults.storageResetToken != nil)
      #expect(AppMetricsUserDefaults.storageResetToken != tokenBefore)
    }
  }

  @Test
  func `keeps the data when the saved value is unreadable`() throws {
    try withTemporaryDirectory { directory in
      let databaseFile = directory.appendingPathComponent("metrics.db")
      let pendingErrorsDirectory = directory.appendingPathComponent("pending-errors")
      try FileManager.default.createDirectory(at: pendingErrorsDirectory, withIntermediateDirectories: true)
      try Data("x".utf8).write(to: databaseFile)
      let tokenBefore = AppMetricsUserDefaults.storageResetToken
      let consent = try #require(ObserveConsent.readSavedState(at: directory))

      AppMetrics.deleteStoredData(consent: consent, databaseDirectory: directory, pendingErrorsDirectory: pendingErrorsDirectory)

      #expect(FileManager.default.fileExists(atPath: databaseFile.path))
      #expect(FileManager.default.fileExists(atPath: pendingErrorsDirectory.path))
      #expect(AppMetricsUserDefaults.storageResetToken == tokenBefore)
    }
  }

  @Test
  func `keeps the reset token when there is no database`() throws {
    try withTemporaryDirectory { directory in
      let tokenBefore = AppMetricsUserDefaults.storageResetToken

      AppMetrics.deleteStoredData(consent: .notGranted, databaseDirectory: directory, pendingErrorsDirectory: directory)

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
