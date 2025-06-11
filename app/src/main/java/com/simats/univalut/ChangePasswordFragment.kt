package com.simats.univalut

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class ChangePasswordFragment : Fragment() {

    private lateinit var oldPasswordEditText: EditText
    private lateinit var newPasswordEditText: EditText
    private lateinit var confirmNewPasswordEditText: EditText
    private lateinit var submitButton: Button
    private lateinit var progressBar: ProgressBar

    private var facultyId: String? = null
    private var userType: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        facultyId = arguments?.getString("ID")
        userType = arguments?.getString("userType")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_change_password, container, false)
        val backButton = view.findViewById<ImageView>(R.id.backButton)
        backButton.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        // Initialize views
        oldPasswordEditText = view.findViewById(R.id.oldPasswordEditText)
        newPasswordEditText = view.findViewById(R.id.newPasswordEditText)
        confirmNewPasswordEditText = view.findViewById(R.id.confirmNewPasswordEditText)
        submitButton = view.findViewById(R.id.submitChangePasswordButton)

        progressBar = view.findViewById(R.id.progressBar)


        submitButton.setOnClickListener {
            val oldPassword = oldPasswordEditText.text.toString()
            val newPassword = newPasswordEditText.text.toString()
            val confirmNewPassword = confirmNewPasswordEditText.text.toString()

            submitButton.text = ""
            progressBar.visibility = View.VISIBLE
            submitButton.isEnabled = false

            if (newPassword == confirmNewPassword) {
                if (facultyId != null && userType != null) {
                    changePassword(facultyId!!, oldPassword, newPassword, userType!!)
                }
            } else {
                Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
                resetButtonState()
            }
        }

        return view
    }

    private fun changePassword(facultyId: String, oldPassword: String, newPassword: String, userType: String) {
        val url = "http://192.168.205.54/UniVault/change_password.php"

        val requestQueue = Volley.newRequestQueue(requireContext())
        val stringRequest = object : StringRequest(Method.POST, url,
            { response ->
                try {
                    val jsonResponse = JSONObject(response)
                    val success = jsonResponse.getBoolean("success")
                    val message = jsonResponse.getString("message")
                    if (success) {
                        Toast.makeText(requireContext(), "Password changed successfully", Toast.LENGTH_SHORT).show()
                        resetButtonState()
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    } else {
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                        resetButtonState()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(requireContext(), "Error parsing response", Toast.LENGTH_SHORT).show()
                    resetButtonState()
                }
            },
            { error ->
                error.printStackTrace()
                Toast.makeText(requireContext(), "Network Error: ${error.message}", Toast.LENGTH_SHORT).show()
                resetButtonState()
            }
        ) {
            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()

                if (userType == "faculty") {
                    params["faculty_id"] = facultyId!!
                } else if (userType == "student") {
                    params["student_number"] = facultyId!!  // assuming it's the student number here
                }
                else if(userType == "admin"){
                    params["admin_id"] = facultyId
                }

                params["old_password"] = oldPassword
                params["new_password"] = newPassword
                params["user_type"] = userType!!

                return params
            }

        }

        requestQueue.add(stringRequest)
    }
    private fun resetButtonState() {
        progressBar.visibility = View.GONE
        submitButton.text = "Change Password"
        submitButton.isEnabled = true
    }
}
