package unics.oknet.request;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Create by luochao
 * on 2023/12/26
 */
class SafeRefList {

    private final ConcurrentHashMap<String, SafeProgressCallback> callbacks = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<SafeProgressCallback> cachedCallbacks = new ConcurrentLinkedQueue<SafeProgressCallback>();

    @NotNull
    public SafeProgressCallback add(String key, ProgressCallback ref) {
        SafeProgressCallback callbackProxy;
        if (callbacks.containsKey(key)) {
            callbackProxy = callbacks.get(key);
            callbackProxy.add(ref);
        } else {
            callbackProxy = cachedCallbacks.poll();
            if (callbackProxy == null) {
                callbackProxy = new SafeProgressCallback(key, ref);
                callbacks.put(key, callbackProxy);
            } else {
                callbackProxy.add(ref);
            }
        }
        return callbackProxy;
    }

    @Nullable
    public SafeProgressCallback get(String key) {
        return callbacks.get(key);
    }

    public void remove(String key) {
        SafeProgressCallback value = callbacks.getOrDefault(key, null);
        if (value != null) {
            value.reset();
            try {
                callbacks.remove(key, value);
            } catch (Throwable ignored) {
            }
            cachedCallbacks.offer(value);
        }
    }

    public void remove(ProgressCallback callback) {
        Iterator<Map.Entry<String, SafeProgressCallback>> it = callbacks.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, SafeProgressCallback> next = it.next();
            SafeProgressCallback safeProgressCallback = next.getValue();
            if (safeProgressCallback == null || (safeProgressCallback.remove(callback) && safeProgressCallback.isEmpty())) {
                it.remove();
                if (safeProgressCallback != null) {
                    safeProgressCallback.reset();
                    cachedCallbacks.offer(safeProgressCallback);
                }
            }
        }
    }

}
