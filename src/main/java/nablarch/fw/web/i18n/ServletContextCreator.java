package nablarch.fw.web.i18n;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

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
