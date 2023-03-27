package unics.oknet.request;

import org.jetbrains.annotations.NotNull;

/**
 * Create by luochao
 * on 2023/12/26
 */
public interface ProgressCallback {

    void onProgressChanged(@NotNull ProgressInfo progress);

}
