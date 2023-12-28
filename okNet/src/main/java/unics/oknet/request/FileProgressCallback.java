package unics.oknet.request;

import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * Create by luochao
 * on 2023/12/26
 */
public interface FileProgressCallback extends ProgressCallback {

    void onComplete(@NotNull File file);

}
