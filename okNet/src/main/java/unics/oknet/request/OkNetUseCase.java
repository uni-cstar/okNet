package unics.oknet.request;


import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.Okio;
import retrofit2.Call;
import unics.oknet.OkNet;

/**
 * Create by luochao
 * on 2023/12/26
 * <p>
 * todo 待处理问题：如何预防客户端没有调用remove导致内存泄露？进行中的任务取消支持
 */
public class OkNetUseCase {

    CallbackList callbacks = new CallbackList();

    //请求中的任务信息
    HashMap<String, ProgressInfo> runningInfo = new HashMap<>();

    private final ExecutorService executor;


    private OkNetUseCase() {
        // CPU的数量
        int cpuCount = Runtime.getRuntime().availableProcessors();
        // 线程池的大小
        int poolSize = cpuCount;
        // 任务队列的长度
        int queueCapacity = 10;
        executor = new ThreadPoolExecutor(
                poolSize,
                poolSize,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(queueCapacity),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    private static class SingleTone {
        private static final OkNetUseCase instance = new OkNetUseCase();
    }

    @NotNull
    public static OkNetUseCase getInstance() {
        return SingleTone.instance;
    }

    /**
     * 是否正在下载
     */
    public boolean isRunning(@NotNull String id) {
        return runningInfo.containsKey(id);
    }

    /**
     * 获取下载信息
     */
    @Nullable
    public ProgressInfo getRunningInfo(@NotNull String id) {
        return runningInfo.get(id);
    }

    /**
     * 移除监听
     */
    public void removeCallback(@NotNull ProgressCallback callback) {
        callbacks.remove(callback);
    }

    /**
     * 移除监听
     *
     * @param id 主键
     */
    public void removeCallback(@NotNull String id) {
        callbacks.remove(id);
    }

    /**
     * 添加监听
     */
    public void addCallback(@NotNull String id, @NotNull ProgressCallback progressCallback) {
        callbacks.add(id, progressCallback);
    }

    public boolean containsCallback(@NotNull ProgressCallback callback) {
        return callbacks.contains(callback);
    }

    /**
     * 下载文件
     *
     * @param url  下载地址
     * @param file 保存的文件
     */
    @NotNull
    public File download(
            @NotNull String url,
            @NotNull File file) throws IOException {
        return download(url, file, null);
    }

    @NotNull
    public File download(
            @NotNull String url,
            @NotNull File file,
            @Nullable ProgressCallback callback) throws IOException {
        String id = String.valueOf(System.nanoTime());
        try {
            if (!notifyCallbackOnlyIfRunning(id, url, callback)) {
                throw new RuntimeException("已存在相同任务");
            }
            Call<ResponseBody> call = createCall(id, url, callback);
            retrofit2.Response<ResponseBody> response = call.execute();
            assert response.body() != null;
            return writeToFile(response.body(), file);
        } finally {
            removeRunningCall(id);
        }
    }

    /**
     * 入队文件下载请求
     *
     * @param url
     * @param file
     * @param callback
     */
    public void downloadEnqueue(
            @NotNull String url,
            @NotNull File file,
            @NotNull FileDownloadCallback callback
    ) {
        downloadEnqueue(url, url, file, callback);
    }

    /**
     * 入队下载文件请求
     *
     * @param id       任务唯一键
     * @param url      下载地址
     * @param file     保存的文件
     * @param callback 回调
     */
    public void downloadEnqueue(
            @NotNull String id,
            @NotNull String url,
            @NotNull File file,
            @NotNull FileDownloadCallback callback
    ) {
        try {
            if (notifyCallbackOnlyIfRunning(id, url, callback))
                return;
            Call<ResponseBody> call = createCall(id, url, callback);
            callback.onStart(url);
            executor.submit(() -> {
                try {
                    ResponseBody body = call.execute().body();
                    assert body != null;
                    writeToFile(body, file);
                    CallbackHolder holder = callbacks.get(id);
                    if (holder != null)
                        holder.onComplete(url, file);
                } catch (Exception e) {
                    e.printStackTrace();
                    CallbackHolder holder = callbacks.get(id);
                    if (holder != null)
                        holder.onError(url, e);
                } finally {
                    removeRunningCall(id);
                }
            });

        } catch (Throwable e) {
            e.printStackTrace();

            CallbackHolder holder = callbacks.get(id);
            if (holder != null)
                holder.onError(url, e);
            removeRunningCall(id);
        }
    }

    /**
     * 如果任务已经运行，只通知回调不做任务下载
     *
     * @param id       任务id
     * @param callback 回调
     * @return true:已经运行
     */
    private boolean notifyCallbackOnlyIfRunning(@NotNull String id, @NotNull String url, @Nullable ProgressCallback callback) {
        ProgressInfo info = getRunningInfo(id);
        if (info != null) {//已经存在下载任务
            if (callback != null) {//回调不为空，添加回调
                callbacks.add(id, callback);
                callback.onProgressChanged(info);
            }
            return true;
        }
        return false;
    }

    private Call<ResponseBody> createCall(@NotNull String id, @NotNull String url, @Nullable ProgressCallback callback) {
        //开始请求前保存进度信息
        ProgressInfo progressInfo = new ProgressInfo(id, url);
        runningInfo.put(id, progressInfo);
        if (callback != null) {
            callbacks.add(id, callback);
            PrgCallbackGlue callbackGlue = new PrgCallbackGlue(progressInfo);
            return OkNet.createApiService(CommonService.class, true).download(url, callbackGlue);
        } else {
            return OkNet.createApiService(CommonService.class, true).download(url);
        }
    }

    /**
     * 移除任务
     *
     * @param id
     */
    private void removeRunningCall(@NotNull String id) {
        callbacks.remove(id);
        runningInfo.remove(id);
    }

    private File writeToFile(@NotNull ResponseBody responseBody, @NotNull File file) throws IOException {

        checkFile(file);
        try (BufferedSink target = Okio.buffer(Okio.sink(file))) {
            target.writeAll(responseBody.source());
        }
        return file;
    }

    private File writeToFile(@NotNull Response response, @NotNull File file) throws IOException {
        assert response.body() != null;
        checkFile(file);
        try (BufferedSink target = Okio.buffer(Okio.sink(file))) {
            target.writeAll(response.body().source());
        }
        return file;
    }

    private void checkFile(@NotNull File file) throws IOException {
        File parentFile = file.getParentFile();
        if (parentFile != null && !parentFile.exists()) {
            if (!parentFile.mkdirs()) {
                throw new FileSystemException("create file dir failed.");
            }
        }
        if (!file.exists()) {
            if (!file.createNewFile()) {
                throw new FileSystemException("create file dir failed.");
            }
        }
    }

}
