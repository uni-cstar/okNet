package unics.oknet.request;

import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Create by luochao
 * on 2023/12/26
 */
public interface FileDownloadCallback extends ProgressCallback {

    void onStart(@NotNull String url);

    void onComplete(@NotNull String url, @NotNull File file);

    void onError(@NotNull String url, @NotNull Throwable e);

}
