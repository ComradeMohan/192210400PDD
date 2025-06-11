package com.simats.univalut

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DepartmentAdapter(
    private val departments: List<ManageDepartmentFragment.Department>,
    private val onClick: (ManageDepartmentFragment.Department) -> Unit,
    private val onDelete: (ManageDepartmentFragment.Department) -> Unit
) : RecyclerView.Adapter<DepartmentAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDeptName: TextView = view.findViewById(R.id.tvDeptName)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteDept)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_department, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = departments.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val dept = departments[position]
        holder.tvDeptName.text = dept.name
        holder.itemView.setOnClickListener { onClick(dept) }
        holder.btnDelete.setOnClickListener { onDelete(dept) }
    }
}
