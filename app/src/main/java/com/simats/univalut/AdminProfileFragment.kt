package com.simats.univalut

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException

class AdminProfileFragment : Fragment() {

    private var adminId: String? = null

    private lateinit var logoutButton: LinearLayout
    private lateinit var changePassword: LinearLayout
    companion object {
        fun newInstance(adminId: String): AdminProfileFragment {
            val fragment = AdminProfileFragment()
            val args = Bundle()
            args.putString("admin_id", adminId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_admin_profile, container, false)

        adminId = arguments?.getString("admin_id")

        logoutButton = view.findViewById(R.id.logoutButton)

        logoutButton.setOnClickListener {
            Toast.makeText(requireContext(), "Logout Clicked", Toast.LENGTH_SHORT).show()
            val sharedPreferences = requireContext().getSharedPreferences("user_sf", Context.MODE_PRIVATE)
            sharedPreferences.edit().clear().apply()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
        // Fetch real data
        fetchAdminDetails(adminId, view)

        // Enable edit mode toggle for each field
        val editableFields = listOf(
            Pair(R.id.etEmail, R.id.btnEditEmail),
            Pair(R.id.etPhone, R.id.btnEditPhone),
        )
        changePassword = view.findViewById(R.id.changePasswordButton)
        editableFields.forEach { (editTextId, buttonId) ->
            val editText = view.findViewById<EditText>(editTextId)
            val editButton = view.findViewById<ImageButton>(buttonId)

            editText.inputType = InputType.TYPE_CLASS_TEXT
            editText.isEnabled = false

            editButton.setOnClickListener {
                editText.isEnabled = !editText.isEnabled
                if (editText.isEnabled) {
                    editText.requestFocus()
                    editText.setSelection(editText.text.length)
                }
            }
        }

        changePassword.setOnClickListener{
            val changePasswordFragment = ChangePasswordFragment()
            val bundle = Bundle()
            bundle.putString("ID", adminId)
            bundle.putString("userType", "admin")
            changePasswordFragment.arguments = bundle

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, changePasswordFragment)
                .addToBackStack(null)
                .commit()
        }
        return view
    }

    private fun fetchAdminDetails(adminId: String?, view: View) {
        if (adminId == null) return

        val url = "https://api-9buk.onrender.com/get_admin_details.php?admin_id=$adminId"
        val requestQueue = Volley.newRequestQueue(requireContext())

        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response ->
                try {
                    // Optional: if your response is not wrapped in {"success":true, "data":{...}}
                    val name = response.getString("name")
                    val email = response.getString("email")
                    val phone = response.getString("phone_number")
                    val employeeId = response.getString("employee_id")
                    val college = response.optString("college", "N/A")

                    view.findViewById<TextView>(R.id.tvAdminName)?.text = name
                    view.findViewById<EditText>(R.id.etName)?.setText(name)
                    view.findViewById<EditText>(R.id.etEmail)?.setText(email)
                    view.findViewById<EditText>(R.id.etPhone)?.setText(phone)
                    view.findViewById<EditText>(R.id.etDepartment)?.setText(college)  // using 'college' as dept
                    view.findViewById<EditText>(R.id.etEmployeeId)?.setText(employeeId)

                    view?.findViewById<TextView>(R.id.tvEmail)?.text = email
                    view?.findViewById<TextView>(R.id.tvPhone)?.text = phone

                } catch (e: JSONException) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Error parsing admin data", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                error.printStackTrace()
                Toast.makeText(requireContext(), "Failed to fetch admin details", Toast.LENGTH_SHORT).show()
            }
        )

        requestQueue.add(jsonObjectRequest)
    }
}
