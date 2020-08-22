package nablarch.common.web.handler.threadcontext;

import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.MockHttpRequest;
import nablarch.fw.web.servlet.ServletExecutionContext;
import nablarch.test.support.web.servlet.MockServletContext;
import nablarch.test.support.web.servlet.MockServletRequest;
import nablarch.test.support.web.servlet.MockServletResponse;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * {@link LanguageAttributeInHttpUtil} のテスト。
 *
 * @author Koichi Asano 
 *
 */
public class LanguageAttributeInHttpUtilTest {
    @Before
    public void setUp() {
        SystemRepository.clear();
        XmlComponentDefinitionLoader loader = new XmlComponentDefinitionLoader("nablarch/common/web/handler/threadcontext/no_support.xml");
        DiContainer container = new DiContainer(loader);
        SystemRepository.load(container);
    }
    
    /**
     * サポート用コンポーネントが存在しなかった場合。
     */
    @Test
    public void testNoSupport() {
        HttpRequest req = new MockHttpRequest();
        MockServletRequest servletReq = new MockServletRequest();
        MockServletResponse servletRes = new MockServletResponse();
        MockServletContext servletCtx = new MockServletContext();
        ServletExecutionContext ctx = new ServletExecutionContext(servletReq, servletRes, servletCtx);
        try {
            LanguageAttributeInHttpUtil.keepLanguage(req, ctx, "ja");
            fail("例外が発生するはず。");
        } catch (RuntimeException e) {
            assertThat(e, instanceOf(RuntimeException.class));
            assertThat(e.getMessage(), is("component languageAttribute was not found."));
        }
    }
}
