package nablarch.fw.web.handler;

import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpRequestHandler;
import nablarch.fw.web.HttpResponse;

/**
 * 何もせず次のハンドラに処理を委譲するハンドラ
 */
public class PassThroughHandler implements HttpRequestHandler {

    @Override
    public HttpResponse handle(HttpRequest request, ExecutionContext context) {
        return context.handleNext(request);
    }
}
