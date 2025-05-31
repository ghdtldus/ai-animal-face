package com.example.android.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import java.io.File

object ImageUtils {

    // 1. Uri → Bitmap 변환 + 리사이즈
    fun getCompressedBitmap(context: Context, uri: Uri, maxSize: Int = 1024): Bitmap? {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null

        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeStream(inputStream, null, options)
        inputStream.close()

        val originalWidth = options.outWidth
        val originalHeight = options.outHeight

        val scale = calculateInSampleSize(options, maxSize, maxSize)

        val newOptions = BitmapFactory.Options().apply {
            inSampleSize = scale
        }

        val imageStream = context.contentResolver.openInputStream(uri) ?: return null
        val bitmap = BitmapFactory.decodeStream(imageStream, null, newOptions)
        imageStream.close()

        val finalBitmap = bitmap?.let { rotateIfRequired(context, uri, it) }

        finalBitmap?.let {
            val isCompressed = (scale > 1)
            val isRotated = it.width != bitmap?.width || it.height != bitmap?.height

            android.util.Log.d("ImageUtils", "압축 여부: $isCompressed, 회전 여부: $isRotated")
            android.util.Log.d("ImageUtils", "최종 크기: ${it.width}x${it.height}")
        }

        return finalBitmap
    }


    // 2. EXIF 회전 보정
    private fun rotateIfRequired(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return bitmap
        val exif = ExifInterface(inputStream)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        inputStream.close()

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
            else -> bitmap
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = android.graphics.Matrix().apply {
            postRotate(degrees)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    // 3. 압축 수준 계산용 함수
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight &&
                (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    // 4. Bitmap → File 저장
    fun bitmapToFile(context: Context, bitmap: Bitmap, fileName: String = "compressed_upload.jpg"): File {
        val file = File(context.cacheDir, fileName)
        file.outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)
        }
        return file
    }
}
