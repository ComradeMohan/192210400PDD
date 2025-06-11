package com.simats.univalut

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.simats.univalut.DeptCourse

class DeptCourseAdapter(
    private val courses: List<DeptCourse>,
    private val onDelete: (DeptCourse) -> Unit
) : RecyclerView.Adapter<DeptCourseAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCourseName: TextView = view.findViewById(R.id.tvCourseName)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteCourse)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dept_course, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = courses.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val course = courses[position]
        holder.tvCourseName.text = course.name
        holder.btnDelete.setOnClickListener { onDelete(course) }
    }
}
