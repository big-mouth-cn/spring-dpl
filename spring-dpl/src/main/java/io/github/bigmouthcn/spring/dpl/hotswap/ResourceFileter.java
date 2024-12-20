package io.github.bigmouthcn.spring.dpl.hotswap;

/**
 * @author allen
 * @date 2019/6/27
 * @since 1.0.0
 */
public interface ResourceFileter {

    default boolean accept(String name) {
        return true;
    }
}
