package nablarch.fw.web.handler;

import nablarch.core.util.annotation.Published;
import nablarch.fw.ExecutionContext;
import nablarch.fw.handler.RewriteRule;
import nablarch.fw.web.HttpResponse;

/**
 * HTTPレスポンスオブジェクト中のコンテンツパス文字列の置換ルール。
 * 
 * @see HttpRewriteHandler
 * @author Iwauo Tajima
 */
@Published(tag = "architect")
public class ContentPathRewriteRule
extends RewriteRule<HttpResponse, ContentPathRewriteRule> {

    @Override
    public String getPathToRewrite(HttpResponse response) {
        return response.getContentPath().toString();
    }

    @Override
    public void applyRewrittenPath(String rewrittenPath, HttpResponse response) {
        response.setContentPath(rewrittenPath);
    }
    
    @Override
    protected Object
    getParam(String type, String name, HttpResponse response, ExecutionContext context) {       
        return "header".equals(type) ? response.getHeader(name)
             : "".equals(type) && "statusCode".equals(name) ? response.getStatusCode()
             : super.getParam(type, name, response, context);
    }
}
