package com.simats.univalut

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class CompletedCoursesAdapter(
    private val context: Context,
    private val courses: List<CompletedCourse>
) : BaseAdapter() {

    override fun getCount(): Int = courses.size

    override fun getItem(position: Int): Any = courses[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_completed_course, parent, false)

        val course = courses[position]

        view.findViewById<TextView>(R.id.tvCourseName).text = course.name
        view.findViewById<TextView>(R.id.tvCourseGrade).text = course.grade

        return view
    }
}
