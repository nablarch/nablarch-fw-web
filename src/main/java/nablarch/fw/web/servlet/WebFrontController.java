package nablarch.fw.web.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
 * &lt;web-app xmlns="http://java.sun.com/xml/ns/javaee"
 *          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 *          xsi:schemaLocation="http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_2_5.xsd"
 *          version="2.5">
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
public class WebFrontController
extends HandlerQueueManager<WebFrontController>
implements Filter {
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
    public void doFilter(ServletRequest  servletRequest,
                         ServletResponse servletResponse,
                         FilterChain     chain)
    throws ServletException, IOException {
        
        ServletExecutionContext context = new ServletExecutionContext(
                (HttpServletRequest) servletRequest,
                (HttpServletResponse) servletResponse,
                config.getServletContext());
        
        HttpRequest request = context.getHttpRequest();
        context.setHandlerQueue(this.handlerQueue)
               .handleNext(request);
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
    private List<Handler> handlerQueue = new ArrayList<Handler>();

    /**
     * サーブレットフィルタの設定情報を設定する.
     * @param config 設定情報
     */
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
    public void destroy() {
        config = null;
    }
}
