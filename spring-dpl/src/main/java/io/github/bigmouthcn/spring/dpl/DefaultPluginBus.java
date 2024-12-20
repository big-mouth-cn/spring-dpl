package io.github.bigmouthcn.spring.dpl;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import io.github.bigmouthcn.spring.dpl.plugin.Plugin;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Iterator;
import java.util.concurrent.ConcurrentMap;

/**
 * @author allen
 * @date 2019/6/28
 * @since 1.0.0
 */
@Slf4j
public class DefaultPluginBus implements PluginBus {

    private final ConcurrentMap<String, Plugin> pluginHolder = Maps.newConcurrentMap();

    @Override
    public void register(Plugin plugin) {
        Preconditions.checkNotNull(plugin);
        String key = plugin.getConfig().getKey();
        Plugin old = pluginHolder.putIfAbsent(key, plugin);
        if (null != old) {
            throw new RuntimeException("Plugin[" + key + "] has existed, ignore.");
        } else {
            log.info("Plugin[{}] has registered successful.", key);
        }
    }

    @Override
    public void unregister(Plugin plugin) {
        Preconditions.checkNotNull(plugin);
        String key = plugin.getConfig().getKey();
        Plugin removed = pluginHolder.remove(key);
        if (null != removed) {
            log.info("Plugin[{}] has unregistered successful.", key);
        } else {
            log.debug("No plugin for {}", key);
        }
    }

    @Override
    public Iterator<Plugin> getAllPlugins() {
        return pluginHolder.values().iterator();
    }

    @Override
    public Plugin lookup(String key) {
        Preconditions.checkArgument(StringUtils.isNotBlank(key));
        Plugin plugin = pluginHolder.get(key);
        if (null == plugin) {
            log.debug("Can not found plugin for {}", key);
        }
        return plugin;
    }
}
