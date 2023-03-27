package unics.oknet.request;

/**
 * Create by luochao
 * on 2023/12/26
 * 内部的进度回调，必须与外部切开，否则可能导致类型判定糅合
 */
interface PrgCallback {

    /**
     * @param byteCount    本次大小
     * @param bytesHandled 已处理大小
     * @param bytesTotal   总数据大小
     */
    void onProgress(long byteCount, long bytesHandled, long bytesTotal);

}

