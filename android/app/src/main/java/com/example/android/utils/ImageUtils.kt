package com.example.android.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.IOException

object ImageUtils {

    // 1. Uri → Bitmap 변환 + 리사이즈 + 회전 보정 + 로그
    fun getCompressedBitmap(context: Context, uri: Uri, maxSize: Int = 1024): Bitmap? {
        // 🔹 1-1. 파일 크기 검사 (예: 5MB 초과 시 오류 발생)
        val fileSizeMB = (context.contentResolver.openFileDescriptor(uri, "r")?.statSize ?: 0L) / (1024 * 1024)
        if (fileSizeMB > 5) {
            throw IOException("이미지 용량이 너무 큽니다. 5MB 이하 파일을 업로드해주세요.")
        }

        // 🔹 1-2. 이미지 크기 측정 (압축 여부 판단용)
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeStream(inputStream, null, options)
        inputStream.close()

        val scale = calculateInSampleSize(options, maxSize, maxSize)
        val isCompressed = (scale > 1)

        // 🔹 1-3. 실제 Bitmap 디코딩
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = scale }
        val imageStream = context.contentResolver.openInputStream(uri) ?: return null
        val bitmap = BitmapFactory.decodeStream(imageStream, null, decodeOptions)
        imageStream.close()

        // 🔹 1-4. 회전 보정 (회전 여부 함께 반환)
        val (finalBitmap, isRotated) = bitmap?.let { rotateIfRequired(context, uri, it) } ?: return null

        // 🔹 1-5. 로그 출력
        Log.d("ImageUtils", "압축 여부: $isCompressed, 회전 여부: $isRotated")
        Log.d("ImageUtils", "최종 크기: ${finalBitmap.width}x${finalBitmap.height}")

        return finalBitmap
    }

    // 2. EXIF 회전 보정 (회전 여부 함께 반환)
    private fun rotateIfRequired(context: Context, uri: Uri, bitmap: Bitmap): Pair<Bitmap, Boolean> {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return Pair(bitmap, false)
        val exif = ExifInterface(inputStream)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        inputStream.close()

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> Pair(rotateBitmap(bitmap, 90f), true)
            ExifInterface.ORIENTATION_ROTATE_180 -> Pair(rotateBitmap(bitmap, 180f), true)
            ExifInterface.ORIENTATION_ROTATE_270 -> Pair(rotateBitmap(bitmap, 270f), true)
            else -> Pair(bitmap, false)
        }
    }

    // 3. Bitmap 회전
    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = android.graphics.Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    // 4. 압축 비율 계산
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    // 5. Bitmap → File 저장
    fun bitmapToFile(context: Context, bitmap: Bitmap, fileName: String = "compressed_upload.jpg"): File {
        val file = File(context.cacheDir, fileName)
        file.outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)
        }
        return file
    }
}
