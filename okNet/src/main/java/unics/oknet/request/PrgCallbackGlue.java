package unics.oknet.request;

import android.os.SystemClock;

import org.jetbrains.annotations.NotNull;

/**
 * Create by luochao
 * on 2023/12/26
 * 回调纽带，将传输的进度回传到客户端（如果能找到回调），并提供限流支持{@link #dispatchInterval}
 */
final class PrgCallbackGlue implements PrgCallback, DownloadPrgCallback, UploadPrgCallback {

    //分发间隔
    private final long dispatchInterval;

    //待分发数据大小
    private long bytesPending;
    //上次分发时间
    private long lastDispatchRealtime;
    //当前进度信息
    private final ProgressInfo progressInfo;

    public PrgCallbackGlue(@NotNull ProgressInfo progressInfo) {
        this(progressInfo, 100);
    }

    /**
     * @param interval 最小刷新间隔
     */
    public PrgCallbackGlue(@NotNull ProgressInfo progressInfo, long interval) {
        if (interval <= 0)
            throw new IllegalArgumentException("minInterval must be greater than 0.");
        this.dispatchInterval = interval;
        this.progressInfo = progressInfo;
    }

    ProgressInfo getProgressInfo() {
        return progressInfo;
    }

    @Override
    public void onProgress(long byteCount, long bytesHandled, long bytesTotal) {
        if (bytesHandled == bytesTotal || SystemClock.elapsedRealtime() - lastDispatchRealtime >= dispatchInterval) {
            //进行分发
            long pendingByteCount = bytesPending + byteCount;
            bytesPending = 0;
            lastDispatchRealtime = SystemClock.elapsedRealtime();
            dispatchCallback(pendingByteCount, bytesHandled, bytesTotal);
        } else {
            //等待分发
            bytesPending += byteCount;
        }
    }

    private void dispatchCallback(long byteCount, long bytesWritten, long bytesTotal) {
        progressInfo.update(byteCount, bytesWritten, bytesTotal);
        CallbackHolder callback = OkNetUseCase.getInstance().callbacks.get(progressInfo.id());
        if (callback != null) {
            callback.onProgressChanged(progressInfo);
        }
    }

}
