package com.simats.univalut

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class FacultyCourseAdapter(
    private val courseList: List<FacultyCourse>,
    private val onItemSelected: (FacultyCourse) -> Unit
) : RecyclerView.Adapter<FacultyCourseAdapter.CourseViewHolder>() {

    private var selectedPosition = -1

    inner class CourseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val code: TextView = itemView.findViewById(R.id.courseCode)
        val title: TextView = itemView.findViewById(R.id.courseTitle)
        val instructor: TextView = itemView.findViewById(R.id.courseInstructor)
        val radio: RadioButton = itemView.findViewById(R.id.radioSelect)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_course, parent, false)
        return CourseViewHolder(view)
    }

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        val course = courseList[position]
        holder.code.text = course.code
        holder.title.text = course.title
        holder.instructor.text = course.instructor
        holder.radio.isChecked = position == selectedPosition

        val clickListener = View.OnClickListener {
            val previousPosition = selectedPosition
            selectedPosition = holder.adapterPosition
            notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)
            onItemSelected(course)
        }

        holder.itemView.setOnClickListener(clickListener)
        holder.radio.setOnClickListener(clickListener)
    }

    override fun getItemCount() = courseList.size
}
