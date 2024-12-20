package io.github.bigmouthcn.spring.dpl.plugin.spring;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.function.Consumer;

/**
 * 在插件的Spring上下文刷新前的处理器，需要把接口的实现注入到顶级Spring ApplicationContext中。
 *
 * {@link #accept(Object)} 传入的上下文是当前插件构建的上下文。
 *
 * @author allen
 * @date 2021-07-08
 * @since 1.0
 */
public interface BeforeRefreshContextHandler extends Consumer<AnnotationConfigApplicationContext> {
}
