package nablarch.common.web.session;

import nablarch.fw.ExecutionContext;

/**
 * セッションストアの有効期限を管理するインターフェース。
 */
public interface Expiration {

    /**
     * 有効期限切れか否かを判定する。
     *
     * @param sessionId       セッションID
     * @param currentDateTime 現在日時
     * @param context         実行コンテキスト
     * @return 有効期限を超えセッションが無効になっている場合、true
     */
    boolean isExpired(String sessionId, long currentDateTime, ExecutionContext context);

    /**
     * 有効期限を保存する。
     *
     * @param sessionId          セッションID
     * @param expirationDateTime 有効期限
     * @param context            実行コンテキスト
     */
    void saveExpirationDateTime(String sessionId, long expirationDateTime, ExecutionContext context);

    /**
     * 有効期限が判定可能かどうかを判定する。
     * {@link SessionStoreHandler}の復路処理にて、別スレッドでのInvalidateを検知するために使用する。
     *
     * @param sessionId セッションID
     * @param context   実行コンテキスト
     * @return セッションが別スレッドで破棄された場合などは false
     */
    boolean isDeterminable(String sessionId, ExecutionContext context);
}
