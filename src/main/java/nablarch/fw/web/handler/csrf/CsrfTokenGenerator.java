package nablarch.fw.web.handler.csrf;

import nablarch.core.util.annotation.Published;

/**
 * CSRFトークンの生成を行うインターフェース。
 * 
 * <p>
 * このインターフェースの実装クラスが生成する値は暗号論的に安全なものでなければいけない。
 * 例えば{@link java.security.SecureRandom}を使用して値を生成するなど。
 * </p>
 * 
 * @author Uragami Taichi
 *
 */
@Published(tag = "architect")
public interface CsrfTokenGenerator {

    /**
     * CSRFトークンを生成して返す。
     * 
     * @return 生成されたCSRFトークン
     */
    String generateToken();
}
