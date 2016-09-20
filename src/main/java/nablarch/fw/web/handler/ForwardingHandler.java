package nablarch.fw.web.handler;

import java.util.ArrayList;
import java.util.List;

import nablarch.common.util.WebRequestUtil;
import nablarch.core.ThreadContext;
import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpRequestHandler;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.ResourceLocator;

/**
 * 内部フォーワード処理を行うHTTPリクエストハンドラクラス。
 * 
 * @author Iwauo Tajima <iwauo@tis.co.jp>
 */
public class ForwardingHandler implements HttpRequestHandler {

    /** {@inheritDoc}
     * この実装では、レスポンスコンテンツパスのスキームが
     * "forward://" であった場合に内部フォーワード処理を行う。
     */
    @SuppressWarnings("rawtypes")
    public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
        List<Handler> snapshot = new ArrayList<Handler>();
        snapshot.add(this);
        snapshot.addAll(ctx.getHandlerQueue());
        
        HttpResponse res = ctx.handleNext(req);
        if (needsForwarding(res)) {
            ctx.getHandlerQueue().clear();
            ctx.getHandlerQueue().addAll(snapshot);
            res = doForwarding(res, req, ctx);
        }
        return res;
    }
    
    /**
     * このHTTPレスポンスを処理する際に内部フォーワードが必要か？
     * @param res HTTPレスポンスオブジェクト
     * @return 内部フォーワードが必要な場合はtrue
     */
    private boolean needsForwarding(HttpResponse res) {
        ResourceLocator contentPath = res.getContentPath();
        return contentPath != null && contentPath.getScheme().equals("forward");
    }

    /**
     * 内部フォーワードを処理する。
     * <pre>
     * コンテンツパスのスキーム値が "forward" であった場合、
     * 指定されたパスをリクエストURIに設定した上で、
     * リクエストプロセッサ以降の処理を再実行し、その結果を返す。
     * 
     * </pre>
     * @param res HTTPレスポンスオブジェクト
     * @param req HTTPリクエストオブジェクト
     * @param ctx 実行コンテキストオブジェクト
     * @return フォーワード結果
     */
    private HttpResponse
    doForwarding(HttpResponse res, HttpRequest req, ExecutionContext ctx) {
        ResourceLocator path = res.getContentPath();
        String uri = path.isRelative()
                   ? req.getRequestUri()
                        .replaceAll("/[^/]*$", "/" + path.getPath())
                   : path.getPath();
        int currentStatus = res.getStatusCode();
        req.setRequestUri(uri);
        ThreadContext.setInternalRequestId(
            WebRequestUtil.getRequestId(req.getRequestPath())
        );
        res = ctx.handleNext(req);
        if (currentStatus > res.getStatusCode()) {
            // This replacement of statusCode works well in most cases
            // but might be problematic in some situation.
            res.setStatusCode(currentStatus);
        }
        return res;
    }

}
