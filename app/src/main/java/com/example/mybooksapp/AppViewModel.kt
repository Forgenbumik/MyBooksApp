package com.example.mybooksapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val _books = MutableStateFlow<List<String>>(emptyList())
    val books: StateFlow<List<String>> = _books.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    var databaseHelper = DatabaseHelper(application.applicationContext)

    init {
        // Загрузка всех книг при инициализации ViewModel
        loadBooks()
    }

    fun loadBooks() {
        viewModelScope.launch(Dispatchers.IO) {
            val allBooks = databaseHelper.getAllBooks()
            _books.value = allBooks
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        filterBooks(query)
    }

    private fun filterBooks(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val allBooks = databaseHelper.getAllBooks()
            val filteredBooks = if (query.isBlank()) {
                allBooks
            } else {
                allBooks.filter { it.contains(query, ignoreCase = true) }
            }
            _books.value = filteredBooks
        }
    }

    fun addBook(title: String, pdfBytes: ByteArray) {
        viewModelScope.launch(Dispatchers.IO) {
            databaseHelper.addBook(title, pdfBytes)
        }
    }
}