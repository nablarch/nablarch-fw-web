package nablarch.common.web.session;

import nablarch.fw.ExecutionContext;

/**
 * セッションストアの有効期限を管理するインターフェース。
 */
public interface Expiration {

    /**
     * 有効期限切れか否かを判定する。
     *
     * @param sessionID       セッションID
     * @param currentDateTime 現在日時
     * @param context         実行コンテキスト
     * @return 有効期限を超えセッションが無効になっている場合、true
     */
    boolean isExpired(String sessionID, long currentDateTime, ExecutionContext context);

    /**
     * 有効期限を保存する。
     *
     * @param sessionId          セッションID
     * @param expirationDateTime 有効期限
     * @param context            実行コンテキスト
     */
    void saveExpirationDateTime(String sessionId, long expirationDateTime, ExecutionContext context);

}
