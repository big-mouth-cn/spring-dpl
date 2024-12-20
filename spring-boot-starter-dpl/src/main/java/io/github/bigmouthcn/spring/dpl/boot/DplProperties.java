package io.github.bigmouthcn.spring.dpl.boot;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author allen
 * @date 2019/6/28
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "spring.dpl")
public class DplProperties {

    private String pluginDir = "../plugins/";
    private String basePackageToScanOfPlugin = "io.github.bigmouthcn.plugin.*";

    public String getPluginDir() {
        return pluginDir;
    }

    public void setPluginDir(String pluginDir) {
        this.pluginDir = pluginDir;
    }

    public String getBasePackageToScanOfPlugin() {
        return basePackageToScanOfPlugin;
    }

    public void setBasePackageToScanOfPlugin(String basePackageToScanOfPlugin) {
        this.basePackageToScanOfPlugin = basePackageToScanOfPlugin;
    }
}
