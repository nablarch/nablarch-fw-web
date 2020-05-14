package nablarch.common.web.csrf;

import nablarch.common.web.WebConfig;
import nablarch.common.web.WebConfigFinder;
import nablarch.common.web.session.SessionUtil;
import nablarch.core.util.annotation.Published;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.handler.CsrfTokenVerificationHandler;

/**
 * CSRFトークンに関するユーティリティ。
 * 
 * @author Uragami Taichi
 *
 */
@Published(tag = "architect")
public final class CsrfTokenUtil {

    /**
     * 本クラスはインスタンスを生成しない。
     */
    private CsrfTokenUtil() {
    }

    /**
     * CSRFトークンをセッションストアから取得する。
     * 
     * @param context 実行コンテキスト
     * @return CSRFトークン。セッションストアに存在しない場合は{@code null}
     */
    public static String getCsrfToken(ExecutionContext context) {
        WebConfig webConfig = WebConfigFinder.getWebConfig();
        return SessionUtil.orNull(context, webConfig.getCsrfTokenSessionStoredVarName());
    }

    /**
     * CSRFトークンをHTTPリクエストヘッダーへ設定する際に使用する名前を取得する。
     * 
     * @return CSRFトークンをHTTPリクエストヘッダーへ設定する際に使用する名前
     */
    public static String getHeaderName() {
        WebConfig webConfig = WebConfigFinder.getWebConfig();
        return webConfig.getCsrfTokenHeaderName();
    }

    /**
     * CSRFトークンをHTTPリクエストパラメーターへ設定する際に使用する名前を取得する。
     * 
     * @return CSRFトークンをHTTPリクエストパラメーターへ設定する際に使用する名前
     */
    public static String getParameterName() {
        WebConfig webConfig = WebConfigFinder.getWebConfig();
        return webConfig.getCsrfTokenParameterName();
    }

    /**
     * CSRFトークンを再生成する。
     * 
     * <p>
     * このメソッドはセキュリティのために用意されている。
     * 悪意のある人がCSRFトークンとそれを保持しているセッションのセッションIDをなんらかの方法で利用者に送り込み、
     * 利用者がこれに気づかずにログインをしたとする。
     * このときCSRFトークンが再生成されていないと、悪意のあるウェブサイトにCSRFトークンを仕込んだ罠ページを用意し、
     * 利用者にリンクのクリックなどの操作をさせることで利用者の意図しない攻撃リクエストを送信させることができてしまう。
     * これを防ぐためにはログイン時にCSRFトークンを再生成しなくてはならない。
     * </p>
     * 
     * <p>
     * ログイン時にセッションを破棄して再生成する実装であればこのメソッドを利用する必要はない。
     * セッションの破棄と共にCSRFトークンも破棄され、その後のページ表示時に新しいCSRFトークンが生成されるためである。
     * ログイン時にセッションそのものの破棄ではなくセッションIDの再生成を行うにとどめる実装の場合は、
     * このメソッドを利用してCSRFトークンも再生成することを推奨する。
     * </p>
     * 
     * @param context 実行コンテキスト
     */
    public static void regenerateCsrfToken(ExecutionContext context) {
        context.setRequestScopedVar(CsrfTokenVerificationHandler.REQUEST_REGENERATE_KEY,
                Boolean.TRUE);
    }
}
