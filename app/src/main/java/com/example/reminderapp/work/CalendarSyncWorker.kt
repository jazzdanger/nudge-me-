package com.example.reminderapp.work

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.reminderapp.AlarmReciver
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date
import kotlin.math.abs
import java.util.Locale
import java.util.TimeZone
import java.util.Calendar as JCalendar

class CalendarSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val context = applicationContext
            val account = GoogleSignIn.getLastSignedInAccount(context)
                ?: return@withContext Result.retry()

            val credential = GoogleAccountCredential.usingOAuth2(
                context,
                setOf(CalendarScopes.CALENDAR_READONLY)
            ).apply {
                selectedAccountName = account.email
            }

            val service = Calendar.Builder(
                NetHttpTransport(),
                GsonFactory(),
                credential
            ).setApplicationName("Nudge Me Calendar").build()

            val now = JCalendar.getInstance()
            val timeMin = now.time
            now.add(JCalendar.MONTH, 3)
            val timeMax = now.time

            // Fetch primary calendar events
            val primaryEvents = service.events()
                .list("primary")
                .setTimeMin(DateTime(timeMin))
                .setTimeMax(DateTime(timeMax))
                .setOrderBy("startTime")
                .setSingleEvents(true)
                .execute()
                .items.orEmpty()

            // Try to find Birthdays calendar and Tasks calendar and fetch
            val list = service.calendarList().list().execute()
            val birthdayEvents = try {
                val bday = list.items?.firstOrNull { it.summary?.contains("Birthday", true) == true }
                if (bday != null) {
                    service.events().list(bday.id)
                        .setTimeMin(DateTime(timeMin))
                        .setTimeMax(DateTime(timeMax))
                        .setOrderBy("startTime")
                        .setSingleEvents(true)
                        .execute()
                        .items.orEmpty()
                } else emptyList()
            } catch (e: Exception) {
                Log.w("CalendarSyncWorker", "Birthdays calendar not available: ${e.message}")
                emptyList()
            }

            val tasksEvents = try {
                val tasksCal = list.items?.firstOrNull { it.summary?.contains("Tasks", true) == true }
                if (tasksCal != null) {
                    service.events().list(tasksCal.id)
                        .setTimeMin(DateTime(timeMin))
                        .setTimeMax(DateTime(timeMax))
                        .setOrderBy("startTime")
                        .setSingleEvents(true)
                        .execute()
                        .items.orEmpty()
                } else emptyList()
            } catch (e: Exception) {
                Log.w("CalendarSyncWorker", "Tasks calendar not available: ${e.message}")
                emptyList()
            }

            val all = primaryEvents + birthdayEvents + tasksEvents
            Log.d("CalendarSyncWorker", "Fetched ${all.size} events (including birthdays)")

            // Schedule alarms: at start time, and 1 hour before if possible
            val birthdayPhrases = listOf(
                "Heyy! It's %s's birthday. Don't forget a gift!",
                "Reminder: %s is celebrating today 🎉",
                "It's %s's big day — send your wishes!",
                "%s turns a year older today. Maybe a gift?"
            )

            all.forEach { ev ->
                val title = ev.summary ?: "Event"
                val whenMs = eventStartMillis(ev.start?.dateTime, ev.start?.date)
                if (whenMs != null && whenMs > System.currentTimeMillis()) {
                    val isBirthday = title.contains("birthday", ignoreCase = true)
                    val notifTitle = if (isBirthday) "Birthday today" else title
                    val notifBody = if (isBirthday) {
                        val name = title.replace("'s birthday", "", true)
                            .replace("birthday of", "", true)
                            .replace("birthday", "", true)
                            .trim()
                        val variant = birthdayPhrases[abs(name.hashCode()) % birthdayPhrases.size]
                        String.format(variant, name.ifEmpty { "someone" })
                    } else "Calendar"

                    scheduleAlarm(context, notifTitle, whenMs, ev.id.hashCode(), notifBody)
                    val oneHourBefore = whenMs - 60 * 60 * 1000
                    if (oneHourBefore > System.currentTimeMillis()) {
                        scheduleAlarm(context, "$title in 1 hour", oneHourBefore, (ev.id + "-pre").hashCode(), "Calendar")
                    }
                }
            }

            Result.success()
        } catch (t: Throwable) {
            Log.e("CalendarSyncWorker", "Sync failed", t)
            Result.retry()
        }
    }

    private fun eventStartMillis(dateTime: DateTime?, allDay: DateTime?): Long? {
        return when {
            dateTime != null -> dateTime.value
            allDay != null -> {
                // All-day events: treat as local midnight
                val tz = TimeZone.getDefault()
                val cal = JCalendar.getInstance(tz, Locale.getDefault())
                cal.time = Date(allDay.value)
                cal.set(JCalendar.HOUR_OF_DAY, 8) // notify at 8am local for all-day
                cal.set(JCalendar.MINUTE, 0)
                cal.set(JCalendar.SECOND, 0)
                cal.set(JCalendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            else -> null
        }
    }

    private fun scheduleAlarm(context: Context, title: String, whenMs: Long, requestCode: Int, notes: String) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReciver::class.java).apply {
                putExtra("title", title)
                putExtra("notes", notes)
            }
            val pending = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMs, pending)
        } catch (e: Exception) {
            Log.e("CalendarSyncWorker", "Failed to schedule alarm: ${e.message}")
        }
    }
}


