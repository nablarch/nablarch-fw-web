package nablarch.common.web.csrf;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Test;

import nablarch.common.web.WebConfigFinder;
import nablarch.common.web.session.SessionEntry;
import nablarch.core.repository.ObjectLoader;
import nablarch.core.repository.SystemRepository;
import nablarch.core.repository.di.ComponentDefinitionLoader;
import nablarch.core.repository.di.DiContainer;
import nablarch.core.repository.di.config.DuplicateDefinitionPolicy;
import nablarch.core.repository.di.config.xml.XmlComponentDefinitionLoader;
import nablarch.fw.ExecutionContext;
import nablarch.fw.web.handler.CsrfTokenVerificationHandler;

/**
 * {@link CsrfTokenUtil}のテストクラス。
 *
 */
public class CsrfTokenUtilTest {

    @Test
    public void testGetCsrfToken() {
        String expected = "csrf token";
        ExecutionContext context = new ExecutionContext();
        String name = WebConfigFinder.getWebConfig().getCsrfTokenSessionStoredVarName();
        context.setSessionStoredVar(name, new SessionEntry(name, expected, null));
        String actual = CsrfTokenUtil.getCsrfToken(context);
        assertEquals(expected, actual);
    }

    @Test
    public void testGetHeaderName() {
        String expected = WebConfigFinder.getWebConfig().getCsrfTokenHeaderName();
        String actual = CsrfTokenUtil.getHeaderName();
        assertEquals(expected, actual);
    }

    @Test
    public void testGetParameterName() {
        String expected = WebConfigFinder.getWebConfig().getCsrfTokenParameterName();
        String actual = CsrfTokenUtil.getParameterName();
        assertEquals(expected, actual);
    }

    @Test
    public void testGetCsrfTokenWithCustomizedName() {
        loadCustomizedConfiguration();

        String expected = "csrf token";
        ExecutionContext context = new ExecutionContext();
        String name = "session-stored-var-name-customized";
        context.setSessionStoredVar(name, new SessionEntry(name, expected, null));
        String actual = CsrfTokenUtil.getCsrfToken(context);
        assertEquals(expected, actual);
    }

    @Test
    public void testGetHeaderNameCustomized() {
        loadCustomizedConfiguration();

        String expected = "X-CUSTOMIZED-CSRF-TOKEN";
        String actual = CsrfTokenUtil.getHeaderName();
        assertEquals(expected, actual);
    }

    @Test
    public void testGetParameterNameCustomized() {
        loadCustomizedConfiguration();

        String expected = "customized-csrf-token";
        String actual = CsrfTokenUtil.getParameterName();
        assertEquals(expected, actual);
    }

    @Test
    public void testRegenerateCsrfToken() {
        ExecutionContext context = new ExecutionContext();

        Object var1 = context
                .getRequestScopedVar(CsrfTokenVerificationHandler.REQUEST_REGENERATE_KEY);
        assertNull(var1);

        CsrfTokenUtil.regenerateCsrfToken(context);

        Boolean var2 = context
                .getRequestScopedVar(CsrfTokenVerificationHandler.REQUEST_REGENERATE_KEY);
        assertEquals(Boolean.TRUE, var2);
    }

    private static void loadCustomizedConfiguration() {
        ComponentDefinitionLoader loader = new XmlComponentDefinitionLoader(
                "classpath:nablarch/common/web/csrf/csrf-customized.xml",
                DuplicateDefinitionPolicy.OVERRIDE);
        ObjectLoader container = new DiContainer(loader);
        SystemRepository.load(container);
    }

    @After
    public void destroy() {
        SystemRepository.clear();
    }
}
