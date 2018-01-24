package nablarch.common.web.token;

import nablarch.core.util.annotation.Published;

/**
 * トークンの生成を行うインタフェース。
 * @author Kiyohito Itoh
 */
@Published(tag = "architect")
public interface TokenGenerator {
    /**
     * トークンを生成する。
     * @return トークン
     */
    String generate();
}
