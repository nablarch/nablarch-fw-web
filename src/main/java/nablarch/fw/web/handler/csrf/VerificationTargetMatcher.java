package nablarch.fw.web.handler.csrf;

import nablarch.core.util.annotation.Published;
import nablarch.fw.web.HttpRequest;

/**
 * HTTPリクエストがCSRFトークンの検証対象となるか判定を行うインターフェース。
 * 
 * @author Uragami Taichi
 *
 */
@Published(tag = "architect")
public interface VerificationTargetMatcher {

    /**
     * HTTPリクエストがCSRFトークンの検証対象となるか判定を行う。
     * 
     * @param request HTTPリクエスト
     * @return 検証対象であればtrue
     */
    boolean match(HttpRequest request);
}
