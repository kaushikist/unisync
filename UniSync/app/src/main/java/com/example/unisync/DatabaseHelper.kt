
package com.example.unisync

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.Locale

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "unisync.db", null, 1) {

    companion object {
        const val ITEMS = "items"
        const val CONTRIBUTIONS = "contributions"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE items (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                description TEXT,
                category TEXT,
                status TEXT,
                location TEXT
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE contributions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                team TEXT NOT NULL,
                member TEXT NOT NULL,
                task TEXT NOT NULL,
                status TEXT NOT NULL
            )
            """.trimIndent()
        )

        val sample = ContentValues().apply {
            put("title", "Blue Water Bottle")
            put("description", "Found near the library")
            put("category", "Bottle")
            put("status", "Found")
            put("location", "Library")
        }
        db.insert(ITEMS, null, sample)
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        // Keep existing records when adding future schema migrations.
        // This starter version has no migrations yet.
    }

    // LOST AND FOUND

    fun insertItem(
        title: String,
        description: String,
        category: String,
        status: String,
        location: String
    ): Long {
        val values = ContentValues().apply {
            put("title", title)
            put("description", description)
            put("category", category)
            put("status", status)
            put("location", location)
        }
        return writableDatabase.insert(ITEMS, null, values)
    }

    fun getAllItems(): Cursor =
        readableDatabase.query(
            ITEMS, null, null, null, null, null, "id DESC"
        )

    fun getItem(id: Long): Cursor =
        readableDatabase.query(
            ITEMS, null, "id = ?",
            arrayOf(id.toString()), null, null, null
        )

    fun updateItemStatus(id: Long, status: String): Int {
        val values = ContentValues().apply {
            put("status", status)
        }
        return writableDatabase.update(
            ITEMS, values, "id = ?", arrayOf(id.toString())
        )
    }

    fun deleteItem(id: Long): Int =
        writableDatabase.delete(
            ITEMS, "id = ?", arrayOf(id.toString())
        )

    // FAIRSHARE CONTRIBUTIONS

    fun insertContribution(
        team: String,
        member: String,
        task: String,
        status: String
    ): Long {
        val values = ContentValues().apply {
            put("team", team)
            put("member", member)
            put("task", task)
            put("status", status)
        }
        return writableDatabase.insert(CONTRIBUTIONS, null, values)
    }

    fun getAllContributions(): Cursor =
        readableDatabase.query(
            CONTRIBUTIONS, null, null, null, null, null, "id DESC"
        )

    fun updateContributionStatus(id: Long, status: String): Int {
        val values = ContentValues().apply {
            put("status", status)
        }
        return writableDatabase.update(
            CONTRIBUTIONS, values, "id = ?", arrayOf(id.toString())
        )
    }

    fun deleteContribution(id: Long): Int =
        writableDatabase.delete(
            CONTRIBUTIONS, "id = ?", arrayOf(id.toString())
        )
}
