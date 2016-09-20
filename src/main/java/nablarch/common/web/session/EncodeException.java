package nablarch.common.web.session;

import nablarch.core.util.annotation.Published;

/**
 * セッションの内容をエンコードする際に発生する例外。
 * <p/>
 * パラメータの改ざん等での発生を想定される。
 *
 * @author kawasima
 * @author tajima
 */
@Published(tag = "architect")
public class EncodeException extends RuntimeException {
    /**
     * コンストラクタ。
     *
     * @param e 元例外
     */
    public EncodeException(Throwable e) {
        super(e);
    }
}
