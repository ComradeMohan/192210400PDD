package com.simats.univault

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.widget.Filter
import android.widget.Filterable
import java.util.*

class FacultyCourseAdapter(
    private val originalList: List<FacultyCourse>,
    private val onItemSelected: (FacultyCourse) -> Unit
) : RecyclerView.Adapter<FacultyCourseAdapter.CourseViewHolder>(), Filterable {

    private var filteredList = originalList.toMutableList()
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
        val course = filteredList[position]
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

    override fun getItemCount() = filteredList.size

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(query: CharSequence?): FilterResults {
                val searchText = query?.toString()?.lowercase(Locale.getDefault()) ?: ""
                val results = if (searchText.isEmpty()) {
                    originalList
                } else {
                    originalList.filter {
                        it.code.lowercase(Locale.getDefault()).contains(searchText) ||
                                it.title.lowercase(Locale.getDefault()).contains(searchText)
                    }
                }
                return FilterResults().apply { values = results }
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredList = (results?.values as? List<FacultyCourse>)?.toMutableList() ?: mutableListOf()
                notifyDataSetChanged()
            }
        }
    }
}
