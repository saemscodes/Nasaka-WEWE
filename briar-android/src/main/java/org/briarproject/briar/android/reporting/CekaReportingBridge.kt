package org.briarproject.briar.android.reporting

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.briarproject.bramble.util.LogUtils
import java.util.logging.Level
import java.util.logging.Logger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CekaReportingBridge @Inject constructor(
    private val supabaseClient: SupabaseClient
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val LOG = Logger.getLogger(CekaReportingBridge::class.java.name)

    fun sendToCeka(reportText: String) {
        scope.launch {
            try {
                LOG.info("Posting diagnostic report to Supabase diagnostics table...")
                val data = mapOf(
                    "report_text" to reportText,
                    "timestamp" to System.currentTimeMillis()
                )
                supabaseClient.postgrest["diagnostics"].insert(data)
                LOG.info("Diagnostic report successfully posted to Supabase.")
            } catch (e: Exception) {
                LogUtils.logException(LOG, Level.WARNING, e)
            }
        }
    }
}
