package nablarch.fw.web.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import nablarch.core.util.annotation.Published;
import nablarch.fw.Handler;
import nablarch.fw.HandlerQueueManager;
import nablarch.fw.web.HttpMethodBinding;
import nablarch.fw.web.HttpRequest;

/**
 * アプリケーションサーバにデプロイして使用するリクエストコントローラ。
 * <pre>
 * 本フレームワークをTomcat/Websphere等のアプリケーションサーバ上で使用する際に、
 * サーブレットフィルタとしてデプロイして使用するリクエストエントリポイントである。
 * 各HTTPリクエスト毎に下記の処理を行う。
 *   1. HttpServletRequestオブジェクトをラップした
 *      HttpRequest, ExecutionContext オブジェクトを生成する。
 *   2. それらを引数としてリクエストプロセッサに処理を委譲する。
 *   3. その結果(HttpResponseオブジェクトの内容)に従って、
 *      HTTPクライアントに対するレスポンス処理を行う。
 * リクエストプロセッサの初期化処理は、本クラスのサブクラスを作成し、
 * オーバライドしたinit()メソッドの中で行う。
 * 本サーブレットフィルタに処理が委譲された場合、必ずレスポンスかフォーワードを行う。
 * このため、後続のサーブレットフィルタチェインに処理が委譲されることは無い。
 *
 * -------------------------------------
 * デプロイメントディスクリプタの記述例
 * -------------------------------------
 * &lt;?xml version="1.0" encoding="UTF-8"?>
 * &lt;web-app xmlns="https://jakarta.ee/xml/ns/jakartaee"
 *          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 *          xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee https://jakarta.ee/xml/ns/jakartaee/web-app_6_0.xsd"
 *          version="6.0">
 *   &lt;display-name>w8&lt;/display-name>
 *   &lt;description>
 *     The default application-context for w8.http-based applications.
 *   &lt;/description>
 *   &lt;filter>
 *     &lt;filter-name>WebFrontController&lt;/filter-name>
 *     &lt;filter-class>
 *       nablarch.fw.web.servlet.WebFrontController
 *     &lt;/filter-class>
 *   &lt;/filter>
 *   &lt;filter-mapping>
 *     &lt;filter-name>WebFrontController&lt;/filter-name>
 *    &lt;url-pattern>/*&lt;/url-pattern>
 *   &lt;/filter-mapping>
 * &lt;/web-app>
 *
 * </pre>
 *
 * @author Iwauo Tajima <iwauo@tis.co.jp>
 */
@SuppressWarnings("JavadocLinkAsPlainText")
public class WebFrontController
extends HandlerQueueManager<WebFrontController>
implements Filter {
    /**
     * セッション生成を防止する機能を有効にするかどうかのフラグ。
     */
    private boolean preventSessionCreation;

    /**
     * デフォルトコンストラクタ
     */
    public WebFrontController() {
        setMethodBinder(new HttpMethodBinding.Binder());
    }
    
    //----------------------------------- Implementation of Servlet filter API
    /**
     * {@inheritDoc}
     * <pre>
     * 本クラスの実装では、HTTPリクエスト毎に下記の処理を行う。
     *   1. HttpServletRequestオブジェクトをラップした
     *      HttpRequest, ExecutionContext オブジェクトを生成する。
     *   2. それらを引数としてリクエストプロセッサに処理を委譲する。
     *   3. その結果(HttpResponseオブジェクトの内容)に従って、
     *      HTTPクライアントに対するレスポンス処理を行う。
     * </pre>
     */
    @SuppressWarnings("RedundantThrows")
    @Published(tag = "architect")
    public void doFilter(ServletRequest  servletRequest,
                         ServletResponse servletResponse,
                         FilterChain     chain)
    throws ServletException, IOException {

        ServletExecutionContext context = new ServletExecutionContext(
                applyPreventingSessionCreation((HttpServletRequest) servletRequest),
                (HttpServletResponse) servletResponse,
                config.getServletContext());
        
        HttpRequest request = context.getHttpRequest();
        context.setHandlerQueue(this.handlerQueue)
               .handleNext(request);
    }

    /**
     * セッション生成防止機能が有効な場合は、指定したリクエストオブジェクトにセッション生成防止機能を適用する。
     * <p/>
     * 機能が無効の場合は、受け取ったリクエストオブジェクトをそのまま返します。
     *
     * @param request 適用対象のリクエストオブジェクト
     * @return 必要に応じてセッション生成防止機能が適用されたリクエストオブジェクト
     */
    private HttpServletRequest applyPreventingSessionCreation(HttpServletRequest request) {
        return preventSessionCreation
                ? new PreventSessionCreationHttpServletRequestWrapper(request)
                : request;
    }

    /**
     * {@inheritDoc}
     * 本クラスの実装では、リポジトリ上にコンポーネント"webFrontController"
     * が存在すれば、そのインスタンスを以降の処理で使用する。
     * 存在しない場合は、このインスタンスをそのまま使用する。
     */
    public void init(FilterConfig config) {
        this.config = config;
    }
    
    @SuppressWarnings("rawtypes")
    @Override
    public List<Handler> getHandlerQueue() {
        return handlerQueue;
    }
    
    /** ハンドラキュー */
    @SuppressWarnings("rawtypes")
    private final List<Handler> handlerQueue = new ArrayList<>();

    /**
     * サーブレットフィルタの設定情報を設定する.
     * @param config 設定情報
     */
    @Published(tag = "architect")
    public void setServletFilterConfig(FilterConfig config) {
        this.config = config;
    }
    
    /**
     * サーブレットフィルタの設定情報を取得する。
     * @return 設定情報
     */
    public FilterConfig getServletFilterConfig() {
        return config;
    }
    
    /** フィルタ設定 */
    private FilterConfig config = null;
    
    /**
     * {@inheritDoc}
     * <pre>
     * 本クラスのdestroy()メソッドでは何も行わない。
     * </pre>
     */
    @Published(tag = "architect")
    public void destroy() {
        config = null;
    }

    /**
     * セッション生成を防止する機能を有効にするかどうかを設定する。
     * @param preventSessionCreation 有効にする場合は {@code true}
     */
    public void setPreventSessionCreation(boolean preventSessionCreation) {
        this.preventSessionCreation = preventSessionCreation;
    }
}
