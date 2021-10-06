package com.linkedin.android.litr.demo.data

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.WorkerThread
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Provides interaction between locally-stored media files and the [MediaStore].
 * Copies video file and creates necessary entries to make video discoverable by the user, surviving application re-installs.
 *
 * @param context The context, used to obtain a [ContentResolver]
 * @param executor The [ExecutorService] to use for fire-and-forget IO
 * @param handler The handler to which [MediaPublishedListener] events are posted
 */
class SharedMediaStoragePublisher @JvmOverloads constructor(
    private val context: Context,
    private val executor: ExecutorService = Executors.newSingleThreadExecutor(),
    private val handler: Handler = Handler(Looper.getMainLooper())
) {

    private val resolver: ContentResolver
        get() = context.contentResolver

    fun publish(file: File, keepOriginal: Boolean, listener: MediaPublishedListener) {
        executor.execute {
            val contentUri = storeVideoInternal(file, keepOriginal)
            handler.post {
                listener.onPublished(file, contentUri)
            }
        }
    }

    @WorkerThread
    private fun storeVideoInternal(file: File, keepOriginal: Boolean): Uri? {
        if (!file.exists()) return null

        val newFileName = file.name

        val values = ContentValues().apply {
            put(MediaStore.Video.Media.MIME_TYPE, MIME_TYPE_MPEG_4)
            put(MediaStore.Video.Media.TITLE, newFileName)
            put(MediaStore.Video.Media.DISPLAY_NAME, newFileName)
            put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis())
            if (isAndroidQ) {
                put(MediaStore.Video.Media.RELATIVE_PATH, relativePathMovies)
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }


        val collectionUrl = if (isAndroidQ) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        var contentUri: Uri? = null

        runCatching {
            resolver.insert(collectionUrl, values)?.also { insertedUri ->
                contentUri = insertedUri
                resolver.openOutputStream(insertedUri)?.use { output ->
                    FileInputStream(file).use { input ->
                        input.copyTo(output)
                    }
                } ?: throw IOException("Failed to copy video to output stream")
            } ?: throw IOException("Failed to insert video to MediaStore")

        }.onFailure {
            Log.e(TAG, "Error copying file to MediaStore", it)
            contentUri?.let { copyFailedUri ->
                resolver.delete(copyFailedUri, null, null)
            }
        }

        if (!keepOriginal) {
            runCatching { file.delete() }.onFailure {
                Log.e(TAG, "Unable to delete original video file $file from system", it)
            }
        }

        if (isAndroidQ) {
            contentUri?.let {
                // Since we're done writing to the Uri, this tells MediaStore that other apps can use the content now.
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                runCatching {
                    resolver.update(it, values, null, null)
                }.onFailure {
                    Log.e(TAG, "Could not update MediaStore for $file", it)
                }
            }
        }

        return contentUri
    }

    interface MediaPublishedListener {
        fun onPublished(file: File, contentUri: Uri?)
    }

    companion object {
        private val TAG = SharedMediaStoragePublisher::class.qualifiedName
        private const val MIME_TYPE_MPEG_4 = "video/mp4"

        private val isAndroidQ
            @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.Q)
            get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q


        private val moviesDirectory = Environment.DIRECTORY_MOVIES
        private val litrDirectory = "LiTr"
        private val relativePathMovies = "${moviesDirectory}/${litrDirectory}"

    }
}