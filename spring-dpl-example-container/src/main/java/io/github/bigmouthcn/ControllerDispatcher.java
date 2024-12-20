package io.github.bigmouthcn;

import io.github.bigmouthcn.facade.MethodService;
import io.github.bigmouthcn.spring.dpl.PluginBus;
import io.github.bigmouthcn.spring.dpl.plugin.Plugin;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Allen Hu
 * @date 2024/12/20
 */
@RestController
public class ControllerDispatcher {

    private final PluginBus pluginBus;

    public ControllerDispatcher(PluginBus pluginBus) {
        this.pluginBus = pluginBus;
    }

    @GetMapping("/get")
    public ResponseEntity<Object> get(String pluginKey) {
        Plugin plugin = pluginBus.lookup(pluginKey);
        if (plugin == null) {
            return ResponseEntity.badRequest().body("plugin not found");
        }
        MethodService methodService = plugin.getService(MethodService.class);
        return ResponseEntity.ok(methodService.getMethodHandle());
    }
}
