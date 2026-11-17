package expo.modules.storageinspector

import android.content.Context
import expo.modules.kotlin.exception.Exceptions
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import java.io.File

/**
 * Inline module that reports which Observe data is stored on the device, for the consent tests.
 * It only reads, so it never creates the installation ID or the database.
 */
class StorageInspector : Module() {
  private val context: Context
    get() = appContext.reactContext ?: throw Exceptions.ReactContextLost()

  override fun definition() = ModuleDefinition {
    Function("inspect") {
      fun fileCount(path: String) = File(context.noBackupFilesDir, path).listFiles()?.size ?: 0
      mapOf(
        "databaseExists" to context.getDatabasePath("app_metrics").exists(),
        "legacyStoreExists" to context.getDatabasePath("eas_observe").exists(),
        "crashFileCount" to fileCount("expo-app-metrics/crashes"),
        "pendingErrorFileCount" to fileCount("ExpoAppMetrics/pending-errors"),
        "installationIdStored" to context
          .getSharedPreferences("dev.expo.EASSharedPreferences", Context.MODE_PRIVATE)
          .contains("eas-client-id")
      )
    }
  }
}
