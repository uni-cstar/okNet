package unics.oknet.request;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.util.HashMap;
import java.util.Objects;

import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.Okio;
import retrofit2.Call;
import unics.oknet.OkNet;

/**
 * Create by luochao
 * on 2023/12/26
 */
public class OkUseCaseJava {

    SafeRefList callbacks = new SafeRefList();

    //请求中的任务
    HashMap<String, ProgressInfo> runningCalls = new HashMap<>();

    private OkUseCaseJava() {
    }

    private static class SingleTone {
        private static final OkUseCaseJava instance = new OkUseCaseJava();
    }

    public static OkUseCaseJava getInstance() {
        return SingleTone.instance;
    }

    /**
     * 是否正在下载
     */
    public boolean isDownloading(String id) {
        return runningCalls.containsKey(id);
    }

    /**
     * 获取下载信息
     */
    @Nullable
    public ProgressInfo getLoadingProgress(String id) {
        return runningCalls.get(id);
    }

    /**
     * 移除监听
     */
    public void removeProgressCallback(ProgressCallback callback) {
        callbacks.remove(callback);
    }

    /**
     * 添加监听
     */
    public void addProgressCallback(String id, ProgressCallback progressCallback) {
        callbacks.add(id, progressCallback);
    }

    /**
     * 移除监听
     */
    public void removeProgressCallback(String id) {
        callbacks.remove(id);
    }

    public File download(
            String url,
            File file
    ) throws IOException {
        return download(url, url, file);
    }

    public File download(
            String id,
            String url,
            File file
    ) throws IOException {
        try {
            //开始请求前保存进度信息
            runningCalls.put(id, null);

            Request request = new Request.Builder()
                    .url(url)
                    .build();
            Response response = OkNet.getOkHttpClient().newCall(request).execute();
            return writeToFile(response, file);
        } finally {
            removeTaskFlag(id);
        }
    }

    public File download(
            String url,
            File file,
            ProgressCallback onProgress
    ) throws IOException {
        return download(url, url, file, onProgress);
    }

    public File download(
            String id,
            String url,
            File file,
            ProgressCallback onProgress
    ) throws IOException {
        try {
            //开始请求前保存进度信息
            SmartProgressCallback callbackWrapper = new SmartProgressCallback(id);
            runningCalls.put(id, callbackWrapper.getProgressInfo());
            callbacks.add(id, onProgress);
            Call<ResponseBody> call = OkNet.createApiService(CommonService.class, true).download(url, callbackWrapper);
//            Request request = new Request.Builder()
//                    .url(url)
//                    .tag(DownloadPrgCallback.class, callbackWrapper)
//                    .build();
//            Response response = OkNet.getOkHttpClient().newCall(request).execute();
//            return handleDownloadRes(response, file);
            return writeToFile(Objects.requireNonNull(call.execute().body()), file);
        } finally {
            removeProgressCallback(id);
            removeTaskFlag(id);
        }
    }

    private boolean prepareDownload(String id, String url, @Nullable ProgressCallback callback) {
        if (isDownloading(id)) {
            //已经在下载，则只监听不再执行下载
            if (callback != null) {
                callbacks.add(id,callback);
                ProgressInfo info = runningCalls.get(id);
                if(info != null){
                    callback.onProgressChanged(info);
                }
            }
            return false;
        }
        return true;
    }

    private void removeTaskFlag(String id) {
        runningCalls.remove(id);
    }

    private File writeToFile(ResponseBody responseBody, File file) throws IOException {

        checkFile(file);
        try (BufferedSink target = Okio.buffer(Okio.sink(file))) {
            target.writeAll(responseBody.source());
        }
        return file;
    }

    private File writeToFile(Response response, File file) throws IOException {
        assert response.body() != null;
        checkFile(file);
        try (BufferedSink target = Okio.buffer(Okio.sink(file))) {
            target.writeAll(response.body().source());
        }
        return file;
    }

    private void checkFile(File file) throws IOException {
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
