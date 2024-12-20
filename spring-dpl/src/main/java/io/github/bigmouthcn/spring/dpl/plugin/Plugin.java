package io.github.bigmouthcn.spring.dpl.plugin;

import io.github.bigmouthcn.spring.dpl.plugin.spring.SpringPlugin;

/**
 * 插件
 *
 * @author allen
 * @date 2019/6/27
 * @since 1.0.0
 */
public interface Plugin {

    /**
     * 销毁这个插件
     */
    void destroy();

    /**
     * 获取这个插件的配置信息
     * @return 配置信息
     */
    PluginConfig getConfig();

    /**
     * 根据插件具体的实现获取这个插件中的某个服务。<br>
     * 比如，接口的实现是{@link SpringPlugin}，那么内部将是从{@code ApplicationContext}中获取注入的{@code Bean}实现。
     *
     * @param type 服务类
     * @param <T> 泛型
     * @return 服务对象
     * @see SpringPlugin
     */
    <T> T getService(Class<T> type);
}
