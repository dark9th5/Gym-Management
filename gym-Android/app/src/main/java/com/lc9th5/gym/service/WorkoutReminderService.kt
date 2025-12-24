package com.lc9th5.gym.service

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.lc9th5.gym.MainActivity
import com.lc9th5.gym.R
import com.lc9th5.gym.data.model.DayOfWeek
import com.lc9th5.gym.data.model.WorkoutReminder
import java.util.Calendar

class WorkoutReminderService(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "workout_reminder_channel"
        const val CHANNEL_NAME = "Nhắc nhở tập luyện"
        const val CHANNEL_DESCRIPTION = "Thông báo nhắc nhở lịch tập luyện"
        
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_REMINDER_TITLE = "reminder_title"
        const val EXTRA_REMINDER_MESSAGE = "reminder_message"
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                enableLights(true)
            }
            
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    fun scheduleReminder(reminder: WorkoutReminder) {
        if (!reminder.isActive) {
            cancelReminder(reminder.id)
            return
        }

        val timeParts = reminder.reminderTime.split(":")
        val hour = timeParts.getOrNull(0)?.toIntOrNull() ?: 8
        val minute = timeParts.getOrNull(1)?.toIntOrNull() ?: 0

        // Schedule for each day of week
        reminder.daysOfWeek.forEach { dayOfWeek ->
            scheduleAlarmForDay(reminder, dayOfWeek, hour, minute)
        }
    }

    private fun scheduleAlarmForDay(
        reminder: WorkoutReminder,
        dayOfWeek: DayOfWeek,
        hour: Int,
        minute: Int
    ) {
        // Schedule 3 alarms: 30 min before, 15 min before, and at scheduled time
        val offsets = listOf(
            Triple(-30, "30 phút", "Chuẩn bị! Còn 30 phút nữa đến giờ tập 🏋️"),
            Triple(-15, "15 phút", "Sắp tới giờ! Còn 15 phút nữa 💪"),
            Triple(0, "Đúng giờ", "ĐẾN GIỜ TẬP RỒI! Bắt đầu ngay thôi! 🔥")
        )
        
        offsets.forEach { (offsetMinutes, label, message) ->
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute + offsetMinutes)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                set(Calendar.DAY_OF_WEEK, dayOfWeek.toCalendarDay())
                
                // Handle minute overflow/underflow
                if (get(Calendar.MINUTE) < 0) {
                    add(Calendar.MINUTE, 60)
                    add(Calendar.HOUR_OF_DAY, -1)
                }
                
                // If the time has passed for this week, schedule for next week
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.WEEK_OF_YEAR, 1)
                }
            }

            val intent = Intent(context, WorkoutReminderReceiver::class.java).apply {
                action = "com.lc9th5.gym.WORKOUT_REMINDER"
                putExtra(EXTRA_REMINDER_ID, reminder.id)
                putExtra(EXTRA_REMINDER_TITLE, reminder.title)
                putExtra(EXTRA_REMINDER_MESSAGE, message)
                putExtra("reminder_offset", offsetMinutes) // Track which notification type
            }

            // Unique request code for each alarm (reminder * 100 + day * 10 + offset index)
            val offsetIndex = when(offsetMinutes) {
                -30 -> 0
                -15 -> 1
                else -> 2
            }
            val requestCode = (reminder.id * 100 + dayOfWeek.ordinal * 10 + offsetIndex).toInt()
            
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Check permission for Android 12+ (API 31)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    // Fall back to inexact alarm if permission not granted
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                    return@forEach
                }
            }

            // Use setExactAndAllowWhileIdle for reliable delivery even in Doze mode
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        }
    }

    fun cancelReminder(reminderId: Long) {
        // Cancel for all days and all 3 offset alarms (30min, 15min, at time)
        DayOfWeek.values().forEach { dayOfWeek ->
            for (offsetIndex in 0..2) {
                val requestCode = (reminderId * 100 + dayOfWeek.ordinal * 10 + offsetIndex).toInt()
                val intent = Intent(context, WorkoutReminderReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                pendingIntent?.let { alarmManager.cancel(it) }
            }
        }
    }

    fun cancelAllReminders(reminders: List<WorkoutReminder>) {
        reminders.forEach { cancelReminder(it.id) }
    }

    fun rescheduleAllReminders(reminders: List<WorkoutReminder>) {
        reminders.forEach { scheduleReminder(it) }
    }
}

class WorkoutReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra(WorkoutReminderService.EXTRA_REMINDER_ID, -1)
        val title = intent.getStringExtra(WorkoutReminderService.EXTRA_REMINDER_TITLE) ?: "Nhắc nhở tập luyện"
        val message = intent.getStringExtra(WorkoutReminderService.EXTRA_REMINDER_MESSAGE) ?: "Đến giờ tập luyện rồi! 💪"

        showNotification(context, reminderId.toInt(), title, message)
    }

    private fun showNotification(context: Context, notificationId: Int, title: String, message: String) {
        // Check permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "workout")
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, WorkoutReminderService.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .build()

        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(notificationId, notification)
    }
}

// Boot receiver to reschedule alarms after device restart
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // TODO: Load reminders from local storage or API and reschedule
            // This requires accessing the repository which should be done in a background thread
        }
    }
}

// Extension function
private fun DayOfWeek.toCalendarDay(): Int = when (this) {
    DayOfWeek.MONDAY -> Calendar.MONDAY
    DayOfWeek.TUESDAY -> Calendar.TUESDAY
    DayOfWeek.WEDNESDAY -> Calendar.WEDNESDAY
    DayOfWeek.THURSDAY -> Calendar.THURSDAY
    DayOfWeek.FRIDAY -> Calendar.FRIDAY
    DayOfWeek.SATURDAY -> Calendar.SATURDAY
    DayOfWeek.SUNDAY -> Calendar.SUNDAY
}
