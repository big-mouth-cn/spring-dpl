package io.github.bigmouthcn.spring.dpl.dir;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EventListener;

/**
 * 插件变更事件监听器
 *
 * @author allen
 * @date 2019/6/28
 * @since 1.0.0
 */
public interface PluginChangedListener extends EventListener {

    Logger LOGGER = LoggerFactory.getLogger(PluginChangedListener.class);

    default void onChanged(PluginChangedEvent event) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("dir has changed, detail info: {}", event);
        }
    }
}
