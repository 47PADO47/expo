package expo.modules.appmetrics

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import expo.modules.appmetrics.crashreporting.CrashFileFormat
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

  private fun appConfig(consent: String) = """{"extra":{"eas":{"observe":{"consent":$consent}}}}"""

  @Test
  fun `app config without the consent key grants consent`() {
    assertTrue(ObserveConsent.resolve(null))
    assertTrue(ObserveConsent.resolve("""{"extra":{"eas":{"projectId":"id"}}}"""))
    assertTrue(ObserveConsent.resolve("not json"))
  }

  @Test
  fun `consent key granted grants consent`() {
    assertTrue(ObserveConsent.resolve(appConfig("\"granted\"")))
  }

  @Test
  fun `consent key not-granted denies consent`() {
    assertFalse(ObserveConsent.resolve(appConfig("\"not-granted\"")))
  }

  @Test
  fun `unknown consent value denies consent`() {
    assertFalse(ObserveConsent.resolve(appConfig("\"pending\"")))
    assertFalse(ObserveConsent.resolve(appConfig("false")))
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
