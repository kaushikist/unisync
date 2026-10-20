package com.example.unisync

import android.R.attr.textAllCaps
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class TeamsActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper
    private lateinit var list: LinearLayout

    private val green = Color.rgb(36, 87, 68)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teams)

        db = DatabaseHelper(this)
        list = findViewById(R.id.contributionContainer)

        findViewById<Button>(R.id.btnAddContribution)
            .setOnClickListener { showAddDialog() }

        findViewById<Button>(R.id.btnBack)
            .setOnClickListener { finish() }

        loadContributions()
    }

    private fun showAddDialog() {
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 8, 24, 8)
        }

        fun input(hintText: String) = EditText(this).apply {
            hint = hintText
            textSize = 15f
            isSingleLine = true
        }

        val team = input("Team name *")
        val member = input("Member name *")
        val task = input("Contribution / task *")
        val status = Spinner(this)

        status.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("Pending", "In Progress", "Completed")
        )

        form.addView(team)
        form.addView(member)
        form.addView(task)
        form.addView(status)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Add contribution")
            .setView(form)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {
                    val teamName = team.text.toString().trim()
                    val memberName = member.text.toString().trim()
                    val taskName = task.text.toString().trim()

                    if (teamName.isBlank() ||
                        memberName.isBlank() ||
                        taskName.isBlank()
                    ) {
                        Toast.makeText(
                            this,
                            "Please complete all required fields",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setOnClickListener
                    }

                    val result = db.insertContribution(
                        teamName,
                        memberName,
                        taskName,
                        status.selectedItem.toString()
                    )

                    if (result != -1L) {
                        Toast.makeText(
                            this, "Contribution added",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadContributions()
                        dialog.dismiss()
                    } else {
                        Toast.makeText(
                            this, "Could not save contribution",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }

        dialog.show()
    }

    private fun loadContributions() {
        list.removeAllViews()
        var count = 0

        db.getAllContributions().use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow("id")
            val teamCol = cursor.getColumnIndexOrThrow("team")
            val memberCol = cursor.getColumnIndexOrThrow("member")
            val taskCol = cursor.getColumnIndexOrThrow("task")
            val statusCol = cursor.getColumnIndexOrThrow("status")

            while (cursor.moveToNext()) {
                count++

                val id = cursor.getLong(idCol)
                val team = cursor.getString(teamCol)
                val member = cursor.getString(memberCol)
                val task = cursor.getString(taskCol)
                val status = cursor.getString(statusCol)

                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(18, 16, 18, 16)
                    setBackgroundResource(R.drawable.bg_item_card)
                }

                val heading = TextView(this).apply {
                    text = task
                    textSize = 17f
                    setTextColor(green)
                    setTypeface(null, Typeface.BOLD)
                }

                val details = TextView(this).apply {
                    text = "Team: $team\nMember: $member\nStatus: $status"
                    textSize = 14f
                    setTextColor(Color.DKGRAY)
                    setPadding(0, 6, 0, 10)
                }

                val actions = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                }

                val updateButton = Button(this).apply {
                    text = "Update status"
                    isAllCaps = false
                }

                val deleteButton = Button(this).apply {
                    text = "Delete"
                    isAllCaps = false
                }

                updateButton.setOnClickListener {
                    updateStatus(id)
                }

                deleteButton.setOnClickListener {
                    confirmDelete(id, task)
                }

                actions.addView(
                    updateButton,
                    LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
                    )
                )
                actions.addView(
                    deleteButton,
                    LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f
                    )
                )

                card.addView(heading)
                card.addView(details)
                card.addView(actions)

                val params = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                params.bottomMargin = 12
                list.addView(card, params)
            }
        }

        if (count == 0) {
            list.addView(TextView(this).apply {
                text = "No contributions yet.\nAdd your team's first task!"
                textSize = 15f
                gravity = android.view.Gravity.CENTER
                setPadding(12, 28, 12, 28)
                setTextColor(Color.DKGRAY)
            })
        }
    }

    private fun updateStatus(id: Long) {
        val statuses = arrayOf("Pending", "In Progress", "Completed")

        AlertDialog.Builder(this)
            .setTitle("Update task status")
            .setItems(statuses) { _, which ->
                val result = db.updateContributionStatus(
                    id, statuses[which]
                )
                if (result > 0) {
                    Toast.makeText(
                        this, "Status updated", Toast.LENGTH_SHORT
                    ).show()
                    loadContributions()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDelete(id: Long, task: String) {
        AlertDialog.Builder(this)
            .setTitle("Delete contribution?")
            .setMessage("Delete \"$task\"?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                db.deleteContribution(id)
                Toast.makeText(
                    this, "Contribution deleted",
                    Toast.LENGTH_SHORT
                ).show()
                loadContributions()
            }
            .show()
    }

    override fun onDestroy() {
        if (::db.isInitialized) db.close()
        super.onDestroy()
    }
}
