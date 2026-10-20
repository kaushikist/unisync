package com.example.unisync

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper
    private lateinit var itemContainer: LinearLayout
    private lateinit var categorySpinner: Spinner

    private val green = Color.rgb(36, 87, 68)
    private val cream = Color.rgb(246, 248, 241)
    private val categories = listOf(
        "All Categories", "Bottle", "Electronics",
        "Books", "Keys", "Other"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = DatabaseHelper(this)
        itemContainer = findViewById(R.id.itemContainer)
        categorySpinner = findViewById(R.id.spCategory)

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            categories
        )
        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )
        categorySpinner.adapter = adapter

        categorySpinner.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) = loadItems()

                override fun onNothingSelected(
                    parent: android.widget.AdapterView<*>?
                ) {}
            }

        findViewById<android.view.View>(R.id.btnReport)
            .setOnClickListener { showReportDialog() }

        findViewById<android.view.View>(R.id.btnTeams)
            .setOnClickListener {
                startActivity(Intent(this, TeamsActivity::class.java))
            }

        loadItems()
    }

    private fun showReportDialog() {
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 8, 24, 8)
        }

        fun makeInput(hintText: String) = EditText(this).apply {
            hint = hintText
            textSize = 15f
            setSingleLine(hintText != "Description")
        }

        val titleInput = makeInput("Item name *")
        val descriptionInput = makeInput("Description")
        val locationInput = makeInput("Location")
        val categoryInput = Spinner(this)
        val statusInput = Spinner(this)

        categoryInput.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            categories.drop(1)
        )
        statusInput.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item,
            listOf("Lost", "Found")
        )

        form.addView(titleInput)
        form.addView(descriptionInput)
        form.addView(locationInput)
        form.addView(categoryInput)
        form.addView(statusInput)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Report an item")
            .setView(form)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {
                    val title = titleInput.text.toString().trim()

                    if (title.isBlank()) {
                        titleInput.error = "Enter an item name"
                        return@setOnClickListener
                    }

                    val result = db.insertItem(
                        title,
                        descriptionInput.text.toString().trim(),
                        categoryInput.selectedItem.toString(),
                        statusInput.selectedItem.toString(),
                        locationInput.text.toString().trim()
                    )

                    if (result != -1L) {
                        Toast.makeText(
                            this, "Item reported successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                        categorySpinner.setSelection(0)
                        loadItems()
                        dialog.dismiss()
                    } else {
                        Toast.makeText(
                            this, "Unable to save item",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }

        dialog.show()
    }

    private fun loadItems() {
        itemContainer.removeAllViews()

        val selected = categorySpinner.selectedItem?.toString()
            ?: "All Categories"
        val cursor = db.getAllItems()
        var count = 0

        cursor.use {
            val idCol = it.getColumnIndexOrThrow("id")
            val titleCol = it.getColumnIndexOrThrow("title")
            val descCol = it.getColumnIndexOrThrow("description")
            val categoryCol = it.getColumnIndexOrThrow("category")
            val statusCol = it.getColumnIndexOrThrow("status")
            val locationCol = it.getColumnIndexOrThrow("location")

            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val title = it.getString(titleCol) ?: "Untitled"
                val description = it.getString(descCol) ?: ""
                val category = it.getString(categoryCol) ?: "Other"
                val status = it.getString(statusCol) ?: "Unknown"
                val location = it.getString(locationCol) ?: "Not specified"

                if (selected != "All Categories" &&
                    !category.equals(selected, ignoreCase = true)
                ) continue

                count++

                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(18, 16, 18, 16)
                    setBackgroundResource(R.drawable.bg_item_card)
                }

                val name = TextView(this).apply {
                    text = title
                    textSize = 18f
                    setTextColor(green)
                    setTypeface(null, Typeface.BOLD)
                }

                val detail = TextView(this).apply {
                    text = "$category • $status\n$location\n$description"
                    textSize = 14f
                    setTextColor(Color.DKGRAY)
                    setPadding(0, 6, 0, 8)
                }

                val action = Button(this).apply {
                    text = if (status == "Claimed") "Delete item"
                    else "View / Manage"
                    isAllCaps = false
                }

                action.setOnClickListener {
                    showItemOptions(id, title, status)
                }

                card.addView(name)
                card.addView(detail)
                card.addView(action)

                val params = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                params.bottomMargin = 12
                itemContainer.addView(card, params)
            }
        }

        if (count == 0) {
            val empty = TextView(this).apply {
                text = "No items here yet.\nBe the first to report one!"
                textSize = 15f
                gravity = Gravity.CENTER
                setTextColor(Color.DKGRAY)
                setPadding(16, 28, 16, 28)
            }
            itemContainer.addView(empty)
        }
    }

    private fun showItemOptions(
        id: Long, title: String, status: String
    ) {
        val options = if (status == "Claimed") {
            arrayOf("View details", "Delete item")
        } else {
            arrayOf("View details", "Mark as claimed", "Delete item")
        }

        AlertDialog.Builder(this)
            .setTitle(title)
            .setItems(options) { _, which ->
                when (options[which]) {
                    "View details" -> showDetails(id)
                    "Mark as claimed" -> {
                        db.updateItemStatus(id, "Claimed")
                        Toast.makeText(
                            this, "Item marked as claimed",
                            Toast.LENGTH_SHORT
                        ).show()
                        loadItems()
                    }
                    "Delete item" -> confirmDelete(id, title)
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showDetails(id: Long) {
        db.getItem(id).use { cursor ->
            if (cursor.moveToFirst()) {
                val title = cursor.getString(
                    cursor.getColumnIndexOrThrow("title")
                ) ?: ""
                val description = cursor.getString(
                    cursor.getColumnIndexOrThrow("description")
                ) ?: ""
                val category = cursor.getString(
                    cursor.getColumnIndexOrThrow("category")
                ) ?: ""
                val status = cursor.getString(
                    cursor.getColumnIndexOrThrow("status")
                ) ?: ""
                val location = cursor.getString(
                    cursor.getColumnIndexOrThrow("location")
                ) ?: ""

                AlertDialog.Builder(this)
                    .setTitle(title)
                    .setMessage(
                        "Category: $category\nStatus: $status\n" +
                                "Location: $location\n\n$description"
                    )
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }

    private fun confirmDelete(id: Long, title: String) {
        AlertDialog.Builder(this)
            .setTitle("Delete item?")
            .setMessage("Remove \"$title\" from the list?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                db.deleteItem(id)
                Toast.makeText(
                    this, "Item deleted", Toast.LENGTH_SHORT
                ).show()
                loadItems()
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        if (::db.isInitialized && ::itemContainer.isInitialized &&
            ::categorySpinner.isInitialized
        ) loadItems()
    }

    override fun onDestroy() {
        if (::db.isInitialized) db.close()
        super.onDestroy()
    }
}
