package io.github.bigmouthcn.spring.dpl.boot;

import io.github.bigmouthcn.spring.dpl.DefaultPluginBus;
import io.github.bigmouthcn.spring.dpl.PluginBus;
import io.github.bigmouthcn.spring.dpl.dir.DefaultPluginChangedListener;
import io.github.bigmouthcn.spring.dpl.dir.PluginChangedListener;
import io.github.bigmouthcn.spring.dpl.dir.PluginDirMonitor;
import io.github.bigmouthcn.spring.dpl.plugin.PluginLoader;
import io.github.bigmouthcn.spring.dpl.plugin.spring.SpringPluginLoader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

/**
 * @author allen
 * @date 2019/6/28
 * @since 1.0.0
 */
@EnableConfigurationProperties(DplProperties.class)
public class DplAutoConfiguration {

    private final DplProperties properties;

    public DplAutoConfiguration(DplProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean(PluginLoader.class)
    public PluginLoader pluginLoader(ApplicationContext applicationContext) {
        return new SpringPluginLoader(properties.getBasePackageToScanOfPlugin(), applicationContext);
    }

    @Bean
    @ConditionalOnMissingBean(PluginBus.class)
    public PluginBus pluginBus() {
        return new DefaultPluginBus();
    }

    @Bean
    @ConditionalOnMissingBean(PluginChangedListener.class)
    public PluginChangedListener pluginChangedListener(PluginBus pluginBus, PluginLoader pluginLoader) {
        return new DefaultPluginChangedListener(pluginBus, pluginLoader);
    }

    @Bean
    public PluginDirMonitor pluginDirMonitor(PluginChangedListener listener) {
        return new PluginDirMonitor(listener, properties.getPluginDir());
    }
}
