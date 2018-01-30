package nablarch.fw.web.handler.responsewriter;

import nablarch.core.util.annotation.Published;
import nablarch.fw.web.handler.HttpResponseHandler;
import nablarch.fw.web.servlet.ServletExecutionContext;

import javax.servlet.ServletException;
import java.io.IOException;

/**
 * HTTPレスポンスの書き込みを行うインタフェース。
 *
 * レスポンスの種類に応じて、処理を行うか否かを判定する。
 * 処理対象と判定した場合、レスポンス出力が実行される。
 * JSP以外で、任意のテンプレートエンジン等を使用して
 * レスポンスを出力する用途を想定している。
 *
 * @author Tsuyoshi Kawasaki
 * @see HttpResponseHandler
 */
@Published(tag = "architect")
public interface CustomResponseWriter {

    /**
     * 処理対象のレスポンスであるか判定する。
     *
     * @param path レスポンス出力に指定されたパス(テンプレートファイルへのパス等を指す。実装依存。)
     * @param context 実行コンテキスト
     * @return 処理対象である場合、真
     */
    boolean isResponsibleTo(String path, ServletExecutionContext context);

    /**
     * レスポンスの書き込みを行う。
     *
     * @param path レスポンス出力に指定されたパス(テンプレートファイルへのパス等を指す。実装依存。)
     * @param context 実行コンテキスト
     * @throws ServletException Servlet API使用時に発生した例外
     * @throws IOException 入出力例外(ソケットI/Oエラー等)
     */
    void writeResponse(String path, ServletExecutionContext context) throws ServletException, IOException;
}
