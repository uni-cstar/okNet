package unics.oknet.request;

import android.os.SystemClock;

/**
 * Create by luochao
 * on 2023/12/26
 */
final class SmartProgressCallback implements PrgCallback, DownloadPrgCallback, UploadPrgCallback {

    //刷新间隔
    private final long notifyInterval;

    //待分发数据大小
    private long bytesPending;
    //上次回调时间
    private long lastDispatchRealtime;
    private ProgressInfo progressInfo;

    public SmartProgressCallback(String id) {
        this(id, 100);
    }

    /**
     * @param interval 最小刷新间隔
     */
    public SmartProgressCallback(String id, long interval) {
        if (interval <= 0)
            throw new IllegalArgumentException("minInterval must be greater than 0.");
        this.notifyInterval = interval;
        progressInfo = new ProgressInfo(id);
    }

    ProgressInfo getProgressInfo(){
        return progressInfo;
    }

    @Override
    public void onProgress(long byteCount, long bytesHandled, long bytesTotal) {
        if (bytesHandled == bytesTotal || SystemClock.elapsedRealtime() - lastDispatchRealtime >= notifyInterval) {
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
        SafeProgressCallback callback = OkUseCaseJava.getInstance().callbacks.get(progressInfo.id());
        if (callback != null) {
            callback.onProgressChanged(progressInfo);
        }
    }

}
