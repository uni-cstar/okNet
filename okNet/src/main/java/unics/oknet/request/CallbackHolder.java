package unics.oknet.request;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Create by luochao
 * on 2023/12/26
 * 回调持有者，里面保存了客户回调
 */
class CallbackHolder implements ProgressCallback, FileDownloadCallback {

    private String id;

    private final CopyOnWriteArrayList<ProgressCallback> refs = new CopyOnWriteArrayList<>();

    public CallbackHolder(String id) {
        this.id = id;
    }

    public CallbackHolder(String id, ProgressCallback ref) {
        this.id = id;
        refs.add(ref);
    }

    @NotNull
    public List<ProgressCallback> getCallbacks() {
        return refs;
    }

    public void add(ProgressCallback real) {
        if (this.refs.contains(real))
            return;
        this.refs.add(real);
    }

    public boolean remove(ProgressCallback callback) {
        return this.refs.remove(callback);
    }

    public boolean contains(ProgressCallback callback) {
        return this.refs.contains(callback);
    }

    public int size() {
        return this.refs.size();
    }

    public boolean isEmpty() {
        return this.refs.isEmpty();
    }

    public void reset() {
        this.refs.clear();
        this.id = null;
    }

    /**
     * 调用reuse之前请自行调用{@link #reset()}
     * @param id
     * @param callback
     */
    void reuse(String id, ProgressCallback callback) {
        this.id = id;
        this.refs.add(callback);
    }

    @Override
    public void onProgressChanged(@NotNull ProgressInfo progress) {
        for (ProgressCallback callback : refs) {
            callback.onProgressChanged(progress);
        }
    }

    @Override
    public void onStart(@NotNull String url) {
        for (ProgressCallback callback : refs) {
            if (callback instanceof FileDownloadCallback) {
                ((FileDownloadCallback) callback).onStart(url);
            }
        }
    }

    @Override
    public void onComplete(@NotNull String url, @NotNull File file) {
        for (ProgressCallback callback : refs) {
            if (callback instanceof FileDownloadCallback) {
                ((FileDownloadCallback) callback).onComplete(url, file);
            }
        }
    }

    @Override
    public void onError(@NotNull String url, @NotNull Throwable e) {
        for (ProgressCallback callback : refs) {
            if (callback instanceof FileDownloadCallback) {
                ((FileDownloadCallback) callback).onError(url, e);
            }
        }
    }
}
