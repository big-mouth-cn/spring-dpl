package io.github.bigmouthcn.spring.dpl;

/**
 * @author allen
 * @date 2019/6/27
 * @since 1.0.0
 */
public class PluginRuntimeException extends RuntimeException {

    private static final long serialVersionUID = -4144464772072587497L;

    public PluginRuntimeException() {
    }

    public PluginRuntimeException(String message) {
        super(message);
    }

    public PluginRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public PluginRuntimeException(Throwable cause) {
        super(cause);
    }

    public PluginRuntimeException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
