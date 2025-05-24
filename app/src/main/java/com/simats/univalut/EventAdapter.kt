package com.simats.univalut

import android.app.AlarmManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.time.format.DateTimeFormatter
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import java.util.*

class EventAdapter(private val eventList: MutableList<Event>) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    // ViewHolder for Event items
    class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.textViewEventTitle)
        val type: TextView = itemView.findViewById(R.id.textViewEventType)
        val startDate: TextView = itemView.findViewById(R.id.textViewEventStartDate)
        val endDate: TextView = itemView.findViewById(R.id.textViewEventEndDate)
        val description: TextView = itemView.findViewById(R.id.textViewEventDescription)
        val notifyButton: Button = itemView.findViewById(R.id.buttonNotify)
    }

    // Inflate the layout for the event
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_event, parent, false)
        return EventViewHolder(itemView)
    }

    // Bind the event data to the view
    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = eventList[position]
        holder.title.text = event.title
        holder.type.text = event.type
        holder.startDate.text = event.startDate.toString()
        holder.endDate.text = event.endDate.toString()
        holder.description.text = event.description

        holder.notifyButton.setOnClickListener {
            // Start date picker for notification
            showDatePicker(holder.itemView.context, event)
        }
    }

    // Return the size of the event list
    override fun getItemCount(): Int {
        return eventList.size
    }
    fun showDatePicker(context: Context, event: Event) {
        val calendar = Calendar.getInstance()

        val datePicker = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val pickedDate = Calendar.getInstance()
                pickedDate.set(year, month, dayOfMonth)
                showTimePicker(context, event, pickedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    fun showTimePicker(context: Context, event: Event, date: Calendar) {
        val calendar = Calendar.getInstance()
        val timePicker = TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                date.set(Calendar.HOUR_OF_DAY, hourOfDay)
                date.set(Calendar.MINUTE, minute)
                date.set(Calendar.SECOND, 0)

                val currentTime = Calendar.getInstance()
                if (date.timeInMillis <= currentTime.timeInMillis) {
                    Toast.makeText(context, "Please select a future time", Toast.LENGTH_SHORT).show()
                    return@TimePickerDialog
                }

                //val triggerAtMillis = System.currentTimeMillis() + 10_000 // 10 seconds from now
                //scheduleNotification(context, event, triggerAtMillis)

               scheduleNotification(context, event, date.timeInMillis)
                Toast.makeText(context, "Notification scheduled!", Toast.LENGTH_SHORT).show()
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        )
        timePicker.show()
    }
    fun scheduleNotification(context: Context, event: Event, triggerAtMillis: Long) {

        Log.d("EventAdapter", "Scheduling notification for '${event.title}' at $triggerAtMillis")

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("title", event.title)
            putExtra("description", event.description)
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            event.title.hashCode(), // unique requestCode per event
            intent,
            flags
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {

                Log.d("EventAdapter", "Exact alarms permission not granted")
                // Permission not granted, prompt user to allow it in settings:
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)

                // Optionally, show a Toast or alert explaining why
                Toast.makeText(context, "Please allow exact alarms in settings to enable notifications", Toast.LENGTH_LONG).show()

                // Don't schedule the alarm now, return or handle accordingly
                return
            }
        }

// If permission is granted, schedule the alarm normally:
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        Log.d("EventAdapter", "Alarm scheduled")
    }
    // Method to update the event list dynamically
    fun updateEvents(events: List<Event>) {
        eventList.clear() // Clear the current list
        eventList.addAll(events) // Add the new events
        notifyDataSetChanged() // Notify the adapter to refresh the view
    }
}
