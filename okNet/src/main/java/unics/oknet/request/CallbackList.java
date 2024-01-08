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
class CallbackList {

    private final ConcurrentHashMap<String, CallbackHolder> callbacks = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<CallbackHolder> cachedCallbacks = new ConcurrentLinkedQueue<CallbackHolder>();

    @NotNull
    public CallbackHolder add(String key, ProgressCallback ref) {
        CallbackHolder callbackProxy;
        if (callbacks.containsKey(key)) {
            callbackProxy = callbacks.get(key);
            callbackProxy.add(ref);
        } else {
            callbackProxy = cachedCallbacks.poll();
            if (callbackProxy == null) {
                callbackProxy = new CallbackHolder(key, ref);
            } else {
                callbackProxy.reuse(key, ref);
            }
            callbacks.put(key, callbackProxy);
        }
        return callbackProxy;
    }

    @Nullable
    public CallbackHolder get(String key) {
        return callbacks.get(key);
    }

    public void remove(String key) {
        CallbackHolder value = callbacks.getOrDefault(key, null);
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
        Iterator<Map.Entry<String, CallbackHolder>> it = callbacks.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, CallbackHolder> next = it.next();
            CallbackHolder safeProgressCallback = next.getValue();
            if (safeProgressCallback == null || (safeProgressCallback.remove(callback) && safeProgressCallback.isEmpty())) {
                it.remove();
                if (safeProgressCallback != null) {
                    safeProgressCallback.reset();
                    cachedCallbacks.offer(safeProgressCallback);
                }
            }
        }
    }

    public boolean contains(ProgressCallback callback) {
        for (CallbackHolder holder : callbacks.values()) {
            if (holder.contains(callback))
                return true;
        }
        return false;
    }

}
