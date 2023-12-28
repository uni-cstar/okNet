package unics.oknet.request

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import okhttp3.Response
import unics.oknet.OkNet
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Create by luochao
 * on 2023/12/26
 * todo 任务队列，目前没有防止同个url重复下载
 */
object OkUseCase {

    @JvmField
    internal val callbacks: SafeRefList = SafeRefList()

    private val downloadingTask: ConcurrentHashMap<String, ProgressInfo?> = ConcurrentHashMap()

    /**
     * 是否正在下载
     */
    fun isDownloading(id: String): Boolean {
        return downloadingTask.containsKey(id)
    }

    /**
     * 获取下载信息
     */
    fun getLoadingProgress(url: String): ProgressInfo? {
        return downloadingTask[url]
    }

    /**
     * 移除监听
     */
    fun removeProgressCallback(callback: ProgressCallback) {
        callbacks.remove(callback)
    }

    /**
     * 添加监听
     */
    fun addProgressCallback(id: String, progressCallback: ProgressCallback) {
        callbacks.add(id, progressCallback)
    }

    /**
     * 移除监听
     */
    fun removeProgressCallback(id: String) {
        callbacks.remove(id)
    }

    suspend fun download(
        url: String,
        file: File
    ): File {
        return download(url, url, file)
    }

    suspend fun download(
        id: String,
        url: String,
        file: File
    ): File {
        try {
            //开始请求前保存进度信息
            downloadingTask[id] = null
            return withContext(Dispatchers.IO) {
                val request = Request.Builder()
                    .url(url)
                    .build()
                val response = OkNet.okHttpClient.newCall(request).execute()
                return@withContext handleDownloadRes(response, file)
            }
        } finally {
            removeTaskFlag(id)
        }
    }

    suspend fun download(
        url: String,
        file: File,
        onProgress: ProgressCallback
    ): File {
        return download(url, url, file, onProgress)
    }

    suspend fun download(
        id: String,
        url: String,
        file: File,
        onProgress: ProgressCallback
    ): File {
        try {
            //开始请求前保存进度信息
            val callbackWrapper = SmartProgressCallback(id)
            downloadingTask[id] = callbackWrapper.progressInfo
            callbacks.add(id, onProgress)
            return withContext(Dispatchers.IO) {
                val request = Request.Builder()
                    .url(url)
                    .tag(DownloadPrgCallback::class.java, callbackWrapper)
                    .build()
                val response = OkNet.okHttpClient.newCall(request).execute()
                return@withContext handleDownloadRes(response, file)
            }
        } finally {
            removeProgressCallback(id)
            removeTaskFlag(id)
        }
    }

    private fun removeTaskFlag(id: String) {
        downloadingTask.remove(id)
    }

    private fun handleDownloadRes(response: Response, file: File): File {
        file.parentFile?.let {
            if (!it.exists()) {
                it.mkdirs()
            }
        }
        file.writeBytes(response.body()!!.bytes())
        return file
    }

}


