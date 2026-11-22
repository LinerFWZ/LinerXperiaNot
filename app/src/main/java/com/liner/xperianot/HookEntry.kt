package com.liner.xperianot

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class HookEntry : IXposedHookLoadPackage {

    companion object {
        private const val TARGET_PKG = "com.android.systemui"
        private const val MODULE_PKG = "com.liner.xperianot" // 修改包名时需同步修改此处
        private const val SETTING_KEY = "sf_rr_mode_event"
        private const val CHANNEL_ID = "sf_rr_headsup_silent"
        private const val NOTIF_ID = 202611
        private const val AUTO_DISMISS_MS = 5000L
        private val registered = AtomicBoolean(false)
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != TARGET_PKG || lpparam.processName != TARGET_PKG) return

        XposedHelpers.findAndHookMethod(
            Application::class.java,
            "onCreate",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    if (!registered.compareAndSet(false, true)) return
                    val ctx = (param.thisObject as Context).applicationContext
                    registerObserver(ctx)
                }
            }
        )
    }

    private fun registerObserver(ctx: Context) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                val raw = Settings.Global.getString(ctx.contentResolver, SETTING_KEY) ?: return
                val mode = raw.substringBefore(":")
                showHeadsUp(ctx, mode)
            }
        }
        ctx.contentResolver.registerContentObserver(
            Settings.Global.getUriFor(SETTING_KEY),
            false,
            observer
        )
    }

    private fun isChinese(ctx: Context): Boolean {
        val locale = ctx.resources.configuration.locales.get(0) ?: Locale.getDefault()
        return locale.language.equals("zh", ignoreCase = true)
    }

    private fun loadModuleNotifIcon(ctx: Context): Icon? {
        return try {
            val modCtx = ctx.createPackageContext(MODULE_PKG, Context.CONTEXT_IGNORE_SECURITY)
            val resId = modCtx.resources.getIdentifier("ic_notif_rr", "drawable", MODULE_PKG)
            if (resId == 0) return null
            val drawable = modCtx.getDrawable(resId) ?: return null
            val size = (24 * ctx.resources.displayMetrics.density).toInt().coerceAtLeast(48)
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, size, size)
            drawable.draw(canvas)
            Icon.createWithBitmap(bitmap)
        } catch (_: Throwable) {
            null
        }
    }

    private fun showHeadsUp(ctx: Context, mode: String) {
        val zh = isChinese(ctx)
        val title = if (zh) "刷新率控制" else "Refresh Rate"
        val channelName = if (zh) "刷新率模式提示" else "Refresh Rate Alerts"
        val msg = when (mode) {
            "120" -> if (zh) "刷新率：全局锁定 120Hz" else "Mode: Locked 120Hz"
            "60" -> if (zh) "刷新率：全局锁定 60Hz" else "Mode: Locked 60Hz"
            "auto" -> if (zh) "刷新率：动态触控 60/120Hz" else "Mode: Dynamic 60/120Hz"
            else -> return
        }

        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(
                    null,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build()
                )
                enableVibration(false)
                enableLights(false)
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }

        val builder = Notification.Builder(ctx, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(msg)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setTimeoutAfter(AUTO_DISMISS_MS)

        val customIcon = loadModuleNotifIcon(ctx)
        if (customIcon != null) {
            builder.setSmallIcon(customIcon)
        } else {
            builder.setSmallIcon(android.R.drawable.ic_dialog_info)
        }

        nm.notify(NOTIF_ID, builder.build())
    }
}
