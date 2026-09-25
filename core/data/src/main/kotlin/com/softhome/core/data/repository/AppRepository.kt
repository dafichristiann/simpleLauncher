package com.softhome.core.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import com.softhome.core.common.DispatcherProvider
import com.softhome.core.model.AppInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** Reads the installed, launchable apps from the system. */
interface AppRepository {
    suspend fun getInstalledApps(): List<AppInfo>
    fun launchApp(app: AppInfo)
}

@Singleton
class AppRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider,
) : AppRepository {

    override suspend fun getInstalledApps(): List<AppInfo> = withContext(dispatchers.io) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved: List<ResolveInfo> = pm.queryIntentActivities(
            intent,
            // Launcher activities do not have to advertise CATEGORY_DEFAULT. Using
            // MATCH_DEFAULT_ONLY silently drops many real apps on Tecno (ChatGPT,
            // Instagram, Discord, Gojek, etc.). The drawer must enumerate every
            // launchable activity, not only the system's preferred/default handlers.
            PackageManager.MATCH_ALL or PackageManager.GET_META_DATA,
        )
        resolved.asSequence()
            .mapNotNull { ri ->
                val ai = ri.activityInfo ?: return@mapNotNull null
                val label = ri.loadLabel(pm).toString().ifBlank { ai.packageName }
                AppInfo(
                    packageName = ai.packageName,
                    className = ai.name,
                    label = label,
                    componentKey = "${ai.packageName}/${ai.name}",
                    isSystem = (ai.applicationInfo?.flags ?: 0) and
                        android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0,
                    category = (ai.applicationInfo?.category ?: 0)
                        .takeIf { it != android.content.pm.ApplicationInfo.CATEGORY_UNDEFINED },
                )
            }
            .distinctBy { it.componentKey }
            // Keep alphabet/index positions stable when labels differ only by case or
            // multiple activities expose the same label.
            .sortedWith(
                compareBy<AppInfo> { it.label.trim().lowercase(Locale.ROOT) }
                    .thenBy { it.componentKey.lowercase(Locale.ROOT) },
            )
            .toList()
    }

    override fun launchApp(app: AppInfo) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            setClassName(app.packageName, app.className)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        }
        try {
            context.startActivity(intent)
        } catch (_: Throwable) {
            // Graceful: fall back to the plain launch intent for the package.
            context.packageManager.getLaunchIntentForPackage(app.packageName)?.let {
                it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                runCatching { context.startActivity(it) }
            }
        }
    }
}
