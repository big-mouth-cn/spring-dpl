package io.github.bigmouthcn.spring.dpl.plugin;

import lombok.Getter;

import java.util.Properties;

/**
 * @author allen
 * @date 2019/6/27
 * @since 1.0.0
 */
@Getter
public class PluginConfig {

    private final String jarPath;

    private Properties properties;

    public PluginConfig(String jarPath) {
        this.jarPath = jarPath;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public String getJarPath() {
        return jarPath;
    }

    public String getKey() {
        return properties.getProperty("name");
    }

    public String getVersion() {
        return properties.getProperty("version");
    }

    public String getAuthor() {
        return properties.getProperty("author");
    }

    public String getDescription() {
        return properties.getProperty("description");
    }

    public Properties getProperties() {
        return properties;
    }
}
