package com.simats.univault

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageButton
import android.widget.TextView

class CompletedCoursesAdapter(
    private val context: Context,
    private var courses: MutableList<CompletedCourse>,
    private val onDeleteClickListener: OnDeleteClickListener
) : BaseAdapter() {

    interface OnDeleteClickListener {
        fun onDeleteClicked(position: Int)
    }

    override fun getCount(): Int = courses.size

    override fun getItem(position: Int): Any = courses[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_completed_course, parent, false)

        val course = courses[position]

        // Hide courseId and show only course name & grade
        view.findViewById<TextView>(R.id.tvCourseCode).visibility = View.GONE
        view.findViewById<TextView>(R.id.tvCourseName).text = course.name
        view.findViewById<TextView>(R.id.tvCourseGrade).text = course.grade

        view.findViewById<ImageButton>(R.id.btnDelete).setOnClickListener {
            onDeleteClickListener.onDeleteClicked(position)
        }

        return view
    }

    fun updateCourses(newCourses: List<CompletedCourse>) {
        courses.clear()
        courses.addAll(newCourses)
        notifyDataSetChanged()
    }
}
