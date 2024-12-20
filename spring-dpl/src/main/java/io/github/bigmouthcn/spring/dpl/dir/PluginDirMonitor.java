package io.github.bigmouthcn.spring.dpl.dir;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 插件目录监听器
 *
 * @author allen
 * @date 2019/6/28
 * @since 1.0.0
 */
@Slf4j
public final class PluginDirMonitor implements InitializingBean, DisposableBean {

    private static final long DEFAULT_CHECK_TIMEOUT = 1000L;
    private final PluginChangedListener listener;
    private final String dir;
    private final long checkTimeout;

    private final ScheduledExecutorService scheduled = new ScheduledThreadPoolExecutor(1, new BasicThreadFactory.Builder().namingPattern("PluginDirMonitor-%d").build());
    private Snapshot lastSnapshot;

    public PluginDirMonitor(String dir) {
        this(new PluginChangedListener() {}, dir);
    }

    public PluginDirMonitor(PluginChangedListener listener, String dir) {
        this(listener, dir, DEFAULT_CHECK_TIMEOUT);
    }

    public PluginDirMonitor(PluginChangedListener listener, String dir, long checkTimeout) {
        this.listener = listener;
        this.dir = dir;
        this.checkTimeout = checkTimeout;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        scheduled.scheduleWithFixedDelay(this::doCheckDir, 0, checkTimeout, TimeUnit.MILLISECONDS);
    }

    @Override
    public void destroy() throws Exception {
        scheduled.shutdownNow();
    }

    private void doCheckDir() {
        try {
            Snapshot newSnapshot = new Snapshot(dir);
            if (isModified(newSnapshot)) {
                if (log.isDebugEnabled()) {
                    log.debug("checkDir: {} is changed.", dir);
                }

                Snapshot oldSnapshot = lastSnapshot;
                listener.onChanged(new PluginChangedEvent(dir, oldSnapshot, newSnapshot));
                lastSnapshot = newSnapshot;
            }
        } catch (Throwable e) {
            log.error("doCheckDir: ", e);
        }
    }

    private boolean isModified(Snapshot newSnapshot) {
        if (null == lastSnapshot) {
            return true;
        } else {
            if (lastSnapshot.hashCode() != newSnapshot.hashCode()) {
                return true;
            } else {
                if (!lastSnapshot.equals(newSnapshot)) {
                    return true;
                }
            }
        }
        return false;
    }

    public String getDir() {
        return dir;
    }

    public long getCheckTimeout() {
        return checkTimeout;
    }
}
