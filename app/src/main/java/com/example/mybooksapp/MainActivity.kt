package com.example.mybooksapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mybooksapp.ui.theme.MyBooksAppTheme

enum class Theme{
    LIGHT, DARK, SYSTEM
}

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    private var selectedPdfUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.loadBooks()
        setContent {
            MyBooksAppTheme {
                MainScreen(
                    onPickFile = { selectPdfFile() },
                    onBookClick = { book -> onBookClick(book) },
                    onDelete = {book -> deleteBook(book) })
            }
        }
    }

    private val pdfLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

        if (result.resultCode == RESULT_OK) {
            selectedPdfUri = result.data?.data
            selectedPdfUri?.let { uri ->
                val showDialog = mutableStateOf(true)
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
                        onBookClick = { book -> onBookClick(book) },
                        onDelete = {book -> deleteBook(book)}
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
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    name = it.getString(nameIndex)
                }
            }
        }
        return name
    }

    private fun selectPdfFile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
        }
        pdfLauncher.launch(intent)
    }

    fun onBookClick(book: Book) {
        val intent = Intent(this, PdfViewerActivity::class.java).apply {
            putExtra("pdfUri", book.filePath)
        }
        startActivity(intent)
    }

    private fun deleteBook(book: Book) {
        // Удаляем из базы данных
        val dbHelper = DatabaseHelper(this)
        val success = dbHelper.deleteBook(book.id)

        if (success) {
            // Пробуем удалить файл, если он существует
            try {
                val uri = Uri.parse(book.filePath)
                contentResolver.delete(uri, null, null)
            } catch (e: Exception) {
                // Игнорируем ошибки при удалении файла
            }

            // Обновляем список книг
            viewModel.loadBooks()

            Toast.makeText(this, "Книга успешно удалена", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Ошибка при удалении книги", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun AuthorInputDialog(
    showDialog: MutableState<Boolean>,
    onConfirm: (String) -> Unit
) {
    if (showDialog.value) {
        var authorName by remember { mutableStateOf("") }

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDialog.value = false },
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
                            showDialog.value = false
                        }
                    }
                ) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                Button(onClick = { showDialog.value = false }) {
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
    onDelete: (Book) -> Unit
) {
    var currentTheme by remember { mutableStateOf(Theme.SYSTEM) }
    val books by viewModel.books.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    MyBooksAppTheme(darkTheme = when (currentTheme) {
        Theme.LIGHT -> false
        Theme.DARK -> true
        Theme.SYSTEM -> isSystemInDarkTheme()
    }) {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onPickFile,
                    containerColor = Color(0xFF6A1B9A),
                    modifier = Modifier.padding(bottom = 32.dp, end=50.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Добавить PDF-файл", tint = Color.White)
                }
            }
        )
        { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SearchBar(searchQuery, viewModel)
                ThemeSwitcher(
                    onLightThemeSelected = { currentTheme = Theme.LIGHT },
                    onDarkThemeSelected = { currentTheme = Theme.DARK },
                    onSystemThemeSelected = { currentTheme = Theme.SYSTEM },
                    selectedTheme = currentTheme.name
                )
                BooksList(books, onBookClick = onBookClick, onDelete = onDelete, modifier = Modifier)
                if (books.isEmpty()) {
                    Text("Книг не существует в этой вселенной")
                }
            }

        }
    }


}

@Composable
fun ThemeSwitcher(
    onLightThemeSelected: () -> Unit,
    onDarkThemeSelected: () -> Unit,
    onSystemThemeSelected: () -> Unit,
    selectedTheme: String
) {
    Row(
        modifier = Modifier
            .padding(16.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.Gray)
            .height(48.dp)
            .fillMaxWidth()
    ) {
        Button(
            onClick = onLightThemeSelected,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selectedTheme == "Light") Color.Yellow else Color.White
            )
        ) {
            Text("Light")
        }
        Button(
            onClick = onDarkThemeSelected,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selectedTheme == "Dark") Color.Yellow else Color.Black
            )
        ) {
            Text("Dark", color = Color.White)
        }
        Button(
            onClick = onSystemThemeSelected,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selectedTheme == "System") Color.Yellow else Color.LightGray
            )
        ) {
            Text("System")
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
fun BooksList(books: List<Book>, onBookClick: (Book) -> Unit, onDelete: (Book) -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
    ) {
        items(books) { book ->
            BookInfo(book, onClick = onBookClick, onDelete = onDelete)
            HorizontalDivider() // Добавляем линию после каждого элемента
        }
    }
}

@Composable
fun BookInfo(book: Book, onClick: (Book) -> Unit, onDelete: (Book) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable { onClick(book) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f) // Занимает доступное пространство
        ) {
            Text("${book.id}")
            Text(book.author)
            Text(book.title)
        }
        IconButton(onClick = { onDelete(book) }) {
            Icon(
                imageVector = Icons.Default.Delete, // Использует стандартную иконку удаления
                contentDescription = "Delete"
            )
        }
    }
}