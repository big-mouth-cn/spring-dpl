package io.github.bigmouthcn.spring.dpl;

import io.github.bigmouthcn.spring.dpl.plugin.Plugin;
import io.github.bigmouthcn.spring.dpl.plugin.PluginConfig;

import java.util.Iterator;

/**
 * 插件总线，可以注册、注销和获取所有插件
 *
 * @author allen
 * @date 2019/6/28
 * @since 1.0.0
 */
public interface PluginBus {

    /**
     * 注册插件
     * @param plugin 插件
     */
    void register(Plugin plugin);

    /**
     * 注销插件
     * @param plugin 插件
     */
    void unregister(Plugin plugin);

    /**
     * 获取所有注册的插件
     * @return 插件迭代器
     */
    Iterator<Plugin> getAllPlugins();

    /**
     * 查找已加载的插件
     * @param key {@link PluginConfig#getKey()}
     * @return 插件
     */
    Plugin lookup(String key);
}
