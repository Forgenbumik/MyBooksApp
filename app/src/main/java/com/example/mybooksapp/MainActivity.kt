package com.example.mybooksapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.testa.PdfViewerActivity
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    private var selectedPdfUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        deleteDatabase("books.db")
        viewModel.loadBooks()
        setContent {
            MainScreen(
                onPickFile = { selectPdfFile() },
                onBookClick = { book -> onBookClick(book) } // Передаем обработчик
            )
        }
    }

    private val pdfLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            selectedPdfUri = result.data?.data
            selectedPdfUri?.let { uri ->
                setContent {


                    AuthorInputDialog(
                        showDialog = showDialog,
                        onConfirm = { author ->
                            val filePath = uri.toString()
                            val title = getFileName(uri)
                            title?.let { Book(0, it, author, filePath) }?.let { viewModel.addBook(it) }
                            viewModel.loadBooks()
                        }
                    )

                    MainScreen(
                        onPickFile = { selectPdfFile() },
                        onBookClick = { book -> onBookClick(book) }
                    )
                }
            }
        } else {
            Toast.makeText(this, "Не удалось выбрать файл", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME) // Используем getColumnIndex
                if (nameIndex != -1) {
                    name = it.getString(nameIndex)
                }
            }
        }
        return name
    }

    private fun selectPdfFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/pdf"
        }
        pdfLauncher.launch(intent)
    }

    fun onBookClick(book: Book) {
        val intent = Intent(this, PdfViewerActivity::class.java).apply {
            putExtra("pdfUri", book.filePath) // Передаем путь к файлу
        }
        startActivity(intent)
    }
}

@Composable
fun AuthorInputDialog(
    showDialog: MutableState<Boolean>, // Добавляем флаг для отображения диалога
    onConfirm: (String) -> Unit
) {
    if (showDialog.value) { // Проверяем состояние, чтобы отобразить диалог
        var authorName by remember { mutableStateOf("") }

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDialog.value = false }, // Закрываем диалог при запросе
            title = { Text("Введите имя автора") },
            text = {
                Column {
                    TextField(
                        value = authorName,
                        onValueChange = { authorName = it },
                        label = { Text("Автор") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (authorName.isNotBlank()) {
                            onConfirm(authorName)
                            showDialog.value = false // Закрываем диалог после подтверждения
                        }
                    }
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                Button(onClick = { showDialog.value = false }) { // Закрываем диалог при нажатии "Отмена"
                    Text("Отмена")
                }
            }
        )
    }
}

@Composable
fun MainScreen(
    viewModel: AppViewModel = viewModel(),
    onPickFile: () -> Unit,
    onBookClick: (Book) -> Unit,
) {
    val books by viewModel.books.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)/*.weight(1f)*/,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SearchBar(searchQuery, viewModel)
        BooksList(books, onBookClick = onBookClick, modifier = Modifier) // Передаем onBookClick
        if (books.isEmpty()) {
            Text("Книг не существует в этой вселенной")
        }
        Button(
            onClick = onPickFile
        ) {
            Text("Добавить PDF-файл")
        }
    }
}

@Composable
private fun SearchBar(searchQuery: String, viewModel: AppViewModel) {
    TextField(
        value = searchQuery,
        onValueChange = { query ->
            viewModel.updateSearchQuery(query)
        },
        label = { Text("Search Books") },
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    )
}

@Composable
fun BooksList(books: List<Book>, onBookClick: (Book) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(ScrollState(0))
    ) {
        items(books) { book ->
            BookInfo(book, onClick = onBookClick) // Передаем обработчик клика
        }
    }
}

@Composable
fun BookInfo(book: Book, onClick: (Book) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable { onClick(book) } // Передаем событие клика с книгой
    ) {
        Text("${book.id}")
        Text(book.author)
        Text(book.title)
    }
}