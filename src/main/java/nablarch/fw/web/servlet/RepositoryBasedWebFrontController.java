package nablarch.fw.web.servlet;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import nablarch.core.repository.SystemRepository;

/**
 * リポジトリ機能を使用して構築した{@link WebFrontController}を保持し、
 * そこに対してリクエスト処理を委譲するリクエストコントローラー
 *
 * <pre>
 * component-nameのパラメータ値にコントローラ名を設定することで、設定した名前を元に移譲するWebFrontControllerを取得することができる。
 * デフォルトではwebFrontControllerという名前で移譲するWebFrontControllerを取得する。
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
 *     &lt;filter-name>webEntryPoint&lt;/filter-name>
 *     &lt;filter-class>
 *       nablarch.fw.web.servlet.RepositoryBasedWebFrontController
 *     &lt;/filter-class>
 *     &lt;init-param>
 *       &lt;param-name>component-name&lt;/param-name>
 *       &lt;param-value>webFrontController&lt;/param-value>
 *     &lt;/init-param>
 *   &lt;/filter>
 *   &lt;filter-mapping>
 *     &lt;filter-name>webEntryPoint&lt;/filter-name>
 *    &lt;url-pattern>/*&lt;/url-pattern>
 *   &lt;/filter-mapping>
 * &lt;/web-app>
 *
 * </pre>
 * @see WebFrontController
 * @author Iwauo Tajima
 */
public class RepositoryBasedWebFrontController implements Filter {

    /**{@inheritDoc}
     * この実装では、保持しているリクエストコントローラに対して
     * 処理を委譲するのみ。
     */
    public void destroy() {
        controller.destroy();
    }

    /**{@inheritDoc}
     * この実装では、保持しているリクエストコントローラに対して
     * 処理を委譲するのみ。
     */
    public void doFilter(ServletRequest  request,
                         ServletResponse response,
                         FilterChain     chain)
    throws IOException, ServletException {
        controller.doFilter(request, response, chain);
    }

    /**{@inheritDoc}
     * リポジトリ機能を用いてWebFrontControllerのインスタンスを初期化し、
     * 以降の全ての処理をそこへ委譲する。
     */
    public void init(FilterConfig filterConfig) throws ServletException {
        String controllerName = filterConfig.getInitParameter("controllerName");
        if (controllerName == null) {
            controllerName="webFrontController";
        }

        controller = SystemRepository.get(controllerName);
        if (controller == null) {
            throw new ServletException(
            "webFrontController must be configured in SystemRepository."
            );
        }
        controller.setServletFilterConfig(filterConfig);
    }
    
    /**
     * 処理を委譲するリクエストコントローラのインスタンス
     */
    private WebFrontController controller = null;

}
