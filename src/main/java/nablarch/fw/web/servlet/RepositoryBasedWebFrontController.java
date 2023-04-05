package nablarch.fw.web.servlet;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import nablarch.core.repository.SystemRepository;

/**
 * リポジトリ機能を使用して構築した{@link WebFrontController}を保持し、
 * そこに対してリクエスト処理を委譲するリクエストコントローラー.<br>
 * controllerNameのパラメータ値にコントローラ名を設定することで、設定した名前を元に移譲するWebFrontControllerを取得することができる。
 * デフォルトではwebFrontControllerという名前で移譲するWebFrontControllerを取得する。
 * <pre>{@code
 * -------------------------------------
 * デプロイメントディスクリプタの記述例
 * -------------------------------------
 * <?xml version="1.0" encoding="UTF-8"?>
 * <web-app xmlns="https://jakarta.ee/xml/ns/jakartaee"
 *          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
 *          xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee
 *          web-app_6_0.xsd"
 *          version="6.0">
 *   <display-name>w8</display-name>
 *   <description>
 *     The default application-context for w8.http-based applications.
 *   </description>
 *   <filter>
 *     <filter-name>webEntryPoint</filter-name>
 *     <filter-class>
 *       nablarch.fw.web.servlet.RepositoryBasedWebFrontController
 *     </filter-class>
 *     <init-param>
 *       <param-name>controllerName</param-name>
 *       <param-value>otherNameController</param-value>
 *     </init-param>
 *   </filter>
 *   <filter-mapping>
 *     <filter-name>webEntryPoint</filter-name>
 *    <url-pattern>/*</url-pattern>
 *   </filter-mapping>
 * </web-app>
 *  }</pre>
 *

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
