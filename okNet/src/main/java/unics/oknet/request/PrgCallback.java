package unics.oknet.request;

/**
 * Create by luochao
 * on 2023/12/26
 */
interface PrgCallback {

    /**
     * @param byteCount    本次大小
     * @param bytesHandled 已处理大小
     * @param bytesTotal   总数据大小
     */
    void onProgress(long byteCount, long bytesHandled, long bytesTotal);

}

