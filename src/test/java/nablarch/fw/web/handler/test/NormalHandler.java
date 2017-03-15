package nablarch.fw.web.handler.test;

import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpRequestHandler;
import nablarch.fw.web.HttpResponse;

public class NormalHandler implements HttpRequestHandler {
    public HttpResponse handle(HttpRequest request, ExecutionContext context) {
        int code = Integer.valueOf(request.getParam("code")[0]);
        String[] pathParams = request.getParam("path");
        if (pathParams != null) {
            return new HttpResponse(code, pathParams[0]);
        } else {
            return new HttpResponse(code);
        }
    }
}
