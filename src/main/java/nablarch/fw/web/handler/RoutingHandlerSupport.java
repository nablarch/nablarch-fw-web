package nablarch.fw.web.handler;

import nablarch.fw.handler.DispatchHandler;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpRequestHandler;
import nablarch.fw.web.HttpResponse;

/**
 * ルーティングハンドラをサポートする抽象クラス。
 *
 * @author Hisaaki Shioiri
 */
public abstract class RoutingHandlerSupport
        extends DispatchHandler<HttpRequest, HttpResponse, RoutingHandlerSupport>
        implements HttpRequestHandler {

    /** {@link nablarch.fw.MethodBinder}のファクトリ。 */
    protected MethodBinderFactory methodBinderFactory;

    /**
     * {@link MethodBinderFactory}を設定する。
     *
     * @param methodBinderFactory {@link MethodBinderFactory}
     */
    public void setMethodBinderFactory(final MethodBinderFactory methodBinderFactory) {
        this.methodBinderFactory = methodBinderFactory;
    }
}
