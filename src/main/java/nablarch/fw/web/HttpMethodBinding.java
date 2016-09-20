package nablarch.fw.web;

import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nablarch.core.log.LogUtil;
import nablarch.core.log.Logger;
import nablarch.core.log.LoggerManager;
import nablarch.core.util.Builder;
import nablarch.fw.ExecutionContext;
import nablarch.fw.HandlerWrapper;
import nablarch.fw.MethodBinder;
import nablarch.fw.handler.MethodBinding;

/**
 * HTTPメソッドベースディスパッチャ。
 * <pre>
 * HTTPリクエストのメソッド名とリクエストURIの値をもとに、
 * 動的に委譲先となるメソッド決定する。
 *  例:
 *    [HTTPリクエストライン]               [委譲先メソッド名]
 *    GET  /foo/baa/dynamic_page.jsp  ->  getDynamicPageJsp()
 *    POST /foo/baa/message           ->  postMessage()
 *  ディスパッチ処理の詳細仕様は、{@link #handle(HttpRequest, ExecutionContext)} を参照。
 * </pre>
 *
 * @author Iwauo Tajima <iwauo@tis.co.jp>
 */
public class HttpMethodBinding
extends MethodBinding<HttpRequest, HttpResponse> implements HttpRequestHandler {
    
    /** 開発ログ */
    private static final Logger DEV_LOGGER = LoggerManager.get("DEV");
    
    /** 開発ログで出力しない属性名のパターン */
    private static final Pattern DEV_LOG_EXCLUDES = Pattern.compile(".*nablarch_.*");
    
    /**
     * HttpMethodBindingのファクトリクラス
     */
    public static class Binder implements MethodBinder<HttpRequest, HttpResponse> {
        /** {@inheritDoc} */
        public HandlerWrapper<HttpRequest, HttpResponse> bind(Object delegate) {
            return new HttpMethodBinding(delegate);
        }
    }

    /**
     * 指定されたオブジェクトに処理を委譲するディスパッチャを作成する。
     * @param delegate ディスパッチの対象となるオブジェクト
     */
    public HttpMethodBinding(Object delegate) {
        super(delegate);
    }
    
    /**
     * {@inheritDoc}
     * <pre>
     * 本クラスの実装では、コンストラクタで渡されたオブジェクトに対して、
     * HTTPリクエストのメソッド名とリクエストURIの値から
     * 実行対象のメソッドを動的に決定して処理を委譲する。
     * 委譲対象となるメソッドは、以下2つの条件に合致するものが選ばれる。
     *   1. メソッドの戻り値の型がHttpResponseかつ、引数を2つもち、
     *      それぞれの型がHttpRequest、ExecutionContextであること。
     *   2. メソッドの名前が次の文字列に一致する。
     *      (リクエストのHTTPメソッド名 もしくは "do") + (リクエストURIのリソース名)
     *      ただし、一致判定は以下の条件のもとで行われる。
     *        - メソッド名の大文字小文字は区別しない。   
     *        - リクエストURIのリソース名に含まれる"."は無視される。
     *        - 委譲先クラスのメソッド名に含まれる"_"は無視される。
     * 例:
     * =====================  ===========================================================
     *    HTTPリクエスト         委譲対象となるメソッドシグニチャの例
     * =====================  ===========================================================
     * GET /app/index.html    HttpResponse getIndexHtml(HttpRequest, ExecutionContext);
     *                        HttpResponse getIndexhtml(HttpRequest, ExecutionContext);
     *                        HttpResponse get_index_html(HttpRequest, ExecutionContext);
     *                        HttpResponse do_index_html(HttpRequest, ExecutionContext);
     *                        HttpResponse doIndexHtml(HttpRequest, ExecutionContext);
     * ---------------------  -----------------------------------------------------------
     * POST /app/message      HttpResponse postMessage(HttpRequest, ExecutionContext);
     *                        HttpResponse do_message (HttpRequest, ExecutionContext);
     * =====================  ===========================================================
     * 上記条件に該当するメソッドが存在しなかった場合、
     * 下記のHTTPメッセージに相当するHttpResponseオブジェクトを返す。
     *    HTTP/1.1 404 Not Found
     *    Content-Type: text/plain
     *    Not Found: /foo/bar/dynamic_page.jsp
     * </pre>
     * @param req HTTPリクエスト
     * @param ctx 実行コンテキスト
     * @return 委譲先メソッド
     */
    protected Method getMethodBoundTo(HttpRequest req, ExecutionContext ctx) {
        Method result;
        Matcher m = RESOURCE_NAME_IN_URI.matcher(req.getRequestPath());
        String resourceName = m.find() ? m.group(1)
                            : "";
        String methodName = (req.getMethod() + resourceName)
                            .toLowerCase()
                            .replaceAll("[^0-9a-zA-Z]", "")
                            .trim();
        result = getHandleMethod(methodName);
        if (result == null) {
            methodName = methodName.replaceFirst("^get|^post", "do");
            result = getHandleMethod(methodName);
        }
        
        if (DEV_LOGGER.isDebugEnabled()) {
            String msg;
            String delegateClassName = getDelegates(req, ctx).get(0).getClass().getName();
            if (result != null) {
                msg = Builder.concat("**** DISPATCHING METHOD **** method = [", delegateClassName, "#", result.getName(), "]");
            } else {
                msg = Builder.concat("**** DISPATCHING METHOD **** method not found. class = [", delegateClassName, "],",
                                     " method signature = [HttpResponse ", methodName, "(HttpRequest, ExecutionContext)]");
            }
            DEV_LOGGER.logDebug(msg);
        }
        
        return result;
    }

    /** URI内のリソース名に対応する正規表現 */
    private static final Pattern RESOURCE_NAME_IN_URI = Pattern.compile(
            "/([^/]*?)/?$"
    );

    @Override
    public HttpResponse handle(HttpRequest req, ExecutionContext ctx) {
        if (DEV_LOGGER.isDebugEnabled()) {
            String separator = Logger.LS + "\t\t";
            DEV_LOGGER.logDebug(Builder.concat(
                    "**** BEFORE ACTION ****",
                    Logger.LS, "\t", "request_parameter = [", LogUtil.dumpMap(req.getParamMap(), separator), "]",
                    Logger.LS, "\t", "request_scope = [", LogUtil.dumpMap(ctx.getRequestScopeMap(), separator, DEV_LOG_EXCLUDES), "]",
                    Logger.LS, "\t", "session_scope = [", LogUtil.dumpMap(ctx.getSessionScopeMap(), separator, DEV_LOG_EXCLUDES), "]"));
        }
        try {
            return super.handle(req, ctx);
            
        } finally {
            if (DEV_LOGGER.isDebugEnabled()) {
                String separator = Logger.LS + "\t\t";
                DEV_LOGGER.logDebug(Builder.concat(
                    "**** AFTER ACTION ****",
                    Logger.LS, "\t", "request_parameter = [", LogUtil.dumpMap(req.getParamMap(), separator), "]",
                    Logger.LS, "\t", "request_scope = [", LogUtil.dumpMap(ctx.getRequestScopeMap(), separator, DEV_LOG_EXCLUDES), "]",
                    Logger.LS, "\t", "session_scope = [", LogUtil.dumpMap(ctx.getSessionScopeMap(), separator, DEV_LOG_EXCLUDES), "]"));
            }
        }
    }
}
