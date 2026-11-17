import Testing
import UIKit

@testable import ExpoRouter

@Suite("PreviewViewController")
@MainActor
struct PreviewViewControllerTests {
  @Test
  func `reopening the preview gives the content view to the new controller`() {
    let content = NativeLinkPreviewContentView(appContext: nil)
    let first = PreviewViewController(linkPreviewNativePreview: content)
    first.loadViewIfNeeded()

    let second = PreviewViewController(linkPreviewNativePreview: content)
    second.loadViewIfNeeded()

    #expect(second.view === content)
    #expect(first.view !== content)
  }
}
