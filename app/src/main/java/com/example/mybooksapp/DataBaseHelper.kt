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
        private const val COLUMN_AUTHOR = "author"
        private const val COLUMN_FILE_PATH = "file_path"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTable = ("CREATE TABLE $TABLE_NAME ($COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "$COLUMN_TITLE TEXT, $COLUMN_AUTHOR TEXT, $COLUMN_FILE_PATH TEXT)")
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    // Метод для добавления книги
    fun addBook(book: Book) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TITLE, book.title)
            put(COLUMN_AUTHOR, book.author)
            put(COLUMN_FILE_PATH, book.filePath)
        }
        db.insert(TABLE_NAME, null, values)
    }

    // Метод для получения всех книг
    fun getAllBooks(): List<Book> {
        val books = mutableListOf<Book>()
        val db = this.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_NAME", null)

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID))
                val title = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TITLE))
                val author = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_AUTHOR))
                val filePath = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_FILE_PATH))
                books.add(Book(id, title, author, filePath))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return books
    }

    fun deleteBook(bookId: Long): Boolean {
        val db = this.writableDatabase
        return db.delete(TABLE_NAME, "$COLUMN_ID = ?", arrayOf(bookId.toString())) > 0
    }
}