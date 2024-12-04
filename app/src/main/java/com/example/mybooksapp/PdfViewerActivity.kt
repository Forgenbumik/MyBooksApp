package com.example.testa

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import java.io.FileNotFoundException
import kotlin.math.max
import kotlin.math.min

class PdfViewerActivity : ComponentActivity() {
    private lateinit var pdfRenderer: PdfRenderer
    private var currentPageIndex by mutableStateOf(0)
    private var totalPages by mutableStateOf(0)
    private var scale by mutableStateOf(1f) // Масштабирование

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Получаем URI PDF из Intent
        val uri: Uri? = intent.getParcelableExtra("pdfUri")
        uri?.let {
            try {
                // Открываем ParcelFileDescriptor
                val parcelFileDescriptor = contentResolver.openFileDescriptor(it, "r")
                parcelFileDescriptor?.let { pfd ->
                    // Создаем PdfRenderer
                    pdfRenderer = PdfRenderer(pfd)
                    totalPages = pdfRenderer.pageCount
                    setContent {
                        PdfViewerScreen()
                    }
                }
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
                // Обработка ошибки: файл не найден
            }
        }
    }

    @Composable
    fun PdfViewerScreen() {
        // Кэшируем битмапы для страниц
        val bitmapCache = remember { mutableStateMapOf<Int, Bitmap>() }
        val page = pdfRenderer.openPage(currentPageIndex)

        // Проверяем, есть ли уже закэшированный битмап
        val bitmap = bitmapCache[currentPageIndex] ?: run {
            // Увеличиваем разрешение битмапа в зависимости от масштаба
            val bitmapWidth = (page.width * scale * 2).toInt() // Увеличиваем разрешение в 2 раза
            val bitmapHeight = (page.height * scale * 2).toInt() // Увеличиваем разрешение в 2 раза
            val newBitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
            // Используем RENDER_MODE_FOR_PRINT для лучшего качества
            page.render(newBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
            bitmapCache[currentPageIndex] = newBitmap // Кэшируем битмап
            newBitmap
        }
        page.close()

        // Состояние для хранения смещения
        var offsetX by remember { mutableStateOf(0f) }
        var offsetY by remember { mutableStateOf(0f) }

        Box(modifier = Modifier.fillMaxSize()) {
            // Отображаем изображение с возможностью масштабирования
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = min(max(scale * zoom, 1f), 5f) // Ограничиваем масштаб от 1x до 5x

                            // Обновляем смещение в зависимости от координат касания
                            offsetX += pan.x
                            offsetY += pan.y

                            // Ограничиваем смещение по X
                            val maxOffsetX = (bitmap.width * scale - size.width).coerceAtLeast(0f)
                            offsetX = offsetX.coerceIn(-maxOffsetX, 0f)

                            // Ограничиваем смещение по Y
                            val maxOffsetY = (bitmap.height * scale - size.height).coerceAtLeast(0f)
                            offsetY = offsetY.coerceIn(-maxOffsetY, 0f)
                        }
                    }
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offsetX,
                        translationY = offsetY
                    )
            )

            // Кнопки для навигации
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = { if (currentPageIndex > 0) {
                    currentPageIndex--
                    bitmapCache.remove(currentPageIndex + 1) // Удаляем кэш следующей страницы
                }}) {
                    Text("<-") // Стрелка влево
                }
                Button(onClick = { if (currentPageIndex < totalPages - 1) {
                    currentPageIndex++
                    bitmapCache.remove(currentPageIndex - 1) // Удаляем кэш предыдущей страницы
                }}) {
                    Text("->") // Стрелка вправо
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Закрываем PdfRenderer при уничтожении активности
        pdfRenderer.close()
    }
}