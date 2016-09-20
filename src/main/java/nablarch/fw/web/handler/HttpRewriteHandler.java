package nablarch.fw.web.handler;

import java.util.ArrayList;
import java.util.List;

import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;

/**
 * HTTPリクエスト中のリクエストパス、および、
 * HTTPレスポンス中のコンテンツパスに対する書き換え処理を行うハンドラ。
 * 
 * このハンドラでは、往路処理で{@link HttpRequest}中のリクエストパスの置換を行う。
 * もし、リクエストパスの置換が行われた場合は、
 * 復路処理で{@link HttpResponse}中のコンテンツパスの置換を行う。
 * 
 * @author Iwauo Tajima
 */
public class HttpRewriteHandler implements Handler<HttpRequest, HttpResponse> {

    // ----------------------------------------------------- properties
    /** リクエストパスリライト定義  */
    private final List<HttpRequestRewriteRule>
        requestPathRewriteRules = new ArrayList<HttpRequestRewriteRule>();
    
    /** コンテンツパスリライト定義  */
    private final List<ContentPathRewriteRule>
        contentPathRewriteRules = new ArrayList<ContentPathRewriteRule>();
    
    
    // --------------------------------------- implementation of Handler I/F
    /** {@inheritDoc}
     * このハンドラでは、往路処理で{@link HttpRequest}中のリクエストパスの置換を行う。
     * もし、リクエストパスの置換が行われた場合は、
     * 復路処理で{@link HttpResponse}中のコンテンツパスの置換を行う。
     */
    public HttpResponse handle(HttpRequest request, ExecutionContext context) {
        String path = rewriteRequestPath(request, context);
        
        if (path.startsWith("servlet://") || path.startsWith("redirect://")) {
            return new HttpResponse(path);
        }
        
        HttpResponse response = context.handleNext(request);
        // レスポンスにコンテンツパスが設定されていれば、その書き換え処理を行う。
        if (response.getContentPath() != null) {
            rewriteContentPath(response, context);
        }
        return response;
    }
    
    /**
     * このハンドラに設定された書き換えルールに従って、
     * HTTPリクエスト中のリクエストパスの書き換え処理を行う。
     * @param request HTTPリクエストオブジェクト
     * @param context 実行コンテキスト
     * @return リライト処理が行われた場合はtrue
     */
    private String rewriteRequestPath(HttpRequest request, ExecutionContext context) {
        for (HttpRequestRewriteRule rule : requestPathRewriteRules) {
            String rewrittenPath = rule.rewrite(request, context);
            // ルールが適用された時点でループを抜ける。
            if (rewrittenPath != null) {
                return rewrittenPath;
            }
        }
        return request.getRequestPath();
    }
    
    /**
     * このハンドラに設定された書き換えルールに従って、
     * HTTPレスポンス中のコンテンツパスに対する書き換え処理を行う。
     * @param response HTTPリクエストオブジェクト
     * @param context 実行コンテキスト
     * @return リライト後コンテンツパス
     */
    private String rewriteContentPath(HttpResponse response, ExecutionContext context) {
        for (ContentPathRewriteRule rule : contentPathRewriteRules) {
            String path = rule.rewrite(response, context);
            // ルールが適用された時点でループを抜ける。 
            if (path != null) {
                return path;
            }
        }
        return response.getContentPath().toString();
    }
    
    // --------------------------------------------------------- accessors
    /**
     * リクエストパスの置換ルールを設定する。
     * 
     * 以前の設定はクリアされる。
     * 
     * @param rules リクエストパスの置換ルール
     * @return このオブジェクト自体。
     */
    public HttpRewriteHandler
    setRequestPathRewriteRules(List<HttpRequestRewriteRule> rules) {
        if (rules == null) {
            throw new IllegalArgumentException(
                "the argument [rules] must not be null."
            );
        }
        requestPathRewriteRules.clear();
        requestPathRewriteRules.addAll(rules);
        return this;
    }
    
    /**
     * リクエストパスの置換ルールを設定する。
     * @param rule リクエストパスの置換ルール
     * @return このオブジェクト自体
     */
    public HttpRewriteHandler
    addRequestPathRewriteRule(HttpRequestRewriteRule rule) {
        if (rule == null) {
            throw new IllegalArgumentException(
                "the argument [rule] must not be null."
            );
        }
        requestPathRewriteRules.add(rule);
        return this;
    }

    /**
     * コンテンツパスの置換ルールを設定する。
     * 
     * 以前の設定はクリアされる。
     * 
     * @param rules リクエストパスの置換ルール
     * @return このオブジェクト自体。
     */
    public HttpRewriteHandler
    setContentPathRewriteRules(List<ContentPathRewriteRule> rules) {
        if (rules == null) {
            throw new IllegalArgumentException(
                "the argument [rules] must not be null."
            );
        }
        contentPathRewriteRules.clear();
        contentPathRewriteRules.addAll(rules);
        return this;
    }
    
    /**
     * コンテンツパスの置換ルールを設定する。
     * @param rule コンテンツパスの置換ルール
     * @return このオブジェクト自体
     */
    public HttpRewriteHandler
    addContentPathRewriteRule(ContentPathRewriteRule rule) {
        if (rule == null) {
            throw new IllegalArgumentException(
                "the argument [rule] must not be null."
            );
        }
        contentPathRewriteRules.add(rule);
        return this;
    }
}
