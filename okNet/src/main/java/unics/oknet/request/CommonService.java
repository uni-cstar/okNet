package unics.oknet.request;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Streaming;
import retrofit2.http.Tag;
import retrofit2.http.Url;

/**
 * Create by luochao
 * on 2023/12/26
 */
interface CommonService {

    @Streaming
    @GET
    Call<ResponseBody> download(@Url String url);

    @Streaming
    @GET
    Call<ResponseBody> download(@Url String url, @Tag DownloadPrgCallback callback);

}
