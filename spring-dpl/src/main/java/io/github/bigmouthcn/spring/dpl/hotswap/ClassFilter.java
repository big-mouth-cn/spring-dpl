package io.github.bigmouthcn.spring.dpl.hotswap;

/**
 * @author allen
 * @date 2019/6/27
 * @since 1.0.0
 */
public interface ClassFilter {

    default boolean accept(Class<?> clazz) {
        return true;
    }
}
