package com.example.mybooksapp

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
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
    private var scale by mutableStateOf(1f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri: Uri? = intent.getStringExtra("pdfUri")?.let { Uri.parse(it) }
        uri?.let {
            try {
                val parcelFileDescriptor = contentResolver.openFileDescriptor(it, "r")
                parcelFileDescriptor?.let { pfd ->
                    pdfRenderer = PdfRenderer(pfd)
                    totalPages = pdfRenderer.pageCount
                    setContent {
                        PdfViewerScreen()
                    }
                }
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }
        }
    }

    @Composable
    fun PdfViewerScreen() {
        val bitmapCache = remember { mutableStateMapOf<Int, Bitmap>() }
        val page = pdfRenderer.openPage(currentPageIndex)

        val bitmap = bitmapCache[currentPageIndex] ?: run {
            val bitmapWidth = (page.width * scale * 2).toInt()
            val bitmapHeight = (page.height * scale * 2).toInt()
            val newBitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
            page.render(newBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
            bitmapCache[currentPageIndex] = newBitmap
            newBitmap
        }
        page.close()

        var offsetX by remember { mutableStateOf(0f) }
        var offsetY by remember { mutableStateOf(0f) }

        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = min(max(scale * zoom, 1f), 5f)

                            offsetX += pan.x
                            offsetY += pan.y

                            val maxOffsetX = (bitmap.width * scale - size.width).coerceAtLeast(0f)
                            offsetX = offsetX.coerceIn(-maxOffsetX, 0f)

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

            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(onClick = { if (currentPageIndex > 0) {
                    currentPageIndex--
                    bitmapCache.remove(currentPageIndex + 1)
                }}) {
                    Text("<-")
                }
                Button(onClick = { if (currentPageIndex < totalPages - 1) {
                    currentPageIndex++
                    bitmapCache.remove(currentPageIndex - 1)
                }}) {
                    Text("->")
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::pdfRenderer.isInitialized) {
            pdfRenderer.close()
        }
    }
}