package nablarch.common.web.handler.threadcontext;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import nablarch.common.handler.threadcontext.LanguageAttribute;
import nablarch.core.util.StringUtil;
import nablarch.fw.ExecutionContext;
import nablarch.fw.Request;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.servlet.ServletExecutionContext;

/**
 * スレッドコンテキストに保持する言語属性をHTTPヘッダ(Accept-Language)から取得するクラス。
 * @author Kiyohito Itoh
 */
public class HttpLanguageAttribute extends LanguageAttribute {
    
    /** サポート対象の言語 */
    private Set<String> supportedLanguages;
    
    /**
     * サポート対象の言語を設定する。
     * @param supportedLanguages サポート対象の言語
     */
    public void setSupportedLanguages(String... supportedLanguages) {
        this.supportedLanguages = new HashSet<String>(Arrays.asList(supportedLanguages));
    }
    
    /**
     * コンテキストスレッドに格納するこのプロパティの値を返す。 
     * <p/>
     * {@link #getLocale(HttpRequest, ServletExecutionContext)}に処理を委譲する。
     * @param req リクエスト
     * @param ctx 実行コンテキスト
     * @return サポート対象の言語
     */
    public Object getValue(Request<?> req, ExecutionContext ctx) {
        return getLocale((HttpRequest) req, (ServletExecutionContext) ctx);
    }
    
    /**
     * スレッドコンテキストに保持する言語属性を返す。
     * <pre>
     * このクラスの実装では以下の処理を行う。
     *   
     * 1.Accept-Languageヘッダから言語の取得を試みる。
     *   ({@link #getAcceptLanguage(HttpRequest, ServletExecutionContext)})
     * 
     *   サポート対象の言語が取得できた場合は取得できた言語を返す。
     *   サポート対象の言語が取得できない場合は2.に進む。
     * 
     * 2.デフォルトの言語を返す。
     *   ({@link LanguageAttribute#getValue(Request, ExecutionContext)})
     * 
     * </pre>
     * @param req リクエスト
     * @param ctx 実行コンテキスト
     * @return サポート対象の言語
     */
    protected Locale getLocale(HttpRequest req, ServletExecutionContext ctx) {
        String acceptLanguage = getAcceptLanguage(req, ctx);
        if (acceptLanguage != null) {
            return new Locale(acceptLanguage);
        }
        return (Locale) super.getValue(req, ctx);
    }
    
    /** カンマを表す正規表現のパターン */
    private static final Pattern COMMA_PATTERN = Pattern.compile(",");
    
    /**
     * "Accept-Language"ヘッダをパースし、一番優先度が高いサポート対象の言語を返す。
     * @param req リクエスト
     * @param ctx 実行コンテキスト
     * @return サポート対象の言語。サポート対象の言語が取得できない場合はnull。
     */
    protected String getAcceptLanguage(HttpRequest req, ServletExecutionContext ctx) {
        
        String acceptLanguages = ctx.getServletRequest().getHeader("Accept-Language");
        if (StringUtil.isNullOrEmpty(acceptLanguages)) {
            // "Accept-Language"ヘッダが存在しない場合。
            return null;
        }
        
        // "Accept-Language"ヘッダをパースし、一番優先度が高い言語を探す。
        String language = null;
        double qualityValue = -1;
        for (String acceptLanguage : COMMA_PATTERN.split(acceptLanguages)) {
            
            String candidateLanguage;
            
            // レンジ指定に応じて言語タグを取得する。
            int languageRangeIndex = acceptLanguage.indexOf(";q=");
            if (languageRangeIndex == -1) {
                candidateLanguage = acceptLanguage;
            } else {
                candidateLanguage = acceptLanguage.substring(0, languageRangeIndex);
            }
            
            // 言語タグからISO-639(2桁の言語)以外を捨てる。
            candidateLanguage = candidateLanguage.trim();
            int subTagIndex = candidateLanguage.indexOf("-");
            if (subTagIndex != -1) {
                candidateLanguage = candidateLanguage.substring(0, subTagIndex); 
            }
            
            if (!isSupportedLanguage(candidateLanguage)) {
                // サポート対象外の言語は飛ばす。
                continue;
            }
            
            // レンジ指定に応じて品質値(優先度、最小値=0、最大値=1)を取得する。
            double candidateQualityValue;
            if (languageRangeIndex == -1) {
                // 品質値の規定は1。
                candidateQualityValue = 1.0;
            } else {
                try {
                    candidateQualityValue = Double.parseDouble(acceptLanguage.substring(languageRangeIndex + 3).trim());
                } catch (NumberFormatException e) {
                    // 品質値(重み)の指定が不正なため飛ばす。
                    // HTTPヘッダーの値は不正値が指定されてくる可能性があるため、
                    // 不正な値の場合でも障害とせずに読み飛ばす。
                    continue;
                }
            }
            
            if (candidateQualityValue == 1.0) {
                // 品質値の最大値は1のため決まり。
                return candidateLanguage;
            } else if (qualityValue < candidateQualityValue) {
                // 品質値が高い場合のみ入れ替え。
                // 品質値が等しい場合、記述順とするため入れ替えなし。
                language = candidateLanguage;
                qualityValue = candidateQualityValue;
            }
        }
        
        // 一番優先度が高いものを返す。
        return language != null ? language : null;
    }
    
    /**
     * サポート対象の言語か否かを判定する。
     * @param language 言語
     * @return サポート対象の言語の場合はtrue
     */
    protected boolean isSupportedLanguage(String language) {
        return supportedLanguages.contains(language);
    }
}
