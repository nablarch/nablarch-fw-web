package nablarch.fw.web.i18n;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import nablarch.core.repository.SystemRepository;
import nablarch.fw.web.servlet.WebFrontController;

/**
 * {@link ServletContextCreator}の基本実装クラス。<br />
 * 
 * セッションおよびリポジトリ経由でサーブレットコンテキストを取得する。<br />
 * サーブレットコンテキストを取得できない場合は{@link IllegalStateException}を送出する。
 * 
 * @author Naoki Yamamoto
 */
public class BasicServletContextCreator implements ServletContextCreator {

    @Override
    public ServletContext create(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            return session.getServletContext();
        }
        // 並行リクエストによってsessionがinvalidateされている場合は、
        // リポジトリ経由でServletContextを取得する。
        // NOTE: テスト時のセットアップが煩雑化するので、この方法を使用するのは
        //       session経由の取得ができない場合のみとしている。
        WebFrontController controller = SystemRepository.get("webFrontController");
        if (controller != null && controller.getServletFilterConfig() != null) {
            return controller.getServletFilterConfig().getServletContext();
        }
        
        // サーブレットコンテナ環境下ではこの処理には到達しないが、
        // 自動テスト環境などではリポジトリ経由でサーブレットコンテキストを取得できず、ここまで到達する可能性がある。
        throw new IllegalStateException("servletContext was not found.");
    }

}
