package nablarch.fw.web.handler.responsewriter;

import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.handler.HttpResponseHandler;
import nablarch.fw.web.servlet.ServletExecutionContext;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
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
public interface CustomResponseWriter {

    /**
     * 処理対象のレスポンスであるか判定する。
     *
     * @param response HTTPレスポンス
     * @param context 実行コンテキスト
     * @return 処理対象である場合、真
     */
    boolean isResponsibleTo(HttpResponse response, ServletExecutionContext context);

    /**
     * レスポンスの書き込みを行う。
     *
     * レスポンスの書き込みには{@link HttpResponse}ではなく{@link HttpServletResponse}を使用すること。
     *
     * @param response HTTPレスポンス
     * @param context 実行コンテキスト
     * @throws ServletException Servlet API使用時に発生した例外
     * @throws IOException 入出力例外(ソケットI/Oエラー等)
     */
    void writeResponse(HttpResponse response, ServletExecutionContext context) throws ServletException, IOException;
}
