package nablarch.common.web.handler.threadcontext;

import java.util.Locale;

import nablarch.core.util.annotation.Published;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.servlet.ServletExecutionContext;

/**
 * HTTP上で言語の保持を行うクラスの実装をサポートするクラス。
 * @author Kiyohito Itoh
 */
@Published(tag = "architect")
public abstract class LanguageAttributeInHttpSupport extends HttpLanguageAttribute {

    /**
     * スレッドコンテキストに保持する言語属性を返す。
     * <pre>
     * このクラスの実装では、以下の処理を行う。
     * 
     * 
     * 1. 保持している言語の取得を試みる。({@link #getKeepingLanguage(HttpRequest, ServletExecutionContext)})
     * 
     *   サポート対象の言語が取得できた場合は、取得できた言語を返す。
     *   
     *   サポート対象の言語が取得できない場合は2.に進む。
     *   
     * 2. Accept-Languageヘッダから言語の取得を試みる。(getAcceptLanguage(HttpRequest, ServletExecutionContext))
     * 
     *   サポート対象の言語が取得できた場合は、取得できた言語を返す。
     *   
     *   サポート対象の言語が取得できない場合は3.に進む。
     * 
     * 3.デフォルトの言語を返す。(nablarch.common.handler.threadcontext.LanguageAttribute#getValue(nablarch.fw.Request, nablarch.fw.ExecutionContext))
     * 
     * </pre>
     * @param req リクエスト
     * @param ctx 実行コンテキスト
     * @return サポート対象の言語
     */
    @Override
    protected Locale getLocale(HttpRequest req, ServletExecutionContext ctx) {
        
        String keepingLanguage = getKeepingLanguage(req, ctx);
        if (isSupportedLanguage(keepingLanguage)) {
            return new Locale(keepingLanguage);
        }
        
        return super.getLocale(req, ctx);
    }
    
    /**
     * ユーザが選択した言語を保持する。
     * @param req リクエスト
     * @param ctx 実行コンテキスト
     * @param language ユーザが選択した言語
     */
    protected abstract void keepLanguage(HttpRequest req, ServletExecutionContext ctx, String language);
    
    /**
     * 保持している言語を取得する。
     * @param req リクエスト
     * @param ctx 実行コンテキスト
     * @return 言語。保持していない場合はnull
     */
    protected abstract String getKeepingLanguage(HttpRequest req, ServletExecutionContext ctx);
}
