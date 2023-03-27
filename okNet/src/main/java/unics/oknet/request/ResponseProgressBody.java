package unics.oknet.request;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.ForwardingSource;
import okio.Okio;
import okio.Source;

/**
 * Create by luochao
 * on 2023/12/26
 */
class ResponseProgressBody extends ResponseBody {

    private final ResponseBody responseBody;
    private final DownloadPrgCallback callback;
    private BufferedSource bufferedSource;

    ResponseProgressBody(@NotNull ResponseBody source, @NotNull DownloadPrgCallback callback) {
        this.responseBody = source;
        this.callback = callback;
    }

    @Override
    public MediaType contentType() {
        return responseBody.contentType();
    }

    @Override
    public long contentLength() {
        return responseBody.contentLength();
    }

    @NotNull
    @Override
    public BufferedSource source() {
        if (bufferedSource == null) {
            bufferedSource = Okio.buffer(progressSource(responseBody.source()));
        }
        return bufferedSource;
    }

    private Source progressSource(Source source) {

        return new ForwardingSource(source) {

            long bytesRead = 0L;
            long contentLength = 0L;

            @Override
            public long read(Buffer sink, long byteCount) throws IOException {
                long readCount = super.read(sink, byteCount);
                if (contentLength == 0) {
                    contentLength = contentLength();
                }
                bytesRead += byteCount;
                callback.onProgress(byteCount, bytesRead, contentLength);
                return readCount;
            }
        };
    }
}
