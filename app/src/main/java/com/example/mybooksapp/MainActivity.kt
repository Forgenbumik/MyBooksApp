package com.example.mybooksapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.mybooksapp.ui.theme.MyBooksAppTheme
import androidx.compose.ui.Alignment
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.testa.PdfViewerActivity

class MainActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()
    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            openPdfViewer(it)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {

            MyBooksAppTheme {
                val message = remember {mutableStateOf("")}
                MainScreen(onPickFile = { pickFileLauncher.launch("application/pdf") },
                    searching = message
                )
            }
        }
    }

    private fun openPdfViewer(uri: Uri) {
        val intent = Intent(this, PdfViewerActivity::class.java).apply {
            putExtra("pdfUri", uri)
        }
        startActivity(intent)
    }
}

@Composable
fun MainScreen(viewModel: AppViewModel = viewModel(), onPickFile: () -> Unit, searching: MutableState<String>) {
    val books = viewModel.listbooks.observeAsState().value
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SearchBar(searching)
        if (books != null) {
            BooksList(books.ListBooks)
        }
        Button(onClick = onPickFile) {
            Text("Выбрать PDF файл")
        }
    }
}

@Composable
fun SearchBar(searching: MutableState<String>) {
    TextField(
        value = "Книга",
        onValueChange = {newText -> SearchOnClick(newText, searching)}
    )
}

fun SearchOnClick(newText: String, Searching: MutableState<String>) {
    Searching.value = newText

}

@Composable
fun BooksList(books: List<Book>) {
    LazyColumn {
        items()
    }
}

@Composable
fun BookInfo(book: Book) {
    Text(book.author)
    Text(book.title)
}