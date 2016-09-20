package nablarch.fw.web.servlet;

import javax.servlet.http.HttpServletRequest;

/**
 * フレームワークで使用するURIの取り扱いを助けるユーティリティ。
 * 
 * @author Kiyohito Itoh
 */
public final class UriUtil {

    /** 隠蔽コンストラクタ */
    private UriUtil() {
    }

    /**
     * コンテキストルートからのパスに変換する。
     * <p/>
     * 指定されたパスが"/"始まりの場合は、何も処理せずに指定されたパスをそのまま返す。
     * 絶対URLまたは相対URLが指定された場合は、コンテキストからのパスに変換する。
     * 
     * @param path パス
     * @param request リクエスト
     * @return コンテキストルートからのパス
     */
    public static String convertToPathFromContextRoot(String path, HttpServletRequest request) {

        if (path.startsWith("/")) { // コンテキストルートからのパスとして指定された場合。

            return path;

        } else if (path.startsWith("http:") || path.startsWith("https:")) { // 絶対URLとして指定された場合。

            // スキーマの削除。
            // 例）"http://test.com/app/action/hoge" -> "test.com/app/action/hoge"
            int fromIndex = path.startsWith("http:") ? "http://".length() : "https://".length(); 
            path = path.substring(fromIndex);

            // スキーマ削除後に"/"がない場合は"/"を返す。
            // 例）"http://test.com" -> "/"
            fromIndex = path.indexOf("/");
            if (fromIndex < 0) {
                return "/";
            }

            // 先頭から1つ目の"/"以降のみ残す。
            // 例）"test.com/app/action/hoge" -> "/app/action/hoge"
            path = path.substring(fromIndex);

            // コンテキストパスを削除。コンテキストパス指定なしの場合は削除しない。
            // 例）contextPath: "/app"
            //     "/app/action/hoge" -> "/action/hoge"
            String contextPath = request.getContextPath();
            if (contextPath.length() != 1 && path.startsWith(contextPath)) {
                path = path.substring(contextPath.length());
            }

            // コンテキストパス削除後に空文字となる場合は"/"を返す。
            // 例）contextPath: "/app"
            //     "/app" -> ""
            return path.length() == 0 ? "/" : path;

        } else { // 現在のリクエストからの相対パスで指定された場合。

            // フォワードされた場合を考慮し、オリジナルのリクエストURI取得を先に試みる。
            // オリジナルのリクエストURIの取得方法は、よく知られたServlet仕様でないため
            // 参照先をここに記載しておく。
            // Java Servlet Specification 2.4 SRV.8.4.2 Forwarded Request Parameters
            // Java Servlet Specification 2.5 SRV.8.4.2 Forwarded Request Parameters
            // Java Servlet Specification 3.0 9.4.2 Forwarded Request Parameters
            String uri = (String) request.getAttribute("javax.servlet.forward.request_uri");
            if (uri == null) {
                uri = request.getRequestURI();
            }

            // コンテキストパスを削除。コンテキストパス指定なしの場合は削除しない。
            int contextPathLength = request.getContextPath().length();
            if (contextPathLength > 1) {
                uri = uri.substring(contextPathLength);
            }

            // 一番後方の"/"以降を削除。
            int lastSlashIndex = uri.lastIndexOf("/");
            if (lastSlashIndex != -1) {
                uri = uri.substring(0, lastSlashIndex);
            }

            return uri + "/" + path;
        }
    }
}
