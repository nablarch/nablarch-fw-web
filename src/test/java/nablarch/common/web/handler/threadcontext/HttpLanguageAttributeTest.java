package nablarch.common.web.handler.threadcontext;

import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.MockHttpRequest;
import nablarch.fw.web.servlet.ServletExecutionContext;
import nablarch.test.support.web.servlet.MockServletContext;
import nablarch.test.support.web.servlet.MockServletRequest;
import nablarch.test.support.web.servlet.MockServletResponse;
import org.junit.Test;

import java.util.Locale;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * {@link HttpLanguageAttribute}のテスト。
 * @author Kiyohito Itoh
 */
public class HttpLanguageAttributeTest {
    
    /**
     * Accept-Languageヘッダから言語を取得するテスト。
     */
    @Test
    public void testGetAcceptLanguage() {

        MockServletRequest servletReq = new MockServletRequest();
        MockServletResponse servletRes = new MockServletResponse();
        MockServletContext servletCtx = new MockServletContext();
        HttpRequest req = new MockHttpRequest();
        ServletExecutionContext ctx = new ServletExecutionContext(servletReq, servletRes, servletCtx);
        
        HttpLanguageAttribute attribute = new HttpLanguageAttribute();
        attribute.setDefaultLanguage("es"); // spanish
        attribute.setSupportedLanguages("en", "it", "ja", "es", "zh"); // Italian(it), Chinese(zh)
        
        Locale locale;
        
        /********************************************************************************
        Accept-Languageヘッダが送信されない場合
        ********************************************************************************/
        
        locale = (Locale) attribute.getValue(req, ctx);
        assertThat("デフォルトの言語が返される。", locale.getLanguage(), is("es"));
        
        /********************************************************************************
        サポート対象のAccept-Languageヘッダが送信される場合
        (品質値なしの言語が先頭に含まれる)
        ********************************************************************************/
        
        ctx = new ServletExecutionContext(servletReq, servletRes, servletCtx);
        ctx.getServletRequest().getHeaderMap().put("Accept-Language", "es, ja;q=0.8, en;q=0.7, it;q=0.7, zh;q=0.5");
        locale = (Locale) attribute.getValue(req, ctx);
        assertThat("品質値なしの言語が返される。", locale.getLanguage(), is("es"));
        
        /********************************************************************************
        サポート対象のAccept-Languageヘッダが送信される場合
        (品質値なしの言語が途中に含まれる)
        ********************************************************************************/
        
        ctx = new ServletExecutionContext(servletReq, servletRes, servletCtx);
        ctx.getServletRequest().getHeaderMap().put("Accept-Language", "es;q=0.7, ja;q=0.8, en, it;q=0.7, zh;q=0.5");
        locale = (Locale) attribute.getValue(req, ctx);
        assertThat("品質値なしの言語が返される。", locale.getLanguage(), is("en"));

        /********************************************************************************
        サポート対象のAccept-Languageヘッダが送信される場合
        (品質値なしの言語が最後に含まれる)
        ********************************************************************************/
        
        ctx = new ServletExecutionContext(servletReq, servletRes, servletCtx);
        ctx.getServletRequest().getHeaderMap().put("Accept-Language", "es;q=0.7, ja;q=0.8, en;q=0.7, it;q=0.7, zh");
        locale = (Locale) attribute.getValue(req, ctx);
        assertThat("品質値なしの言語が返される。", locale.getLanguage(), is("zh"));
        
        /********************************************************************************
        サポート対象のAccept-Languageヘッダが送信される場合
        (品質値なしの言語が含まれない、かつ品質値が一番高い言語が先頭に含まれる)
        ********************************************************************************/
        
        ctx = new ServletExecutionContext(servletReq, servletRes, servletCtx);
        ctx.getServletRequest().getHeaderMap().put("Accept-Language", "es;q=0.9, ja;q=0.8, en;q=0.7, it;q=0.7, zh;q=0.5");
        locale = (Locale) attribute.getValue(req, ctx);
        assertThat("品質値が一番高い言語が返される。", locale.getLanguage(), is("es"));

        /********************************************************************************
        サポート対象のAccept-Languageヘッダが送信される場合
        (品質値なしの言語が含まれない、かつ品質値が一番高い言語が途中に含まれる)
        ********************************************************************************/
        
        ctx = new ServletExecutionContext(servletReq, servletRes, servletCtx);
        ctx.getServletRequest().getHeaderMap().put("Accept-Language", "es;q=0.5, ja;q=0.9, en;q=0.7, it;q=0.7, zh;q=0.5");
        locale = (Locale) attribute.getValue(req, ctx);
        assertThat("品質値が一番高い言語が返される。", locale.getLanguage(), is("ja"));

        /********************************************************************************
        サポート対象のAccept-Languageヘッダが送信される場合
        (品質値なしの言語が含まれない、かつ品質値が一番高い言語が最後に含まれる)
        ********************************************************************************/
        
        ctx = new ServletExecutionContext(servletReq, servletRes, servletCtx);
        ctx.getServletRequest().getHeaderMap().put("Accept-Language", "es;q=0.5, ja;q=0.8, en;q=0.7, it;q=0.7, zh;q=0.9");
        locale = (Locale) attribute.getValue(req, ctx);
        assertThat("品質値が一番高い言語が返される。", locale.getLanguage(), is("zh"));

        /********************************************************************************
        サポート対象のAccept-Languageヘッダが送信される場合
        (品質値なしの言語が含まれない、かつ品質値が等しい複数の言語が先頭に含まれる)
        ********************************************************************************/
        
        ctx = new ServletExecutionContext(servletReq, servletRes, servletCtx);
        ctx.getServletRequest().getHeaderMap().put("Accept-Language", "es;q=0.9, ja;q=0.9, en;q=0.9, it;q=0.7, zh;q=0.5");
        locale = (Locale) attribute.getValue(req, ctx);
        assertThat("品質値が一番高い先頭の言語が返される。", locale.getLanguage(), is("es"));

        /********************************************************************************
        サポート対象のAccept-Languageヘッダが送信される場合
        (品質値なしの言語が含まれない、かつ品質値が等しい複数の言語が途中に含まれる)
        ********************************************************************************/
        
        ctx = new ServletExecutionContext(servletReq, servletRes, servletCtx);
        ctx.getServletRequest().getHeaderMap().put("Accept-Language", "es;q=0.5, ja;q=0.9, en;q=0.9, it;q=0.9, zh;q=0.5");
        locale = (Locale) attribute.getValue(req, ctx);
        assertThat("品質値が一番高い先頭の言語が返される。", locale.getLanguage(), is("ja"));

        /********************************************************************************
        サポート対象のAccept-Languageヘッダが送信される場合
        (品質値なしの言語が含まれない、かつ品質値が等しい複数の言語が最後に含まれる)
        ********************************************************************************/
        
        ctx = new ServletExecutionContext(servletReq, servletRes, servletCtx);
        ctx.getServletRequest().getHeaderMap().put("Accept-Language", "es;q=0.5, ja;q=0.8, en;q=0.9, it;q=0.9, zh;q=0.9");
        locale = (Locale) attribute.getValue(req, ctx);
        assertThat("品質値が一番高い先頭の言語が返される。", locale.getLanguage(), is("en"));
        
        /********************************************************************************
        サポート対象とサポート対象でないAccept-Languageヘッダが送信される場合
        ********************************************************************************/
        
        ctx = new ServletExecutionContext(servletReq, servletRes, servletCtx);
        ctx.getServletRequest().getHeaderMap().put("Accept-Language", "fr, ko;q=0.8, ja;q=0.1"); // Korean(ko)
        locale = (Locale) attribute.getValue(req, ctx);
        assertThat("サポート対象の言語が返される。", locale.getLanguage(), is("ja"));
        
        /********************************************************************************
        サブタグを含むAccept-Languageヘッダが送信される場合
        ********************************************************************************/
        
        ctx = new ServletExecutionContext(servletReq, servletRes, servletCtx);
        ctx.getServletRequest().getHeaderMap().put("Accept-Language", "i-cherokee, x-pig-latin, en-US, fr, ko;q=0.8");
        locale = (Locale) attribute.getValue(req, ctx);
        assertThat("サポート対象の言語が返される。", locale.getLanguage(), is("en"));
        
        /********************************************************************************
        サポート対象でないAccept-Languageヘッダが送信される場合
        ********************************************************************************/
        
        ctx = new ServletExecutionContext(servletReq, servletRes, servletCtx);
        ctx.getServletRequest().getHeaderMap().put("Accept-Language", "fr, ko;q=0.8");
        locale = (Locale) attribute.getValue(req, ctx);
        assertThat("デフォルトの言語が返される。", locale.getLanguage(), is("es"));

        /********************************************************************************
        不正な品質値を含むAccept-Languageヘッダが送信される場合
        ********************************************************************************/

        ctx = new ServletExecutionContext(servletReq, servletRes, servletCtx);
        ctx.getServletRequest().getHeaderMap().put("Accept-Language", "es;q=0.5, ja;q=0.8, en;q=0.9a, it;q=a0.9, zh;q=0.a9");
        locale = (Locale) attribute.getValue(req, ctx);
        assertThat("品質値が一番高い言語が返される。", locale.getLanguage(), is("ja"));
    }
}
