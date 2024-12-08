package com.example.mybooksapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "books.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "books"
        private const val COLUMN_ID = "id"
        private const val COLUMN_TITLE = "title"
        private const val COLUMN_PDF = "pdf"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = ("CREATE TABLE $TABLE_NAME ($COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "$COLUMN_TITLE TEXT, $COLUMN_PDF BLOB)")
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    // Метод для добавления книги
    fun addBook(title: String, pdf: ByteArray) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TITLE, title)
            put(COLUMN_PDF, pdf)
        }
        db.insert(TABLE_NAME, null, values)
    }

    // Метод для получения всех названий книг
    fun getAllBooks(): List<String> {
        val books = mutableListOf<String>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT $COLUMN_ID, $COLUMN_TITLE FROM $TABLE_NAME", null)
        if (cursor.moveToFirst()) {
            do {
                books.add(cursor.getString(0))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return books
    }

    fun getBookById() {

    }
}