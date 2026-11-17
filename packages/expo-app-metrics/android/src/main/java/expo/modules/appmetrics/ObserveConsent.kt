package expo.modules.appmetrics

import android.content.Context
import android.util.Log
import expo.modules.appmetrics.crashreporting.CrashFileFormat
import expo.modules.appmetrics.jserrors.PendingErrorStore
import expo.modules.appmetrics.storage.MetricsDatabase
import java.io.FileNotFoundException
import java.util.UUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Whether the user consented to Observe collecting data. While not granted, app metrics stores
 * nothing and Observe sends nothing.
 */
object ObserveConsent {
  // Granted until the application start hook calls `load`.
  @Volatile
  var isGranted: Boolean = true
    private set

  // After a revoke, a grant in the same process only saves the value.
  private var hasRevoked = false

  /** Reads the saved consent value, or the app config embedded in the app when there is none. */
  @Synchronized
  fun load(context: Context) {
    isGranted = resolve(AppMetricsPreferences.getObserveConsent(context), readEmbeddedAppConfig(context))
    hasRevoked = false
  }

  internal data class Update(val changed: Boolean, val isSaved: Boolean)

  /**
   * Saves the value and changes the gate. A grant after a revoke changes it only at the next launch.
   * When the save fails, a revoke still closes the gate, but a grant changes nothing: the next
   * launch would read the earlier value.
   */
  @Synchronized
  internal fun update(context: Context, granted: Boolean): Update {
    val isSaved = AppMetricsPreferences.setObserveConsent(context, if (granted) GRANTED else NOT_GRANTED)
    if (granted == isGranted || (granted && (hasRevoked || !isSaved))) {
      return Update(changed = false, isSaved = isSaved)
    }
    isGranted = granted
    hasRevoked = hasRevoked || !granted
    return Update(changed = true, isSaved = isSaved)
  }

  /**
   * Consent from the saved value, else from the `extra.eas.observe.consent` key of the app config
   * JSON, granted when both are missing.
   */
  internal fun resolve(savedConsent: String?, appConfig: String?): Boolean {
    if (savedConsent != null) {
      return savedConsent == GRANTED
    }
    val consent = (appConfig?.let { runCatching { Json.parseToJsonElement(it) }.getOrNull() } as? JsonObject)
      ?.let { it["extra"] as? JsonObject }
      ?.let { it["eas"] as? JsonObject }
      ?.let { it["observe"] as? JsonObject }
      ?.get("consent")
      ?: return true
    val value = (consent as? JsonPrimitive)?.takeIf { it.isString }?.content
    if (value != GRANTED && value != NOT_GRANTED) {
      Log.w(
        TAG,
        "[AppMetrics] The app config value `extra.eas.observe.consent` is $consent, which is not " +
          "'granted' or 'not-granted', so Observe stays inactive. Set it to 'granted' or 'not-granted'."
      )
    }
    return value == GRANTED
  }

  private const val GRANTED = "granted"
  private const val NOT_GRANTED = "not-granted"

  private fun readEmbeddedAppConfig(context: Context): String? =
    try {
      context.assets.open("app.config").bufferedReader().use { it.readText() }
    } catch (_: FileNotFoundException) {
      null
    } catch (e: Exception) {
      Log.w(TAG, "[AppMetrics] Failed to read the embedded app config", e)
      null
    }
}

/**
 * Deletes the metrics database, the crash files, and the pending-error files. When a database
 * existed, it saves a new storage reset token, so Observe knows that its dispatch cursors point
 * past the ids of the next database.
 */
internal fun deleteStoredData(context: Context) {
  if (context.getDatabasePath(MetricsDatabase.NAME).exists()) {
    AppMetricsPreferences.setStorageResetToken(context, UUID.randomUUID().toString())
  }
  context.deleteDatabase(MetricsDatabase.NAME)
  CrashFileFormat.crashesDir(context).deleteRecursively()
  PendingErrorStore.directory(context).deleteRecursively()
}
