package com.khadmati.app.data.qr

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

/** Renders content into a print-ready QR bitmap via ZXing. */
object QrGenerator {

    fun bitmap(content: String, size: Int = 720): Bitmap {
        val bits = QRCodeWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            size,
            size,
            mapOf(EncodeHintType.MARGIN to 1),
        )
        val pixels = IntArray(size * size) { index ->
            if (bits.get(index % size, index / size)) Color.BLACK else Color.WHITE
        }
        return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
    }
}
