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

    private val _currentTheme = MutableStateFlow(Theme.SYSTEM)
    val currentTheme: StateFlow<Theme> = _currentTheme

    private val _books = MutableStateFlow<List<Book>>(emptyList())
    val books: StateFlow<List<Book>> = _books.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val databaseHelper = DatabaseHelper(application.applicationContext)

    init {
        // Загрузка всех книг при инициализации ViewModel
        loadBooks()
    }

    fun setTheme(theme: Theme) {
        _currentTheme.value = theme
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
                allBooks.filter { it.title.contains(query, ignoreCase = true) }
            }
            _books.value = filteredBooks
        }
    }

    fun addBook(book: Book) {
        viewModelScope.launch(Dispatchers.IO) {
            databaseHelper.addBook(book)
            loadBooks()
        }
    }
}