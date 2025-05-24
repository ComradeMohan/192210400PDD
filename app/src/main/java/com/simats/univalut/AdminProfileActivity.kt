package com.simats.univalut

import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class AdminProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_profile)

        // List of all editable rows
        val editableFields = listOf(
            Triple(R.id.etName, R.id.btnEditName, "Mohan Reddy"),
            Triple(R.id.etEmail, R.id.btnEditEmail, "AdminSSE@college.edu"),
            Triple(R.id.etPhone, R.id.btnEditPhone, "+91 98765 43210"),
            Triple(R.id.etDepartment, R.id.btnEditDepartment, "Administration"),
            Triple(R.id.etEmployeeId, R.id.btnEditEmployeeId, "ADM001")
        )

        editableFields.forEach { (editTextId, buttonId, value) ->
            val editText = findViewById<EditText>(editTextId)
            val editButton = findViewById<ImageButton>(buttonId)

            editText.setText(value)
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
    }
}
