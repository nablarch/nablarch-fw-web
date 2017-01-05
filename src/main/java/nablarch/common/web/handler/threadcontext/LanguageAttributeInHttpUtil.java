package nablarch.common.web.handler.threadcontext;

import java.util.Locale;

import nablarch.core.ThreadContext;
import nablarch.core.repository.SystemRepository;
import nablarch.core.util.annotation.Published;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.servlet.ServletExecutionContext;

/**
 * HTTP上で選択された言語の保持を行う際に使用するユーティリティクラス。
 * <p/>
 * {@link nablarch.core.repository.SystemRepository}から"languageAttribute"という名前で取得した{@link LanguageAttributeInHttpSupport}のサブクラスに処理を委譲する。
 * このため、本クラスを使用する場合は、{@link nablarch.core.repository.SystemRepository}に{@link LanguageAttributeInHttpSupport}のサブクラスを登録すること。
 * 
 * @author Kiyohito Itoh
 */
@Published(tag = "architect")
public final class LanguageAttributeInHttpUtil {
    
    /** リポジトリから{@link LanguageAttributeInHttpSupport}を取得する際に使用するコンポーネント名 */
    private static final String LANGUAGE_ATTRIBUTE_COMPONENT_NAME = "languageAttribute";
    
    /** 隠蔽コンストラクタ。 */
    private LanguageAttributeInHttpUtil() {
    }
    
    /**
     * 指定された言語の保持と{@link ThreadContext}への設定を行う。
     * <p/>
     * 指定された言語がサポート対象外である場合は処理を行わない。
     * サポート対象言語とは、{@link HttpLanguageAttribute}で設定された言語である。
     * <p/>
     * 言語の保持については、アプリケーションで使用する{@link LanguageAttributeInHttpSupport}のサブクラスのJavadocを参照。
     * 
     * @param request リクエスト
     * @param context 実行コンテキスト
     * @param language 言語
     */
    public static void keepLanguage(HttpRequest request, ExecutionContext context, String language) {
        LanguageAttributeInHttpSupport support = SystemRepository.get(LANGUAGE_ATTRIBUTE_COMPONENT_NAME);
        if (support == null) {
            throw new RuntimeException("component " + LANGUAGE_ATTRIBUTE_COMPONENT_NAME + " was not found.");
        }
        if (!support.isSupportedLanguage(language)) {
            return;
        }
        support.keepLanguage(request, (ServletExecutionContext) context, language);
        ThreadContext.setLanguage(new Locale(language));
    }
}
