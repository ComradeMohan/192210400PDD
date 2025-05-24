package com.simats.univalut

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CourseAdapter(
    private var courses: List<Course>, // Make courses mutable
    private val onCourseClick: (Course) -> Unit
) : RecyclerView.Adapter<CourseAdapter.CourseViewHolder>() {

    inner class CourseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(course: Course) {
            itemView.findViewById<TextView>(R.id.textCourseCode).text = course.Code
            itemView.findViewById<TextView>(R.id.textCourseTitle).text = course.Title
            itemView.findViewById<TextView>(R.id.textCredits).text = "Credits: ${course.credits}"

            itemView.setOnClickListener {
                onCourseClick(course)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_course_card, parent, false)
        return CourseViewHolder(view)
    }

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        holder.bind(courses[position])
    }

    override fun getItemCount(): Int = courses.size

    // Method to update the course list
    fun updateCourseList(newCourses: List<Course>) {
        courses = newCourses // Update the course list
        notifyDataSetChanged() // Notify the adapter that the data has changed
    }
}
