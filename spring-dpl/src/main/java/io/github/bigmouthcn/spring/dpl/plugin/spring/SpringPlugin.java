package io.github.bigmouthcn.spring.dpl.plugin.spring;

import io.github.bigmouthcn.spring.dpl.plugin.Plugin;
import io.github.bigmouthcn.spring.dpl.plugin.PluginConfig;
import org.springframework.context.support.AbstractApplicationContext;

/**
 * @author allen
 * @date 2019/6/27
 * @since 1.0.0
 */
public class SpringPlugin implements Plugin {

    private final PluginConfig config;
    private final AbstractApplicationContext applicationContext;

    public SpringPlugin(PluginConfig config, AbstractApplicationContext applicationContext) {
        this.config = config;
        this.applicationContext = applicationContext;
    }

    @Override
    public void destroy() {
        applicationContext.close();
    }

    @Override
    public PluginConfig getConfig() {
        return config;
    }

    public AbstractApplicationContext getApplicationContext() {
        return applicationContext;
    }

    @Override
    public <T> T getService(Class<T> type) {
        return applicationContext.getBean(type);
    }
}
