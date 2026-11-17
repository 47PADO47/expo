package expo.modules.appmetrics

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import expo.modules.appmetrics.crashreporting.CrashFileFormat
import io.mockk.mockk
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [28])
class ObserveConsentTest {
  private val context: Context = ApplicationProvider.getApplicationContext()

  @Before
  fun setUp() {
    context.getSharedPreferences("dev.expo.app-metrics", Context.MODE_PRIVATE).edit().clear().commit()
  }

  // Restores the default gate for the next test class.
  @After
  fun tearDown() {
    setUp()
    ObserveConsent.load(context)
  }

  private fun appConfig(consent: String) = """{"extra":{"eas":{"observe":{"consent":$consent}}}}"""

  private fun savedConsent(): String? =
    context.getSharedPreferences("dev.expo.app-metrics", Context.MODE_PRIVATE).getString("observeConsent", null)

  @Test
  fun `app config without the consent key grants consent`() {
    assertTrue(ObserveConsent.resolve(null, null))
    assertTrue(ObserveConsent.resolve(null, """{"extra":{"eas":{"projectId":"id"}}}"""))
    assertTrue(ObserveConsent.resolve(null, "not json"))
  }

  @Test
  fun `consent key granted grants consent`() {
    assertTrue(ObserveConsent.resolve(null, appConfig("\"granted\"")))
  }

  @Test
  fun `consent key not-granted denies consent`() {
    assertFalse(ObserveConsent.resolve(null, appConfig("\"not-granted\"")))
  }

  @Test
  fun `unknown consent value denies consent`() {
    assertFalse(ObserveConsent.resolve(null, appConfig("\"pending\"")))
    assertFalse(ObserveConsent.resolve(null, appConfig("false")))
  }

  @Test
  fun `saved value overrides the app config`() {
    assertFalse(ObserveConsent.resolve("not-granted", appConfig("\"granted\"")))
    assertTrue(ObserveConsent.resolve("granted", appConfig("\"not-granted\"")))
    assertFalse(ObserveConsent.resolve("not-granted", null))
  }

  @Test
  fun `load reads the saved value`() {
    ObserveConsent.update(context, granted = false)

    ObserveConsent.load(context)

    assertFalse(ObserveConsent.isGranted)
  }

  @Test
  fun `update saves the value and changes the gate`() {
    ObserveConsent.load(context)

    assertFalse(ObserveConsent.update(context, granted = true).changed)
    assertEquals("granted", savedConsent())
    assertTrue(ObserveConsent.isGranted)

    assertTrue(ObserveConsent.update(context, granted = false).changed)
    assertEquals("not-granted", savedConsent())
    assertFalse(ObserveConsent.isGranted)

    assertFalse(ObserveConsent.update(context, granted = false).changed)
  }

  @Test
  fun `a grant after a revoke only saves the value until the next launch`() {
    ObserveConsent.update(context, granted = false)
    ObserveConsent.load(context)

    assertTrue(ObserveConsent.update(context, granted = true).changed)
    assertTrue(ObserveConsent.update(context, granted = false).changed)
    assertFalse(ObserveConsent.update(context, granted = true).changed)
    assertEquals("granted", savedConsent())
    assertFalse(ObserveConsent.isGranted)

    ObserveConsent.load(context)
    assertTrue(ObserveConsent.isGranted)
  }

  @Test
  fun `a failed save still revokes, and a grant changes nothing`() {
    // A relaxed mock returns `false` from `commit()`.
    val unsaved = mockk<Context>(relaxed = true)
    ObserveConsent.load(context)

    assertEquals(ObserveConsent.Update(changed = true, isSaved = false), ObserveConsent.update(unsaved, granted = false))
    assertFalse(ObserveConsent.isGranted)

    ObserveConsent.update(context, granted = false)
    ObserveConsent.load(context)
    assertEquals(ObserveConsent.Update(changed = false, isSaved = false), ObserveConsent.update(unsaved, granted = true))
    assertFalse(ObserveConsent.isGranted)
  }

  @Test
  fun `deleteStoredData deletes the database, crash files, and pending errors and changes the reset token`() {
    val database = context.getDatabasePath("app_metrics").apply {
      parentFile?.mkdirs()
      writeText("db")
    }
    val crashFile = File(CrashFileFormat.crashesDir(context), "crash-1-1.json").apply {
      parentFile?.mkdirs()
      writeText("{}")
    }
    val pendingError = File(context.noBackupFilesDir, "ExpoAppMetrics/pending-errors/error.json").apply {
      parentFile?.mkdirs()
      writeText("{}")
    }

    deleteStoredData(context)

    assertFalse(database.exists())
    assertFalse(crashFile.exists())
    assertFalse(pendingError.exists())
    val token = AppMetricsPreferences.getStorageResetToken(context)
    assertNotEquals(null, token)

    // A second launch without a database keeps the token.
    deleteStoredData(context)
    assertEquals(token, AppMetricsPreferences.getStorageResetToken(context))
  }

  @Test
  fun `deleteStoredData without a database keeps the reset token`() {
    deleteStoredData(context)

    assertNull(AppMetricsPreferences.getStorageResetToken(context))
  }
}
