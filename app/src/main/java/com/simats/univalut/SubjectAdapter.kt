    package com.simats.univalut

    import android.view.LayoutInflater
    import android.view.View
    import android.view.ViewGroup
    import android.widget.TextView
    import androidx.recyclerview.widget.RecyclerView

    class SubjectAdapter(private var subjects: List<Subject>) :
        RecyclerView.Adapter<SubjectAdapter.SubjectViewHolder>() {

        class SubjectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvName: TextView = itemView.findViewById(R.id.tvSubjectName)
            val tvCredits: TextView = itemView.findViewById(R.id.tvSubjectCredits)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubjectViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_subject, parent, false)
            return SubjectViewHolder(view)
        }

        override fun onBindViewHolder(holder: SubjectViewHolder, position: Int) {
            val subject = subjects[position]
            holder.tvName.text = subject.name         // ✅ Correct mapping
            holder.tvCredits.text = "Credits: ${subject.credits}"
        }

        override fun getItemCount(): Int = subjects.size
        fun updateData(newSubjects: List<Subject>) {
            subjects = newSubjects
            notifyDataSetChanged()
        }
    }
