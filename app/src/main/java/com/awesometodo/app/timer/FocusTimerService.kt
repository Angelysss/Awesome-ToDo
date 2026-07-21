package com.awesometodo.app.timer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.awesometodo.app.AwesomeTodoApplication
import com.awesometodo.app.MainActivity
import com.awesometodo.app.data.TimerStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class FocusTimerService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val repository by lazy { (application as AwesomeTodoApplication).repository }
    private val dao by lazy { (application as AwesomeTodoApplication).database.appDao() }
    private var tickerJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> scope.launch { repository.pauseTimer() }
            ACTION_RESUME -> scope.launch { repository.resumeTimer() }
        }
        startTicker()
        return START_STICKY
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch {
            while (isActive) {
                val active = dao.getActiveTimer()
                if (active == null) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    break
                }
                val remaining = TimerPolicy.remainingSeconds(active, System.currentTimeMillis())
                startForeground(ONGOING_ID, ongoingNotification(active.todoTitle, remaining, active.status))
                if (remaining == 0L) {
                    if (repository.completeNaturally()) notifyFinished(active.todoTitle)
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    break
                }
                delay(1_000L)
            }
        }
    }

    private fun ongoingNotification(title: String, seconds: Long, status: TimerStatus) =
        NotificationCompat.Builder(this, CHANNEL_ONGOING)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(if (status == TimerStatus.PAUSED) "已暂停 · ${format(seconds)}" else "专注中 · ${format(seconds)}")
            .setContentIntent(openAppIntent())
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .addAction(
                0,
                if (status == TimerStatus.PAUSED) "继续" else "暂停",
                serviceIntent(if (status == TimerStatus.PAUSED) ACTION_RESUME else ACTION_PAUSE),
            )
            .build()

    private fun notifyFinished(title: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_FINISHED)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("专注完成")
            .setContentText("“$title”已完成，休息一下吧")
            .setContentIntent(openAppIntent())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
        try {
            NotificationManagerCompat.from(this).notify(FINISHED_ID, notification)
        } catch (_: SecurityException) {
            // Android 13+ notification permission may be denied; the timer still completes correctly.
        }
    }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun serviceIntent(action: String): PendingIntent = PendingIntent.getService(
        this, action.hashCode(), Intent(this, FocusTimerService::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun createChannels() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ONGOING, "专注计时", NotificationManager.IMPORTANCE_LOW))
        val finished = NotificationChannel(CHANNEL_FINISHED, "专注完成提醒", NotificationManager.IMPORTANCE_HIGH).apply {
            enableVibration(true)
            setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI, AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build())
        }
        manager.createNotificationChannel(finished)
    }

    override fun onDestroy() {
        tickerJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.awesometodo.app.timer.START"
        const val ACTION_PAUSE = "com.awesometodo.app.timer.PAUSE"
        const val ACTION_RESUME = "com.awesometodo.app.timer.RESUME"
        private const val CHANNEL_ONGOING = "focus_timer"
        private const val CHANNEL_FINISHED = "focus_finished"
        private const val ONGOING_ID = 701
        private const val FINISHED_ID = 702

        fun startIntent(context: Context) = Intent(context, FocusTimerService::class.java).setAction(ACTION_START)
        private fun format(seconds: Long) = "%02d:%02d".format(seconds / 60, seconds % 60)
    }
}
