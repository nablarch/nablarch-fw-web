package nablarch.common.web.token;

import nablarch.fw.web.servlet.NablarchHttpServletRequestWrapper;
import nablarch.fw.web.servlet.ServletExecutionContext;

/**
 * 二重サブミットトークンの管理を行うクラス
 *
 * @author Tsuyoshi Kawasaki
 */
public interface TokenManager {

    /**
     * トークンを保存する。
     *
     * @param serverToken サーバで払い出したトークン
     * @param request     リクエスト
     */
    void saveToken(String serverToken, NablarchHttpServletRequestWrapper request);

    /**
     * トークンが有効であるかを判定する。
     *
     * @param clientToken クライアントから送信されたトークン
     * @param context     実行コンテキスト
     * @return トークンが有効な場合はtrue、有効でない場合はfalse
     */
    boolean isValidToken(String clientToken, ServletExecutionContext context);
}
