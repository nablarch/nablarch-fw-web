package nablarch.fw.web.handler;

import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;

public class InternalMonitor implements Handler<HttpRequest, HttpResponse> {
    public static HttpRequest      request  = null;
    public static ExecutionContext context  = null;
    public static HttpResponse     response = null;
    public static Throwable        error    = null;

    public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
        try {
            request = req;
            context = ctx;
            return (response = ctx.handleNext(req));
            
        } catch (RuntimeException e) {
            error = e;
            throw e;
            
        } catch (Error e) {
            error = e;
            throw e;
        }
    }
}
