package unics.oknet.request;

import android.os.SystemClock;
import android.text.format.DateUtils;

import org.jetbrains.annotations.NotNull;

/**
 * Create by luochao
 * on 2023/12/26
 */
public class ProgressInfo {

    /**
     * 最大等待时间
     */
    public static final long MAX_WAITING_TIME = 31536000000L;

    //唯一键
    private final String id;

    //请求的初始url
    private final String url;

    //本次变化大小、已完成大小、总大小
    private long bytesInterval, bytesWritten, bytesTotal;

    //开始时间
    private final long startRealtime = SystemClock.elapsedRealtime();

    //上一次更新进度的时间
    private long lastUpdateRealtime = startRealtime;

    //距离上一次更新的间隔时间
    private long intervalUpdateRealtime = 0;

    ProgressInfo(@NotNull String id, @NotNull String url) {
        this.id = id;
        this.url = url;
    }

    void update(long byteCount, long bytesWritten, long bytesTotal) {
        long realtime = SystemClock.elapsedRealtime();
        this.intervalUpdateRealtime = realtime - lastUpdateRealtime;
        this.lastUpdateRealtime = realtime;
        this.bytesTotal = bytesTotal;
        this.bytesWritten = bytesWritten;
        this.bytesInterval = byteCount;
    }

    public String id() {
        return this.id;
    }

    public String url(){
        return this.url;
    }

    /**
     * 当前完成进度
     *
     * @return 0-100
     */
    public int progress() {
        if (bytesTotal == bytesWritten)
            return 100;
        else if (bytesWritten <= 0 || bytesTotal == 0) {
            return 0;
        } else if (bytesWritten >= bytesTotal) {
            return 100;
        } else {
            return (int) ((bytesWritten * 1.0 / bytesTotal) * 100);
        }
    }

    /**
     * 总大小
     *
     * @return bytes
     */
    public long totalSize() {
        return bytesTotal;
    }

    /**
     * 已完成大小
     *
     * @return bytes
     */
    public long currentSize() {
        return bytesWritten;
    }

    /**
     * 剩余大小
     *
     * @return bytes
     */
    public long remainSize() {
        return Math.max(bytesTotal - bytesWritten, 0);
    }

    /**
     * 当前下载速度
     *
     * @return byte/seconds
     */
    public long speed() {
        if (intervalUpdateRealtime <= 0)
            return 0;
        //*1000是将毫秒转换成秒
        return (long) (bytesInterval * 1000.0 / intervalUpdateRealtime);
    }

    /**
     * 平均速度
     *
     * @return byte/seconds
     */
    public long avgSpeed() {
        long time = lastUpdateRealtime - startRealtime;
        if (time <= 0)
            return 0;
        //*1000是将毫秒转换成秒
        return (long) (bytesWritten * 1000.0 / time);
    }

    /**
     * 已用时间
     *
     * @return 单位：毫秒
     */
    public long usedTimeMillis() {
        return SystemClock.elapsedRealtime() - startRealtime;
    }

    /**
     * 剩余时间
     *
     * @return 单位：毫秒
     */
    public long remainTimeMillis() {
        long speed = speed();
        if (speed <= 0) {
            return MAX_WAITING_TIME;
        }
        //*1000 是因为速度是byte/秒
        return ((bytesTotal - bytesWritten) / speed) * 1000;
    }

    /**
     * 剩余时间
     *
     * @return 单位：秒
     */
    public long remainTimeSeconds() {
        return remainTimeMillis() / 1000;
    }

    /**
     * 已用时间
     *
     * @return 格式化后的字符串
     * @see DateUtils#formatElapsedTime(long)
     */
    public String usedTimeMillisFormatted() {
        return DateUtils.formatElapsedTime(remainTimeSeconds());
    }
//
//    public String totalSizeFormatted() {
//        return Formatter.formatFileSize(OkNet.getApp(), totalSize());
//    }

}
