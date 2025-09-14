import android.content.Intent
    import android.os.Bundle
    import android.text.Editable
    import android.text.TextWatcher
    import android.view.LayoutInflater
    import android.view.View
    import android.view.ViewGroup
    import android.widget.EditText
    import android.widget.Toast
    import androidx.fragment.app.Fragment
    import androidx.recyclerview.widget.LinearLayoutManager
    import androidx.recyclerview.widget.RecyclerView
    import com.android.volley.toolbox.JsonObjectRequest
    import com.android.volley.toolbox.Volley
    import com.simats.univault.Course
    import com.simats.univault.CourseAdapter
    import com.simats.univault.CourseMaterialsActivity
import com.simats.univault.PrepActivity
import com.simats.univault.R
    import org.json.JSONException

    class CourseFragment : Fragment() {
        private var studentID: String? = null
        private val allCourses = mutableListOf<Course>()
        private lateinit var adapter: CourseAdapter
        private lateinit var editTextSearch: EditText
        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            return inflater.inflate(R.layout.fragment_course, container, false)
        }
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            // Retrieve arguments
            studentID = arguments?.getString("studentID")

            editTextSearch = view.findViewById(R.id.editTextSearch)

            val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewCourses)
            recyclerView.layoutManager = LinearLayoutManager(requireContext())

            val courseList = mutableListOf<Course>()
            adapter = CourseAdapter(courseList) { course ->
                val intent = Intent(requireContext(), PrepActivity::class.java)
                intent.putExtra("courseCode", course.Code)
                intent.putExtra("courseName", course.Title)
                startActivity(intent)
            }
            recyclerView.adapter = adapter

            editTextSearch.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    filterCourses(s.toString())
                }
                override fun afterTextChanged(s: Editable?) { }
            })
            
            // Directly fetch courses without college name logic
            fetchCourses(adapter)
        }
        private fun filterCourses(query: String) {
            if (query.isEmpty()) {
                // Show full list when query empty
                adapter.updateCourseList(allCourses)
            } else {
                // Filter list by code or subject name ignoring case
                val filtered = allCourses.filter { course ->
                    course.Code.contains(query, ignoreCase = true) ||
                            course.Title.contains(query, ignoreCase = true)
                }
                adapter.updateCourseList(filtered)
            }
        }
        private fun fetchCourses(adapter: CourseAdapter) {
//            val url = "http://10.86.199.54/univault/fetch_courses.php?college=$collegeName"
            val url = "http://10.86.199.54/univault/get_prep_courses.php"
            val queue = Volley.newRequestQueue(requireContext())

            val jsonObjectRequest = JsonObjectRequest(
                com.android.volley.Request.Method.GET, url, null,
                { response ->
                    try {
                        val success = response.getBoolean("success")
                        if (success) {
                            val coursesArray = response.getJSONArray("courses")
                            val courseList = mutableListOf<Course>()
                            for (i in 0 until coursesArray.length()) {
                                val courseObject = coursesArray.getJSONObject(i)
                                val courseCode = courseObject.getString("course_code")
                                val subjectName = courseObject.getString("subject_name")
                                val credits = courseObject.getInt("strength") // Update to match actual API field if needed
                                courseList.add(Course(courseCode, subjectName, credits))

                                allCourses.clear()
                                allCourses.addAll(courseList) // courseList is what you got from response

                                adapter.updateCourseList(courseList) // This sets up initial display


                            }
                            adapter.updateCourseList(courseList)
                        } else {
                            Toast.makeText(requireContext(), "Error fetching courses", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: JSONException) {
                        Toast.makeText(requireContext(), "Error parsing courses response", Toast.LENGTH_SHORT).show()
                    }
                },
                { error ->
                    Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            )

            queue.add(jsonObjectRequest)
        }

        companion object {
            fun newInstance(studentID: String): CourseFragment {
                val fragment = CourseFragment()
                val args = Bundle()
                args.putString("studentID", studentID)
                fragment.arguments = args
                return fragment
            }
        }
    }