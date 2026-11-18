package com.muse.email

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import org.json.JSONObject
import java.util.concurrent.TimeUnit

const val MAIL_CHECK_WORK = "mail-check"
private const val CHANNEL_ID = "mail_new"
private const val PREFS_NAME = "muse-email"

/**
 * 后台轮询新邮件：服务端没有推送通道，只能定时拉一次收件箱未读。
 * 只对“上次见过之后新到的”发通知；App 内刷过列表会同步更新 lastNotifiedId，
 * 所以已读过的不会再弹。首次开启那次只记水位、不通知，避免把旧未读全弹出来。
 */
class MailCheckWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val base = prefs.getString("baseUrl", "") ?: ""
        if (base.isBlank()) return Result.success()
        return try {
            RustBridge.call("init", JSONObject().apply {
                put("baseUrl", base)
                put("dataDir", applicationContext.filesDir.absolutePath)
                put("allowInsecureTls", prefs.getBoolean("insecure", false))
                put("timeoutSeconds", 20)
            })
            if (!RustBridge.call("ping").bool("loggedIn")) return Result.success()
            val result = RustBridge.call("messages", JSONObject().apply {
                put("folder", "INBOX")
                put("limit", 5)
                put("offset", 0)
                put("unread", true)
            })
            val unread = result.optJSONArray("messages").objects().map { MessageSummary.from(it) }
            if (unread.isEmpty()) return Result.success()
            val maxId = unread.maxOf { it.id }
            val lastSeen = prefs.getLong("lastNotifiedId", 0L)
            prefs.edit().putLong("lastNotifiedId", maxOf(lastSeen, maxId)).apply()
            if (lastSeen == 0L) return Result.success() // 首次运行只记水位
            val fresh = unread.filter { it.id > lastSeen }
            if (fresh.isEmpty()) return Result.success()
            notifyNewMail(fresh.maxByOrNull { it.id }!!)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun notifyNewMail(message: MessageSummary) {
        val context = applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "新邮件", NotificationManager.IMPORTANCE_DEFAULT)
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pending = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val sender = message.fromName.ifBlank { message.from.ifBlank { "新邮件" } }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.mascot))
            .setContentTitle(sender)
            .setContentText(message.subject.ifBlank { "(无主题)" })
            .setStyle(NotificationCompat.BigTextStyle().bigText(message.snippet.ifBlank { message.subject }))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(message.id.toInt(), notification)
        } catch (e: SecurityException) {
            // 通知权限被收回时静默跳过，不炸后台任务
        }
    }
}

fun scheduleMailCheck(context: Context, intervalMinutes: Long) {
    val request = PeriodicWorkRequestBuilder<MailCheckWorker>(intervalMinutes, TimeUnit.MINUTES)
        .setConstraints(
            androidx.work.Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        )
        .build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        MAIL_CHECK_WORK, ExistingPeriodicWorkPolicy.UPDATE, request
    )
}

fun cancelMailCheck(context: Context) {
    WorkManager.getInstance(context).cancelUniqueWork(MAIL_CHECK_WORK)
}
