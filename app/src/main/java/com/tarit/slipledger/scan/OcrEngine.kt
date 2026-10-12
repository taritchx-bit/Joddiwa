package com.tarit.slipledger.scan

import android.content.Context
import android.graphics.Bitmap
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.Closeable
import java.io.File

/** Thin wrapper over Tesseract with Thai + English models bundled in assets/tessdata. Not thread-safe. */
class OcrEngine(context: Context) : Closeable {
    private val api = TessBaseAPI()

    init {
        val root = File(context.filesDir, "tesseract")
        val tessdata = File(root, "tessdata").apply { mkdirs() }
        for (name in context.assets.list("tessdata").orEmpty()) {
            val target = File(tessdata, name)
            if (target.exists()) continue
            context.assets.open("tessdata/$name").use { input -> target.outputStream().use { input.copyTo(it) } }
        }
        check(api.init(root.absolutePath, "tha+eng")) { "Tesseract init failed" }
        api.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO)
    }

    fun read(bitmap: Bitmap): String {
        api.setImage(bitmap)
        return api.getUTF8Text().orEmpty().also { api.clear() }
    }

    override fun close() = api.recycle()
}
