package io.github.bigmouthcn.spring.dpl.plugin;

/**
 * 插件加载器
 * @author allen
 * @date 2019/6/27
 * @since 1.0.0
 */
public interface PluginLoader {

    /**
     * 根据插件配置加载插件
     * @param config 插件配置
     * @return 加载完成的插件
     */
    Plugin load(PluginConfig config);
}
