package nablarch.common.web.session;

import nablarch.core.util.annotation.Published;

/**
 * セッションからバイト列へ直列化、
 * およびバイト列からセッション内容の復元を行う際に用いられる
 * モジュールが実装するインターフェース。
 *
 * @author kawasima
 * @author tajima
 */
@Published(tag = "architect")
public interface StateEncoder {
    /**
     * バイト列へのエンコードを行う。
     *
     * @param <T> 総称型
     * @param obj エンコードするオブジェクト
     * @return エンコード後のバイト配列
     */
    <T> byte[] encode(T obj);

    /**
     * バイト列からデコードを行う。
     *
     * @param <T> 総称型
     * @param dmp デコードするバイト配列
     * @param type クラスタイプ
     * @return オブジェクト
     */
    <T> T decode(byte[] dmp, Class<T> type);
}
