package nablarch.fw.web.handler;

import nablarch.core.util.Builder;
import nablarch.core.util.annotation.Published;
import nablarch.fw.ExecutionContext;
import nablarch.fw.handler.RewriteRule;
import nablarch.fw.web.HttpRequest;

/**
 * {@link HttpRequest} 中のリクエストパスの書き換え処理を行うクラス。
 * 
 * @author Iwauo Tajima
 */
@Published(tag = "architect")
public class HttpRequestRewriteRule extends
RewriteRule<HttpRequest, HttpRequestRewriteRule> {
    
    

    @Override
    public String getPathToRewrite(HttpRequest request) {
        return request.getRequestPath();
    }

    @Override
    public void applyRewrittenPath(String rewrittenPath, HttpRequest request) {
        if (!rewrittenPath.startsWith("servlet://") && !rewrittenPath.startsWith("redirect://")) {
            request.setRequestPath(rewrittenPath);
        }
    }

    @Override
    protected Object
    getParam(String type, String name, HttpRequest request, ExecutionContext context) {       
        return "header".equals(type) ? request.getHeader(name)
             : "param".equals(type)  ? request.getParam(name)
             : "".equals(type)       ? getProperty(name, request)
             : super.getParam(type, name, request, context);
    }

    /**
     * HTTPリクエストオブジェクト自体に付随するプロパティを返す。
     * @param name    プロパティ名
     * @param request HTTPリクエストオブジェクト
     * @return プロパティの値
     */
    private String getProperty(String name, HttpRequest request) {
        return "paramNames".equals(name)  ? Builder.join(request.getParamMap().keySet().toArray(), ",")
             : "httpVersion".equals(name) ? request.getHttpVersion()
             : "httpMethod".equals(name)  ? request.getMethod()
             : null;
    }

    @Override
    protected void
    exportParam(String scope, String name, String value, HttpRequest req, ExecutionContext context) {
        if ("param".equals(scope)) {
            req.getParamMap().put(name, new String[]{value});
        } else {
          super.exportParam(scope, name, value, req, context);
        }
    }
}
