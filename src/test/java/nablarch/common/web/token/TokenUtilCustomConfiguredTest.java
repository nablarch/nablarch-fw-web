package nablarch.common.web.token;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.UUID;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import nablarch.common.web.WebConfig;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.servlet.HttpRequestWrapper;
import nablarch.fw.web.servlet.NablarchHttpServletRequestWrapper;
import nablarch.fw.web.servlet.ServletExecutionContext;
import nablarch.test.support.SystemRepositoryResource;
import nablarch.test.support.web.MockHttpSession;
import nablarch.test.support.web.servlet.MockServletRequest;

/**
 * {@link WebConfig}をカスタマイズした場合の{@link TokenUtil}のテストクラス。
 */
public class TokenUtilCustomConfiguredTest {

    /**
     * {@link WebConfig}をカスタマイズするコンポーネント設定。
     */
    @Rule
    public SystemRepositoryResource systemRepositoryResource = new SystemRepositoryResource(
            "nablarch/common/web/token/token-util-custom-configured-test.xml");

    private MockServletRequest mockRequest;
    private MockHttpSession mockSession;
    private NablarchHttpServletRequestWrapper request;
    private HttpRequest httpRequest;
    private ServletExecutionContext context;

    @Before
    public void setUp() {
        mockRequest = new MockServletRequest();

        mockSession = new MockHttpSession();
        mockRequest.setSession(mockSession);

        request = new NablarchHttpServletRequestWrapper(mockRequest);

        httpRequest = new HttpRequestWrapper(request);

        context = new ServletExecutionContext(request, null, null);
    }

    /**
     * {@link TokenUtil#generateToken(NablarchHttpServletRequestWrapper)}のテスト。
     * 
     * 生成されたトークンはリクエストとセッションに格納されるが、
     * それぞれカスタマイズした名前で格納しているか確認する。
     * 
     * @see WebConfig#getDoubleSubmissionTokenRequestAttributeName()
     * @see WebConfig#getDoubleSubmissionTokenSessionAttributeName()
     */
    @Test
    public void generateToken() {
        assertThat(mockRequest.getAttribute("customizedRequestAttribute"), nullValue());
        assertThat(mockSession.getAttribute("customizedSessionAttribute"), nullValue());

        final String generatedToken = TokenUtil.generateToken(request);

        assertThat(mockRequest.getAttribute("customizedRequestAttribute").equals(generatedToken),
                is(true));
        assertThat(mockSession.getAttribute("customizedSessionAttribute").equals(generatedToken),
                is(true));
    }

    /**
     * {@link TokenUtil#isValidToken(HttpRequest, nablarch.fw.ExecutionContext)}のテスト。
     * 
     * リクエストパラメーターに含まれるトークンとセッションに格納されているトークンを比較するときに、
     * それぞれカスタマイズした名前で参照していることを確認する。
     * 
     * @see WebConfig#getDoubleSubmissionTokenSessionAttributeName()
     * @see WebConfig#getDoubleSubmissionTokenParameterName()
     */
    @Test
    public void isValidToken() {
        final String token = UUID.randomUUID().toString();

        mockSession.setAttribute("customizedSessionAttribute", token);

        mockRequest.getParams().put("customizedParameter", new String[] { token });

        final boolean result = TokenUtil.isValidToken(httpRequest, context);

        assertThat(result, is(true));
    }
}
