package unics.oknet.request;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;
import okio.Sink;

/**
 * Create by luochao
 * on 2023/12/26
 */
class RequestProgressBody extends RequestBody {

    private final RequestBody requestBody;
    private final UploadPrgCallback callback;

    RequestProgressBody(@NotNull RequestBody requestBody, @Nullable UploadPrgCallback callback) {
        this.requestBody = requestBody;
        this.callback = callback;
    }

    RequestBody getRequestBody() {
        return requestBody;
    }

    @Override
    public MediaType contentType() {
        return requestBody.contentType();
    }

    @Override
    public long contentLength() throws IOException {
        return requestBody.contentLength();
    }

    @Override
    public void writeTo(@NotNull BufferedSink sink) throws IOException {
        //如果没有回调、或者是AS拦截器监听，则直接写入
        if (callback == null || sink instanceof Buffer
                || sink.toString().contains(
                "com.android.tools.profiler.support.network.HttpTracker$OutputStreamTracker")) {
            requestBody.writeTo(sink);
        } else {
            BufferedSink bufferedSink = Okio.buffer(progressSink(sink));
            requestBody.writeTo(bufferedSink);
            bufferedSink.close();
        }
    }

    private Sink progressSink(Sink sink) {

        return new ForwardingSink(sink) {
            long bytesWritten = 0L;
            long contentLength = 0L;

            @Override
            public void write(@NotNull Buffer source, long byteCount) throws IOException {
                super.write(source, byteCount);
                if (contentLength == 0) {
                    contentLength = contentLength();
                }
                bytesWritten += byteCount;
                callback.onProgress(byteCount, bytesWritten, contentLength);
            }
        };
    }
}
