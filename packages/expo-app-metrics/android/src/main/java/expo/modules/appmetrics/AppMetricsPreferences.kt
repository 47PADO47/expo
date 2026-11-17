package expo.modules.appmetrics

import android.content.Context
import androidx.core.content.edit
import expo.modules.appmetrics.networkrequests.NetworkTracesConfiguration

private const val PREFS_NAME = "dev.expo.app-metrics"
private const val KEY_ENVIRONMENT = "environment"
private const val KEY_LAST_PROCESSED_EXIT_MILLIS = "lastProcessedExitMillis"
private const val KEY_NETWORK_TRACES_CONFIGURATION = "networkTracesConfiguration"
private const val KEY_STORAGE_RESET_TOKEN = "storageResetToken"
private const val KEY_OBSERVE_CONSENT = "observeConsent"

object AppMetricsPreferences {
  /**
   * Timestamp (epoch millis) of the newest `ApplicationExitInfo` record already
   * turned into a crash report. `0` (the default) means none processed yet.
   */
  fun getLastProcessedExitTimestampMillis(context: Context): Long {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getLong(KEY_LAST_PROCESSED_EXIT_MILLIS, 0L)
  }

  fun setLastProcessedExitTimestampMillis(
    context: Context,
    millis: Long
  ) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit(commit = true) { putLong(KEY_LAST_PROCESSED_EXIT_MILLIS, millis) }
  }

  fun getEnvironment(context: Context): String? {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getString(KEY_ENVIRONMENT, null) ?: getDefaultEnvironment()
  }

  fun setEnvironment(
    context: Context,
    environment: String
  ) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit(commit = true) { putString(KEY_ENVIRONMENT, environment) }
  }

  fun getDefaultEnvironment(): String? {
    return if (BuildConfig.DEBUG) {
      "development"
    } else {
      null
    }
  }

  /**
   * Last-applied network traces recording policy, or the default when JS never configured one.
   * Persisted so requests observed before the JS bundle configures the SDK (early startup, or
   * the next launch) follow the last-known setting.
   */
  fun getNetworkTracesConfiguration(context: Context): NetworkTracesConfiguration {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val json = prefs.getString(KEY_NETWORK_TRACES_CONFIGURATION, null)
      ?: return NetworkTracesConfiguration()
    return NetworkTracesConfiguration.fromJson(json)
  }

  fun setNetworkTracesConfiguration(
    context: Context,
    configuration: NetworkTracesConfiguration
  ) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    // `apply()` rather than `commit()`: this runs inline on the JS thread, and iOS persists
    // through `UserDefaults` without a synchronous flush either.
    prefs.edit { putString(KEY_NETWORK_TRACES_CONFIGURATION, configuration.toJson()) }
  }

  /**
   * Changes each time a launch without consent deletes the metrics database. Observe compares it
   * with the token it saved to reset its dispatch cursors. `null` until the first such deletion.
   */
  fun getStorageResetToken(context: Context): String? =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_STORAGE_RESET_TOKEN, null)

  internal fun setStorageResetToken(context: Context, token: String) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit(commit = true) { putString(KEY_STORAGE_RESET_TOKEN, token) }
  }

  /** The consent value saved by `Observe.setConsent`. `null` until the first call. */
  internal fun getObserveConsent(context: Context): String? =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_OBSERVE_CONSENT, null)

  /** Returns whether the value was written to disk. */
  internal fun setObserveConsent(context: Context, consent: String): Boolean =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(KEY_OBSERVE_CONSENT, consent).commit()
}
