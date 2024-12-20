package io.github.bigmouthcn.spring.dpl.plugin.spring;

import com.google.common.collect.Lists;
import io.github.bigmouthcn.spring.dpl.PluginRuntimeException;
import io.github.bigmouthcn.spring.dpl.hotswap.ClassFilter;
import io.github.bigmouthcn.spring.dpl.hotswap.PluginClassLoader;
import io.github.bigmouthcn.spring.dpl.hotswap.ResourceFileter;
import io.github.bigmouthcn.spring.dpl.plugin.Plugin;
import io.github.bigmouthcn.spring.dpl.plugin.PluginConfig;
import io.github.bigmouthcn.spring.dpl.plugin.PluginConfigAware;
import io.github.bigmouthcn.spring.dpl.plugin.PluginLoader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author allen
 * @date 2019/6/27
 * @since 1.0.0
 */
@Slf4j
public class SpringPluginLoader implements PluginLoader {

    private static final List<Class<? extends Annotation>> DEFAULT_LOADING_ANNOTATION = Lists.newArrayList(
            org.springframework.stereotype.Component.class,
            org.springframework.stereotype.Repository.class,
            org.springframework.stereotype.Service.class,
            org.springframework.stereotype.Controller.class
    );

    private final String basePackage;
    private final List<Class<? extends Annotation>> loadAnnotations;
    private final ApplicationContext parent;

    public SpringPluginLoader(String basePackage, ApplicationContext parent) {
        this(basePackage, DEFAULT_LOADING_ANNOTATION, parent);
    }

    public SpringPluginLoader(String basePackage, List<Class<? extends Annotation>> loadAnnotations, ApplicationContext parent) {
        this.basePackage = basePackage;
        this.loadAnnotations = loadAnnotations;
        this.parent = parent;
    }

    @Override
    public Plugin load(PluginConfig config) {
        AbstractApplicationContext applicationContext = register(config);
        return new SpringPlugin(config, applicationContext);
    }

    private AbstractApplicationContext register(PluginConfig config) {
        ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            PluginClassLoader pluginClassLoader = new PluginClassLoader(config.getJarPath());
            Thread.currentThread().setContextClassLoader(pluginClassLoader);

            AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext();

            List<Class<?>> classesNeeded = pluginClassLoader.searchClasses(basePackage, new ClassFilter() {
                @Override
                public boolean accept(Class<?> clazz) {
                    if (null != loadAnnotations && !loadAnnotations.isEmpty()) {
                        for (Class<? extends Annotation> loadClass : loadAnnotations) {
                            if (clazz.isAnnotationPresent(loadClass)) {
                                return true;
                            }
                        }
                    }
                    return false;
                }
            });
            if (null == classesNeeded || classesNeeded.isEmpty()) {
                log.warn("Plugin[{}] No annotations to load. {}", config.getJarPath(), loadAnnotations);
                return null;
            }
            this.fillConfig(config, pluginClassLoader);

            applicationContext.setParent(parent);
            applicationContext.setClassLoader(pluginClassLoader);
            applicationContext.register(classesNeeded.toArray(new Class<?>[0]));
            Map<String, BeforeRefreshContextHandler> beansOfType = parent.getBeansOfType(BeforeRefreshContextHandler.class);
            for (BeforeRefreshContextHandler handler : beansOfType.values()) {
                handler.accept(applicationContext);
            }
            applicationContext.refresh();

            Map<String, PluginConfigAware> beans = applicationContext.getBeansOfType(PluginConfigAware.class);
            for (PluginConfigAware aware : beans.values()) {
                aware.setPluginConfig(config);
            }

            return applicationContext;
        } finally {
            Thread.currentThread().setContextClassLoader(currentClassLoader);
        }
    }

    private void fillConfig(PluginConfig config, PluginClassLoader pluginClassLoader) {
        List<String> resources = pluginClassLoader.searchResources("META-INF/*", new ResourceFileter() {
            @Override
            public boolean accept(String name) {
                return StringUtils.endsWith(name, "plugin.properties");
            }
        }, false);
        if (null == resources || resources.isEmpty()) {
            throw new PluginRuntimeException("Plugin[" + config.getJarPath() + "] Can not found file classpath:/META-INF/plugin.properties");
        }
        for (String resource : resources) {
            try {
                Properties properties = new Properties();
                properties.load(new ClassPathResource(resource).getInputStream());
                config.setProperties(properties);
            } catch (IOException e) {
                log.error("Can not load[{}] resource for class path.", resource);
            }
        }
    }
}
