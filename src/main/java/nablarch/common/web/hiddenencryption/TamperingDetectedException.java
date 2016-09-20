package nablarch.common.web.hiddenencryption;

import nablarch.core.util.annotation.Published;

/**
 * hiddenタグの暗号化機能で改竄を検知した場合に発生する例外。
 * @author Kiyohito Itoh
 */
@Published(tag = "architect")
public class TamperingDetectedException extends RuntimeException {
    /**
     * コンストラクタ。
     * @param message メッセージ
     */
    public TamperingDetectedException(String message) {
        super(message);
    }
    /**
     * コンストラクタ。
     * @param message メッセージ
     * @param cause 原因
     */
    public TamperingDetectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
