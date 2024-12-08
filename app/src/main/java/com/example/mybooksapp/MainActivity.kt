package com.example.mybooksapp

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ListView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
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

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    private var selectedPdfUri: Uri? = null
    private lateinit var bookListView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        viewModel.loadBooks()
        setContent {
            MainScreen(onPickFile = { selectPdfFile() })
        }
    }

    private val pdfLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedPdfUri = result.data?.data
            selectedPdfUri?.let { uri ->
                val pdfBytes = readPdfFile(uri)
                //viewModel.addBook()
            }
        } else {
            Toast.makeText(this, "Не удалось выбрать файл", Toast.LENGTH_SHORT).show()
        }
    }

    private fun selectPdfFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/pdf"
        }
        pdfLauncher.launch(intent)
    }

    private fun readPdfFile(uri: Uri): ByteArray {
        contentResolver.openInputStream(uri)?.use { inputStream ->
            return inputStream.readBytes()
        } ?: throw Exception("Не удалось прочитать файл")
    }
}

@Composable
fun MainScreen(viewModel: AppViewModel = viewModel(), onPickFile: () -> Unit) {

    val books by viewModel.books.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Поле ввода для поиска
        SearchBar(searchQuery, viewModel)
        BooksList(books)
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
fun BooksList(books: List<String>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        items(books) { bookTitle ->
            Text(
                text = bookTitle,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
            /*Button(modifier = Modifier.fillMaxSize(),
                onClick = )*/
        }
    }
}

fun getBookById() {

}