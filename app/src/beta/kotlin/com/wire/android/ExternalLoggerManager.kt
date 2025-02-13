package com.wire.android

import android.app.Activity
import android.content.Context
import com.datadog.android.Datadog
import com.datadog.android.DatadogSite
import com.datadog.android.core.configuration.Credentials
import com.datadog.android.privacy.TrackingConsent
import com.datadog.android.rum.GlobalRum
import com.datadog.android.rum.RumMonitor
import com.datadog.android.rum.tracking.ActivityViewTrackingStrategy
import com.datadog.android.rum.tracking.ComponentPredicate
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.ui.WireActivity
import com.wire.android.util.getDeviceId
import com.wire.android.util.sha256
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private const val LONG_TASK_THRESH_HOLD_MS = 1000L

object ExternalLoggerManager {

    fun initDatadogLogger(context: Context, globalDataStore: GlobalDataStore) {

        val clientToken = "pub98ad02250435b6082337bb79f66cbc19"
        val applicationId = "619af3ef-2fa6-41e2-8bb1-b42041d50802"

        val environmentName = "internal"
        val appVariantName = "com.wire.android.${BuildConfig.FLAVOR}.${BuildConfig.BUILD_TYPE}"

        val configuration = com.datadog.android.core.configuration.Configuration.Builder(
            logsEnabled = true,
            tracesEnabled = true,
            rumEnabled = true,
            crashReportsEnabled = true,
        )
            .useViewTrackingStrategy(
                ActivityViewTrackingStrategy(
                    trackExtras = true,
                    componentPredicate = object : ComponentPredicate<Activity> {
                        override fun accept(component: Activity): Boolean {
                            // reject Activities which are hosts of Compose views, so that they are not counted as views
                            return component !is WireActivity
                        }

                        override fun getViewName(component: Activity): String? = null
                    }
                )
            )
            .trackInteractions()
            .trackBackgroundRumEvents(true)
            .trackLongTasks(LONG_TASK_THRESH_HOLD_MS)
            .useSite(DatadogSite.EU1)
            .build()

        val credentials = Credentials(clientToken, environmentName, appVariantName, applicationId)
        val extraInfo = mapOf(
            "encrypted_proteus_storage_enabled" to runBlocking { globalDataStore.isEncryptedProteusStorageEnabled().first() }
        )

        Datadog.initialize(context, credentials, configuration, TrackingConsent.GRANTED)
        Datadog.setUserInfo(id = context.getDeviceId()?.sha256(), extraInfo = extraInfo)
        GlobalRum.registerIfAbsent(RumMonitor.Builder().build())
    }
}
