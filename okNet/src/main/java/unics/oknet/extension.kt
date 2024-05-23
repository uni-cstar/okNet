package unics.oknet

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import okhttp3.OkHttpClient
import unics.oknet.okhttp.OkDomain

private const val TAG = "OkNet"

inline fun OkHttpClient.Builder.addOkDomain(baseUrl: String): OkHttpClient.Builder {
    OkDomain.useOkDomain(this, baseUrl)
    return this
}

inline fun <reified T : Any> apiService(cacheable: Boolean = true): T {
    return OkNet.createApiService(T::class.java, cacheable)
}

@SuppressLint("ObsoleteSdkInt")
internal var isAndroidPlatform: Boolean = run {
    try {
        Class.forName("android.os.Build")
        Build.VERSION.SDK_INT != 0
    } catch (ignored: ClassNotFoundException) {
        false
    }
}

internal fun isDependOn(className: String): Boolean {
    return try {
        Class.forName(className)
        true
    } catch (e: Throwable) {
        e.printStackTrace()
        false
    }
}

/**
 * 采用这种方法（内联），在使用的时候，可以避免字符串的创建
 */
internal inline fun logd(msgCreator: () -> String) {
    if (!OkNet.DEBUG)
        return
    if (isAndroidPlatform) {
        Log.d(TAG, msgCreator.invoke())
    } else {
        println("${TAG}:${msgCreator.invoke()}")
    }
}

internal inline fun logw(msgCreator: () -> String) {
    if (!OkNet.DEBUG)
        return
    if (isAndroidPlatform) {
        Log.w(TAG, msgCreator.invoke())
    } else {
        println("${TAG}:${msgCreator.invoke()}")
    }
}