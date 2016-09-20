package nablarch.fw.web.handler;

import nablarch.fw.ExecutionContext;
import nablarch.fw.Handler;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;

/**
 * 携帯端末からのアクセスに対して、以下の処理を行うハンドラ。
 * 
 * <pre>
 * - 遷移先のJSPページで、javascriptを使用しないページを出力させる
 *   フラグ(nablarch_jsUnsupported)をリクエストスコープ変数に設定する。
 * 
 * - リクエストパラメータ中に"nablarch_uri_override_"で始まる名前のパラメータが
 *   存在した場合、パラメータ名中の残りの文字列でリクエストパスを置換する。
 * </pre>
 * 
 * @author Iwauo Tajima
 */
public class KeitaiAccessHandler implements Handler<HttpRequest, HttpResponse> {

    // -------------------------------------------------------- properties
    /** HTTPリクエスト・レスポンスの書き換えを行うハンドラ  */
    private final HttpRewriteHandler rewriteHandler;
    
    /** HTTPリクエスト中のリクエストパスに対する書き換えルール */
    private final HttpRequestRewriteRule requestPathRewriteRule;
    
    // -------------------------------------------------------- constants
    /**
     * javascriptを使用できない端末を想定した挙動に変更する際に使用する
     * リクエストスコープ上のフラグ変数の名称
     */
    public static final String JS_UNSUPPORTED_FLAG_NAME = "nablarch_jsUnsupported";
    
    /** 
     * javascriptが利用できない場合に、遷移先URIおよび、submit_button パラメータの
     * 値を保持するリクエストパラメータの接頭辞
     * */
    public static final String URI_OVERRIDE_PRAM_PREFIX = "nablarch_uri_override_";

   
    // -------------------------------------------------------- constructors
    /**
     * デフォルトコンストラクタ
     */
    public KeitaiAccessHandler() {
        requestPathRewriteRule = new HttpRequestRewriteRule()
            .setPattern("^.*")
            .addCondition("%{paramNames} " + URI_OVERRIDE_PRAM_PREFIX
                                           + "([^\\s|]+)\\|([^\\s,]+)")
            .setRewriteTo("${paramNames:2}")
            .addExport("%{param:nablarch_submit} ${paramNames:1}");

        rewriteHandler = new HttpRewriteHandler()
            .addRequestPathRewriteRule(requestPathRewriteRule);
    }
    
    // ----------------------------------------- implementation of Handler I/F
    /** {@inheritDoc}
     *  本ハンドラに対する設定に従い、 {@link HttpRewriteHandler} による
     *  リクエストパスとコンテンツパスに対する書き換え処理を行う。
     */
    public HttpResponse handle(HttpRequest request, ExecutionContext context) {
        context.setRequestScopedVar(JS_UNSUPPORTED_FLAG_NAME, "true");
        return rewriteHandler.handle(request, context);
    }
}
