package unics.oknet.request;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Create by luochao
 * on 2023/12/26
 */
class SafeProgressCallback implements ProgressCallback {

    private String id;

    private CopyOnWriteArrayList<ProgressCallback> refs = new CopyOnWriteArrayList<>();

    public SafeProgressCallback(String id) {
        this.id = id;
    }

    public SafeProgressCallback(String id, ProgressCallback ref) {
        this.id = id;
        refs.add(ref);
    }

    public void add(ProgressCallback real) {
        this.refs.add(real);
    }

    public boolean remove(ProgressCallback callback) {
        return this.refs.remove(callback);
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

    @Override
    public void onProgressChanged(@NotNull ProgressInfo progress) {
        for (ProgressCallback callback : refs) {
            callback.onProgressChanged(progress);
        }
    }

}
