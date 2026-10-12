package com.tarit.slipledger.scan

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.tarit.slipledger.data.Prefs
import com.tarit.slipledger.data.Repository
import com.tarit.slipledger.data.ScannedImage
import com.tarit.slipledger.data.Tx
import com.tarit.slipledger.data.TxSource
import com.tarit.slipledger.data.TxType
import com.tarit.slipledger.parse.SlipParser
import com.tarit.slipledger.parse.SlipQr
import com.tarit.slipledger.parse.SlipQrParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class ScanProgress(val done: Int, val total: Int, val found: Int)

/**
 * Finds bank slips among gallery images. Every Thai slip carries a verification QR code, so images
 * without a QR are skipped cheaply with ML Kit; only the rest go through the slower Tesseract OCR.
 */
class SlipScanner(private val context: Context) {
    private val repo = Repository.get(context)
    private val prefs = Prefs.get(context)

    private data class Image(val id: Long, val uri: Uri, val takenAt: Long, val width: Int, val height: Int)

    /** Returns the number of new transactions recorded. */
    suspend fun scan(onProgress: (ScanProgress) -> Unit = {}): Int = mutex.withLock {
        withContext(Dispatchers.Default) {
            if (!hasPermission(context)) return@withContext 0
            val seen = repo.scannedImages.allIds().toHashSet()
            val images = queryImages(prefs.settings.value.scanFrom).filter { it.id !in seen }
            if (images.isEmpty()) return@withContext 0

            val qrScanner = BarcodeScanning.getClient(
                BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
            )
            var ocr: OcrEngine? = null
            var found = 0
            try {
                images.forEachIndexed { index, image ->
                    ensureActive()
                    onProgress(ScanProgress(index, images.size, found))
                    val isSlip = runCatching {
                        val engine = ocr ?: OcrEngine(context).also { ocr = it }
                        processImage(image, qrScanner, engine)
                    }.onFailure { Log.w(TAG, "Failed on ${image.uri}", it) }.getOrDefault(false)
                    if (isSlip) found++
                    repo.scannedImages.insert(ScannedImage(image.id, isSlip, System.currentTimeMillis()))
                }
                onProgress(ScanProgress(images.size, images.size, found))
            } finally {
                ocr?.close()
                qrScanner.close()
            }
            found
        }
    }

    /** Returns true when a new transaction was recorded from this image. */
    private suspend fun processImage(
        image: Image,
        qrScanner: BarcodeScanner,
        ocr: OcrEngine,
    ): Boolean {
        // Slips are portrait screenshots; skip landscape photos without decoding them.
        if (image.width > 0 && image.height > 0 && image.width > image.height * 1.2) return false

        val bitmap = decode(image.uri, maxSide = 2400) ?: return false
        try {
            val qrValues = qrScanner.process(InputImage.fromBitmap(bitmap, 0)).await().mapNotNull { it.rawValue }
            Log.d(TAG, "${image.uri}: ${qrValues.size} QR ${qrValues.firstOrNull()?.take(80)}")
            if (qrValues.isEmpty()) return false
            val qr: SlipQr? = qrValues.firstNotNullOfOrNull { SlipQrParser.parse(it) }

            val owners = prefs.settings.value.ownerNames
            var text = ocr.read(scaleWidth(bitmap, 1200))
            var slip = SlipParser.parse(text, owners, qr)
            if (slip == null) {
                // Bright coloured text (e.g. Dime's big green amount) is about as light as the white
                // background, so Tesseract's binarisation erases it. Darkest-channel grey makes it black.
                text += "\n" + ocr.read(darkestChannel(scaleWidth(bitmap, 1200)))
                slip = SlipParser.parse(text, owners, qr)
            }
            Log.d(TAG, "OCR -> $slip\n$text")
            if (slip == null) return false

            val timestamp = slip.timestamp?.let { printed ->
                // The slip may only show a date; then the screenshot time gives a better time of day.
                val printedDate = Instant.ofEpochMilli(printed).atZone(ZoneId.systemDefault())
                val takenDate = Instant.ofEpochMilli(image.takenAt).atZone(ZoneId.systemDefault())
                val midnight = printedDate.toLocalTime().toSecondOfDay() == 0
                if (midnight && printedDate.toLocalDate() == takenDate.toLocalDate()) image.takenAt else printed
            } ?: image.takenAt

            val counterparty = if (slip.type == TxType.INCOME) slip.from else slip.to
            // The QR names the sending bank, so only the paying side can create a wallet automatically.
            val walletId = when (slip.type) {
                TxType.INCOME -> repo.resolveWallet("", slip.toAccount, create = false)
                else -> repo.resolveWallet(slip.bank, slip.fromAccount, create = true)
            }
            val toWalletId = if (slip.type == TxType.TRANSFER) repo.resolveWallet("", slip.toAccount, create = false) else null
            return repo.addIfNew(
                Tx(
                    type = slip.type,
                    amountSatang = slip.amountSatang,
                    timestamp = timestamp,
                    counterparty = counterparty,
                    bank = slip.bank,
                    category = repo.learnedCategory(counterparty)
                        ?.takeIf { slip.type != TxType.TRANSFER } ?: slip.category,
                    source = TxSource.SLIP,
                    sourceKey = qr?.transRef?.let { "ref:$it" } ?: "img:${image.id}",
                    imageUri = image.uri.toString(),
                    rawText = text.take(2000),
                    account = if (slip.type == TxType.INCOME) slip.toAccount else slip.fromAccount,
                    walletId = walletId,
                    toWalletId = toWalletId,
                    // OCR could not read everything: let the user confirm it in the review inbox.
                    needsReview = slip.timestamp == null || counterparty.isBlank() || walletId == null,
                )
            )
        } finally {
            bitmap.recycle()
        }
    }

    private fun queryImages(fromMillis: Long): List<Image> {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
        )
        val out = mutableListOf<Image>()
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            "${MediaStore.Images.Media.DATE_ADDED} >= ?",
            arrayOf((fromMillis / 1000).toString()),
            "${MediaStore.Images.Media.DATE_ADDED} DESC",
        )?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val takenCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val addedCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val wCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val hCol = c.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val taken = c.getLong(takenCol).takeIf { it > 0 } ?: (c.getLong(addedCol) * 1000)
                out += Image(
                    id = id,
                    uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id),
                    takenAt = taken,
                    width = c.getInt(wCol),
                    height = c.getInt(hCol),
                )
            }
        }
        return out
    }

    private fun decode(uri: Uri, maxSide: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    }

    /** Tesseract reads normal slip text best around 1200px wide. */
    private fun scaleWidth(bitmap: Bitmap, target: Int): Bitmap {
        if (bitmap.width in (target * 0.8).toInt()..(target * 1.25).toInt()) return bitmap
        val h = (bitmap.height * target.toFloat() / bitmap.width).toInt()
        return Bitmap.createScaledBitmap(bitmap, target, h, true)
    }

    private fun darkestChannel(src: Bitmap): Bitmap {
        val px = IntArray(src.width * src.height)
        src.getPixels(px, 0, src.width, 0, 0, src.width, src.height)
        for (i in px.indices) {
            val c = px[i]
            val v = minOf((c shr 16) and 0xFF, (c shr 8) and 0xFF, c and 0xFF)
            px[i] = (0xFF shl 24) or (v shl 16) or (v shl 8) or v
        }
        return Bitmap.createBitmap(px, src.width, src.height, Bitmap.Config.ARGB_8888)
    }

    companion object {
        private const val TAG = "SlipScanner"
        private val mutex = Mutex()

        val imagePermission: String =
            if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_IMAGES
            else Manifest.permission.READ_EXTERNAL_STORAGE

        fun hasPermission(context: Context): Boolean =
            context.checkSelfPermission(imagePermission) == PackageManager.PERMISSION_GRANTED
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
}
