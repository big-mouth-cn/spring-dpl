package io.github.bigmouthcn.plugin;

import io.github.bigmouthcn.facade.MethodService;
import org.springframework.stereotype.Service;

/**
 * @author Allen Hu
 * @date 2024/12/20
 */
@Service
public class DemoMethodService implements MethodService {

    @Override
    public Object getMethodHandle() {
        return "Hello, I'm DemoMethodService, get method handle.";
    }

    @Override
    public Object postMethodHandle() {
        return "Hello, I'm DemoMethodService, post method handle.";
    }
}
