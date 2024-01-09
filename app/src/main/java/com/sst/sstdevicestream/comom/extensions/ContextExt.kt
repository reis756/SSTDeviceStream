package com.reisdeveloper.listeneraccuracy.base.ext

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import java.io.File
import java.net.InetAddress
import java.net.UnknownHostException

const val UBER_AVATAR_MIME_TYPE = "jpeg"
const val UBER_AVATAR_FILENAME = "UberAvatar"

@SuppressLint("HardwareIds")
fun Context.getAndroidId(): String {
    val contentResolver: ContentResolver = contentResolver
    return Settings.Secure.getString(
        contentResolver,
        Settings.Secure.ANDROID_ID
    )
}

fun Context.getProviderPackage(): String =
    "$packageName.provider"

fun Context.getVersionName(): String =
    packageManager.getPackageInfo(packageName, 0).versionName

fun Context.shareReceiptIntent(uri: Uri, title: String, imageMimeType: String = "image/*") {
    Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        type = imageMimeType
    }.run {
        startActivity(
            Intent.createChooser(
                this,
                title
            )
        )
    }
}

suspend fun isNetworkConnected(): Boolean =
    try {
        withContext(IO) {
            val address = InetAddress.getByName("www.google.com")
            address.hostName.isNotEmpty()
        }
    } catch (e: UnknownHostException) {
        false
    }

/**
 * Get URI to image received from capture by camera.
 */
fun Context.getImageOutputUri(fileName: String, mimeType: String): Uri? {
    var outputFileUri: Uri? = null
    val getImage = externalCacheDir
    if (getImage != null) {
        outputFileUri = Uri.fromFile(File(getImage.path, "$fileName.$mimeType"))
    }
    return outputFileUri
}

fun Context.getImageUri(data: Intent?): Uri? {
    var isCamera = true

    if (data != null) {
        isCamera = data.action != null && data.action == MediaStore.ACTION_IMAGE_CAPTURE
    }

    return if (isCamera) getImageOutputUri(UBER_AVATAR_FILENAME, UBER_AVATAR_MIME_TYPE)
    else data?.data
}