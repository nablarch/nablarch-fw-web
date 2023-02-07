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
        String controllerName = filterConfig.getInitParameter("component-name");
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
