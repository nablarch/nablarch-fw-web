package nablarch.fw.web.handler.csrf;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import nablarch.core.util.annotation.Published;
import nablarch.fw.web.HttpRequest;

/**
 * HTTPメソッドをもとにしてHTTPリクエストがCSRFトークンの検証対象となるか判定を行うクラス。
 *
 * <p>
 * デフォルトでは以下のHTTPメソッドをCSRFトークンの検証対象外とする。
 * <ul>
 *     <li>GET</li>
 *     <li>HEAD</li>
 *     <li>TRACE</li>
 *     <li>OPTIONS</li>
 * </ul>
 * </p>
 *
 * @author Uragami Taichi
 *
 */
public class HttpMethodVerificationTargetMatcher implements VerificationTargetMatcher {

    /**
     * allowedMethodsのデフォルト値
     */
    private static final Set<String> DEFAULT_ALLOWED_METHODS;
    static {
        Set<String> allowedMethods = new HashSet<String>();
        allowedMethods.add("GET");
        allowedMethods.add("HEAD");
        allowedMethods.add("TRACE");
        allowedMethods.add("OPTIONS");
        DEFAULT_ALLOWED_METHODS = Collections.unmodifiableSet(allowedMethods);
    }

    /**
     * CSRFトークンの検証対象外となるHTTPメソッドの集合
     */
    private Set<String> allowedMethods = DEFAULT_ALLOWED_METHODS;

    @Override
    public boolean match(HttpRequest request) {
        return !allowedMethods.contains(request.getMethod());
    }

    /**
     * CSRFトークンの検証対象外となるHTTPメソッドの集合を設定する。
     * 
     * @param allowedMethods CSRFトークンの検証対象外となるHTTPメソッドの集合
     */
    public void setAllowedMethods(Set<String> allowedMethods) {
        this.allowedMethods = allowedMethods;
    }
}
