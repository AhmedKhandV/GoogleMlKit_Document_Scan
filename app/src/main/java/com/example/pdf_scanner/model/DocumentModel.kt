package com.example.pdf_scanner.model

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

class DocumentModel(private val activity: Activity) {

    private val options = GmsDocumentScannerOptions.Builder()
        .setGalleryImportAllowed(false)
        .setPageLimit(2)
        .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG, GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
        .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
        .build()

    private val scanner = GmsDocumentScanning.getClient(options)

    var callback: ScanResultCallback? = null
    private var scannerLauncher: ActivityResultLauncher<IntentSenderRequest>? = null

    fun setScanLauncher(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        this.scannerLauncher = launcher
    }

    fun startScanning() {
        scanner.getStartScanIntent(activity).addOnSuccessListener { intentSender ->
            scannerLauncher?.launch(IntentSenderRequest.Builder(intentSender).build())
        }
    }

    fun saveImageToLocalStorage(imageUri: Uri?): Uri? {
        imageUri?.let { uri ->
            val inputStream: InputStream? = activity.contentResolver.openInputStream(uri)
            val imageFile = getOutputFile("scanned_image_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.jpg")
            return try {
                FileOutputStream(imageFile).use { outputStream ->
                    inputStream?.copyTo(outputStream)
                }
                Uri.fromFile(imageFile)
            } catch (e: Exception) {
                Log.e("DocumentModel", "Error saving image: ${e.message}", e)
                null
            } finally {
                inputStream?.close()
            }
        }
        return null
    }

    fun savePdfToLocalStorage(pdfUri: Uri): Uri? {
        val contentResolver = activity.contentResolver
        val inputStream = contentResolver.openInputStream(pdfUri)
        val pdfFile = getOutputFile("scanned_document_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.pdf")
        return try {
            FileOutputStream(pdfFile).use { outputStream ->
                inputStream?.copyTo(outputStream)
            }
            Uri.fromFile(pdfFile)
        } catch (e: Exception) {
            Log.e("DocumentModel", "Error saving PDF: ${e.message}", e)
            null
        } finally {
            inputStream?.close()
        }
    }

    private fun getOutputFile(fileName: String): File {
        val directory = File(activity.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "PDF_Scanner")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return File(directory, fileName)
    }

    interface ScanResultCallback {
        fun onScanResult(pages: List<Uri>?, pdfUri: Uri?, pageCount: Int?)
        fun onScanError(message: String)
    }
}
