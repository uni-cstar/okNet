package unics.oknet.request;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Create by luochao
 * on 2023/12/26
 * 上传/下载 进度拦截器
 */
public class ProgressInterceptor implements Interceptor {

    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        Request request = chain.request();
        UploadPrgCallback reqPrgCallback = request.tag(UploadPrgCallback.class);
        if (reqPrgCallback != null && request.body() != null) {
            request = request.newBuilder()
                    .method(request.method(), new RequestProgressBody(request.body(), reqPrgCallback))
                    .build();
        }
        Response originResp = chain.proceed(request);

        //处理下载进度监听
        DownloadPrgCallback resPrgCallback = request.tag(DownloadPrgCallback.class);
        if (resPrgCallback != null && originResp.body() != null) {
            return originResp.newBuilder()
                    .body(new ResponseProgressBody(originResp.body(), resPrgCallback))
                    .build();

        }
        return originResp;
    }

}
