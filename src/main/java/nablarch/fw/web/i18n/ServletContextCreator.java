package nablarch.fw.web.i18n;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;

/**
 * サーブレットコンテキストの生成を行うインタフェース。
 * 
 * @author Naoki Yamamoto
 */
public interface ServletContextCreator {

    /**
     * サーブレットコンテキストを生成する。
     * 
     * @param request リクエスト
     * @return サーブレットコンテキスト
     */
    ServletContext create(HttpServletRequest request);
}
