package io.github.bigmouthcn.spring.dpl.dir;


import com.google.common.collect.Maps;
import io.github.bigmouthcn.spring.dpl.PluginBus;
import io.github.bigmouthcn.spring.dpl.PluginRuntimeException;
import io.github.bigmouthcn.spring.dpl.plugin.Plugin;
import io.github.bigmouthcn.spring.dpl.plugin.PluginConfig;
import io.github.bigmouthcn.spring.dpl.plugin.PluginLoader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author allen
 * @date 2019/6/28
 * @since 1.0.0
 */
@Slf4j
public class DefaultPluginChangedListener implements PluginChangedListener {

    private final PluginBus pluginBus;
    private final PluginLoader pluginLoader;

    private final Map<String, String> filePathAndKeyMapping = Maps.newHashMap();

    private final ExecutorService controlExecutor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(),
            new BasicThreadFactory.Builder().namingPattern("DPL-control-%d").build());

    public DefaultPluginChangedListener(PluginBus pluginBus, PluginLoader pluginLoader) {
        this.pluginBus = pluginBus;
        this.pluginLoader = pluginLoader;
    }

    @Override
    public void onChanged(PluginChangedEvent event) {
        List<String> added = event.getAdded();
        List<String> updated = event.getUpdated();
        List<String> removed = event.getRemoved();

        if (null != added && !added.isEmpty()) {
            for (String f : added) {
                addPlugin(f);
            }
        }
        if (null != updated && !updated.isEmpty()) {
            for (String f : updated) {
                String pluginKey = filePathAndKeyMapping.get(f);
                if (StringUtils.isBlank(pluginKey)) {
                    log.debug("Plugin file[{}] load has failed previously, add plugin directly.", f);
                    addPlugin(f);
                    continue;
                }
                updatePlugin(f, pluginKey);
            }
        }
        if (null != removed && !removed.isEmpty()) {
            for (String f : removed) {
                String pluginKey = filePathAndKeyMapping.get(f);
                if (StringUtils.isBlank(pluginKey)) {
                    continue;
                }
                removePlugin(f, pluginKey);
            }
        }
    }

    private void addPlugin(String filePath) {
        String pluginKey = installPlugin(filePath);
        if (StringUtils.isBlank(pluginKey)) {
            return;
        }
        filePathAndKeyMapping.put(filePath, pluginKey);
    }

    private String installPlugin(String filePath) {
        Future<String> future = controlExecutor.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                try {
                    Plugin plugin = createPlugin(filePath);
                    if (null == plugin) {
                        return null;
                    }
                    pluginBus.register(plugin);
                    return plugin.getConfig().getKey();
                } catch (PluginRuntimeException e) {
                    log.error("installPlugin: " + filePath, e);
                    return null;
                }
            }
        });
        try {
            return future.get();
        } catch (InterruptedException e) {
            future.cancel(true);
            throw new RuntimeException("installPlugin: ", e);
        } catch (ExecutionException e) {
            future.cancel(true);
            Throwable cause = e.getCause();
            throw launderThrowable("installPlugIn[" + filePath + "]:", cause);
        }
    }

    private Plugin createPlugin(String filePath) {
        return pluginLoader.load(new PluginConfig(filePath));
    }

    private void updatePlugin(String filePath, String pluginKey) {
        uninstallPlugin(pluginKey);
        installPlugin(filePath);
    }

    private void removePlugin(String filePath, String pluginKey) {
        uninstallPlugin(pluginKey);
        filePathAndKeyMapping.remove(filePath);
    }

    private void uninstallPlugin(String pluginKey) {
        controlExecutor.execute(() -> {
            Plugin plugin = pluginBus.lookup(pluginKey);
            if (null == plugin) {
                log.error("Key[{}] is not mapping any plugin.", pluginKey);
                return;
            }

            pluginBus.unregister(plugin);
            plugin.destroy();
        });
    }

    private RuntimeException launderThrowable(String message, Throwable t) {
        if (t instanceof RuntimeException) {
            // return (RuntimeException) t;
            return new RuntimeException(message, t);
        } else if (t instanceof Error) {
            throw (Error) t;
        } else {
            throw new RuntimeException(message, t);
        }
    }
}
