package nablarch.fw.web;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.Cookie;

import nablarch.TestUtil;
import org.hamcrest.Matchers;
import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import mockit.Deencapsulation;
import mockit.Mocked;
import org.junit.rules.ExpectedException;

/**
 * {@link HttpCookie}のテストクラス。
 */
public class HttpCookieTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    private HttpCookie sut;

    /**
     * Max-Ageを取得できることを確認
     * @throws Exception
     */
    @Test
    public void testGetMaxAge(@Mocked final Cookie cookie) throws Exception {
        sut = new HttpCookie();
        Deencapsulation.setField(sut, "maxAge", 100);
        assertThat("Max-Ageを取得できること", sut.getMaxAge(), is(100));
    }

    /**
     * Max-Ageを設定できることを確認
     * @throws Exception
     */
    @Test
    public void testSetMaxAge(@Mocked final Cookie cookie) throws Exception {
        sut = new HttpCookie();
        sut.setMaxAge(200);
        assertThat("Max-Ageを設定できること", (Integer) Deencapsulation.getField(sut, "maxAge") , is(200));

    }

    /**
     * Pathを取得できることを確認
     * @throws Exception
     */
    @Test
    public void testGetPath(@Mocked final Cookie cookie) throws Exception {
        sut = new HttpCookie();
        Deencapsulation.setField(sut, "path", "/test");
        assertThat("Pathを取得できること", sut.getPath(), is("/test"));
    }

    /**
     * Pathを設定できることを確認
     * @throws Exception
     */
    @Test
    public void testSetPath(@Mocked final Cookie cookie) throws Exception {
        sut = new HttpCookie();
        sut.setPath("/test");
        assertThat("Pathを設定できること", Deencapsulation.getField(sut, "path").toString(), is("/test"));
    }

    /**
     * Domainを取得できることを確認
     * @throws Exception
     */
    @Test
    public void testGetDomain(@Mocked final Cookie cookie) throws Exception {
        sut = new HttpCookie();
        Deencapsulation.setField(sut, "domain", "example.com");
        assertThat("Domainを取得できること", sut.getDomain(), is("example.com"));
    }

    /**
     * Domainを設定できることを確認
     * @throws Exception
     */
    @Test
    public void testSetDomain(@Mocked final Cookie cookie) throws Exception {
        sut = new HttpCookie();
        sut.setDomain("example.com");
        assertThat("Domainを設定できること", Deencapsulation.getField(sut, "domain").toString(), is("example.com"));
    }

    /**
     * Secureを取得できることを確認
     * @throws Exception
     */
    @Test
    public void testIsSecure(@Mocked final Cookie cookie) throws Exception {
        sut = new HttpCookie();
        Deencapsulation.setField(sut, "secure", true);
        assertThat("Secureを取得できること", sut.isSecure(), is(true));
    }

    /**
     * Secureを設定できることを確認
     * @throws Exception
     */
    @Test
    public void testSetSecure(@Mocked final Cookie cookie) throws Exception {
        sut = new HttpCookie();
        sut.setSecure(false);
        assertThat("Secureを設定できること", (Boolean) Deencapsulation.getField(sut, "secure"), is(false));
    }

    /**
     * ServletAPIのバージョンが3.0以前の場合に、HttpOnly取得時に例外が発生することを確認
     * @throws Exception
     */
    @Test
    public void testIsHttpOnly_error() throws Exception {
        Assume.assumeTrue(!TestUtil.isJetty9());

        sut = new HttpCookie();

        try {
            sut.isHttpOnly();
            fail("ServletAPIのバージョンが古いため、エラーが発生");
        } catch (UnsupportedOperationException e) {
            assertThat(e.getMessage(), is("ServletAPI in use is unsupported the HttpOnly attribute. " +
                    "Please update version of ServletAPI to 3.0 or more."));
        }

    }

    /**
     * ServletAPIのバージョンが3.0以前の場合に、HttpOnly設定時に例外が発生することを確認
     * @throws Exception
     */
    @Test
    public void testSetHttpOnly_error() throws Exception {
        Assume.assumeTrue(!TestUtil.isJetty9());

        sut = new HttpCookie();
        try {
            sut.setHttpOnly(false);
            fail("ServletAPIのバージョンが古いため、エラーが発生");
        } catch (UnsupportedOperationException e) {
            assertThat(e.getMessage(), is("ServletAPI in use is unsupported the HttpOnly attribute. " +
                    "Please update version of ServletAPI to 3.0 or more."));
        }
    }

    /**
     * HttpOnlyがサポートされているか否かの情報が取得できること。
     */
    @Test
    @Ignore("これはテスト出来ない…")
    public void testSupportsHttpOnly() throws Exception {
        sut = new HttpCookie();
        Deencapsulation.setField(sut, "SET_HTTP_ONLY_METHOD", null);
        assertThat("HttpOnlyはサポートされていない", sut.supportsHttpOnly(), is(false));

        // とりあえず適当なMethodオブジェクトを設定する。
        final Method method = this.getClass().getMethod("testSupportsHttpOnly");
        Deencapsulation.setField(sut, "SET_HTTP_ONLY_METHOD", method);

        assertThat("SET_HTTP_ONLY_METHODがnullではないので、httpOnlyがサポートされている",
                sut.supportsHttpOnly(), is(true));
        Deencapsulation.setField(sut, "SET_HTTP_ONLY_METHOD", null);
    }

    @Test
    public void testConvertServletCookies_default() {

        sut = new HttpCookie();
        sut.put("key1", "value1");

        List<Cookie> result = sut.convertServletCookies();

        assertThat("結果が空でないこと", result.isEmpty(), is(false));
        assertThat("クッキーの名称が正しいこと", result.get(0).getName(), is("key1"));
        assertThat("クッキーの値が正しいこと", result.get(0).getValue(), is("value1"));
        assertThat("Max-Ageが正しいこと", result.get(0).getMaxAge(), is(-1));
        assertThat("Pathが正しいこと", result.get(0).getPath(), is(nullValue()));
        assertThat("Domainが正しいこと", result.get(0).getDomain(), is(nullValue()));
        assertThat("Secureが正しいこと", result.get(0).getSecure(), is(false));
    }

    @Test
    public void testConvertServletCookies_allSetting() {

        sut = new HttpCookie();
        sut.setMaxAge(100);
        sut.setPath("test.com/index.html");
        sut.setDomain("test.com");
        sut.setSecure(true);
        sut.put("key1", "value1");
        sut.put("key2", "value2");
        sut.put("key3", "value3");

        List<Cookie> result = sut.convertServletCookies();

        assertThat("結果が空でないこと", result.isEmpty(), is(false));

        List<String> keys = new ArrayList<String>(){{
            add("key1");
            add("key2");
            add("key3");
        }};

        for (Cookie c : result) {
            if (keys.contains(c.getName())) {
                keys.remove(c.getName());
            } else {
                fail("想定しないキーが設定されている");
            }
            if ("key1".equals(c.getName())) {
                assertThat("クッキーの名称が正しいこと", c.getName(), is("key1"));
                assertThat("クッキーの値が正しいこと", c.getValue(), is("value1"));
                assertThat("Max-Ageが正しいこと", c.getMaxAge(), is(100));
                assertThat("Pathが正しいこと", c.getPath(), is("test.com/index.html"));
                assertThat("Domainが正しいこと", c.getDomain(), is("test.com"));
                assertThat("Secureが正しいこと", c.getSecure(), is(true));
            } else if ("key2".equals(c.getName())) {
                assertThat("クッキーの名称が正しいこと", c.getName(), is("key2"));
                assertThat("クッキーの値が正しいこと", c.getValue(), is("value2"));
                assertThat("Max-Ageが正しいこと", c.getMaxAge(), is(100));
                assertThat("Pathが正しいこと", c.getPath(), is("test.com/index.html"));
                assertThat("Domainが正しいこと", c.getDomain(), is("test.com"));
                assertThat("Secureが正しいこと", c.getSecure(), is(true));
            } else if ("key3".equals(c.getName())) {
                assertThat("クッキーの名称が正しいこと", c.getName(), is("key3"));
                assertThat("クッキーの値が正しいこと", c.getValue(), is("value3"));
                assertThat("Max-Ageが正しいこと", c.getMaxAge(), is(100));
                assertThat("Pathが正しいこと", c.getPath(), is("test.com/index.html"));
                assertThat("Domainが正しいこと", c.getDomain(), is("test.com"));
                assertThat("Secureが正しいこと", c.getSecure(), is(true));
            }
        }
    }

    @Test
    public void testParsingSetCookieHeader() {
        HttpCookie cookie = HttpCookie.fromSetCookieHeader("Set-Cookie: cookie=\"value\"");
        assertTrue(cookie.containsKey("cookie"));
        assertEquals("value", cookie.get("cookie"));
    }

    @Test
    public void testParsingSetCookieHeaderWithSpace() {
        HttpCookie cookie = HttpCookie.fromSetCookieHeader("Set-Cookie:  cookie = \"value\" ");
        assertTrue(cookie.containsKey("cookie"));
        assertEquals("value", cookie.get("cookie"));
    }

    @Test
    public void testParsingSetCookieHeaderMaxAge() {
        HttpCookie cookie = HttpCookie.fromSetCookieHeader("Set-Cookie: cookie=value; Max-Age=3600");
        assertTrue(cookie.containsKey("cookie"));
        assertEquals("value", cookie.get("cookie"));
        assertEquals(3600, (int)cookie.getMaxAge());
    }

    @Test
    public void testParsingSetCookieHeaderMaxAgeWithSpace() {
        HttpCookie cookie = HttpCookie.fromSetCookieHeader("Set-Cookie:  cookie = value ;  Max-Age = 3600 ");
        assertTrue(cookie.containsKey("cookie"));
        assertEquals("value", cookie.get("cookie"));
        assertEquals(3600, (int)cookie.getMaxAge());
    }

    @Test
    public void testParsingSetCookieHeaderPath() {
        HttpCookie cookie = HttpCookie.fromSetCookieHeader("Set-Cookie: cookie=value; Path=/");
        assertTrue(cookie.containsKey("cookie"));
        assertEquals("value", cookie.get("cookie"));
        assertEquals("/", cookie.getPath());
    }

    @Test
    public void testParsingSetCookieHeaderPathWithSpace() {
        HttpCookie cookie = HttpCookie.fromSetCookieHeader("Set-Cookie: cookie=value;  Path = / ");
        assertTrue(cookie.containsKey("cookie"));
        assertEquals("value", cookie.get("cookie"));
        assertEquals("/", cookie.getPath());
    }

    @Test
    public void testParsingSetCookieHeaderDomain() {
        HttpCookie cookie = HttpCookie.fromSetCookieHeader("Set-Cookie: cookie=value; Domain=example.com");
        assertTrue(cookie.containsKey("cookie"));
        assertEquals("value", cookie.get("cookie"));
        assertEquals("example.com", cookie.getDomain());
    }

    @Test
    public void testParsingSetCookieHeaderDomainWithSpace() {
        HttpCookie cookie = HttpCookie.fromSetCookieHeader("Set-Cookie: cookie=value;  Domain = example.com ");
        assertTrue(cookie.containsKey("cookie"));
        assertEquals("value", cookie.get("cookie"));
        assertEquals("example.com", cookie.getDomain());
    }

    @Test
    public void testParsingSetCookieHeaderSecure() {
        HttpCookie cookie = HttpCookie.fromSetCookieHeader("Set-Cookie: cookie=value; Secure");
        assertTrue(cookie.containsKey("cookie"));
        assertEquals("value", cookie.get("cookie"));
        assertTrue(cookie.isSecure());
    }

    @Test
    public void testParsingSetCookieHeaderSecureWithSpace() {
        HttpCookie cookie = HttpCookie.fromSetCookieHeader("Set-Cookie: cookie=value;  Secure ");
        assertTrue(cookie.containsKey("cookie"));
        assertEquals("value", cookie.get("cookie"));
        assertTrue(cookie.isSecure());
    }

    @Test
    public void testParsingSetCookieHeaderHttpOnly() {
        HttpCookie cookie = HttpCookie.fromSetCookieHeader("Set-Cookie: cookie=value; HttpOnly");
        assertTrue(cookie.containsKey("cookie"));
        assertEquals("value", cookie.get("cookie"));
        // 以下は実行できない
        // assertEquals(true, Cookie.class.getMethod("isHttpOnly").invoke(cookie));
    }

    @Test
    public void testParsingSetCookieHeaderHttpOnlyWithSpace() {
        HttpCookie cookie = HttpCookie.fromSetCookieHeader("Set-Cookie: cookie=value;  HttpOnly ");
        assertTrue(cookie.containsKey("cookie"));
        assertEquals("value", cookie.get("cookie"));
        // 以下は実行できない
        // assertEquals(true, Cookie.class.getMethod("isHttpOnly").invoke(cookie));
    }

    @Test
    public void testParsingSetCookieHeaderMaxAgeCaseInsensitive() {
        HttpCookie cookie = HttpCookie.fromSetCookieHeader("Set-Cookie: cookie=value; mAX-aGE=3600");
        assertTrue(cookie.containsKey("cookie"));
        assertEquals("value", cookie.get("cookie"));
        assertEquals(3600, (int)cookie.getMaxAge());
    }

    @Test
    public void testParsingSetCookieHeaderPathCaseInsensitive() {
        HttpCookie cookie = HttpCookie.fromSetCookieHeader("Set-Cookie: cookie=value; pATH=/");
        assertTrue(cookie.containsKey("cookie"));
        assertEquals("value", cookie.get("cookie"));
        assertEquals("/", cookie.getPath());
    }

    @Test
    public void testParsingSetCookieHeaderDomainCaseInsensitive() {
        HttpCookie cookie = HttpCookie.fromSetCookieHeader("Set-Cookie: cookie=value; dOMAIN=example.com");
        assertTrue(cookie.containsKey("cookie"));
        assertEquals("value", cookie.get("cookie"));
        assertEquals("example.com", cookie.getDomain());
    }

    @Test
    public void testParsingSetCookieHeaderSecureCaseInsensitive() {
        HttpCookie cookie = HttpCookie.fromSetCookieHeader("Set-Cookie: cookie=value; sECURE");
        assertTrue(cookie.containsKey("cookie"));
        assertEquals("value", cookie.get("cookie"));
        assertTrue(cookie.isSecure());
    }

    @Test
    public void testParsingSetCookieHeaderHttpOnlyCaseInsensitive() {
        HttpCookie cookie = HttpCookie.fromSetCookieHeader("Set-Cookie: cookie=value; hTTPoNLY");
        assertTrue(cookie.containsKey("cookie"));
        assertEquals("value", cookie.get("cookie"));
        // 以下は実行できない
        // assertEquals(true, Cookie.class.getMethod("isHttpOnly").invoke(cookie));
    }

    @Test
    public void testParsingSetCookieHeaderMaxAgeNull1() {
        expectedException.expect(Matchers.allOf(
                Matchers.instanceOf(IllegalArgumentException.class),
                Matchers.hasProperty("message", Matchers.is("Illegal cookie max-age attribute"))
        ));

        HttpCookie.fromSetCookieHeader("Set-Cookie: cookie=value; Max-Age=");
    }

    @Test
    public void testParsingSetCookieHeaderMaxAgeNull2() {
        expectedException.expect(Matchers.allOf(
                Matchers.instanceOf(IllegalArgumentException.class),
                Matchers.hasProperty("message", Matchers.is("Illegal cookie max-age attribute"))
        ));
        HttpCookie.fromSetCookieHeader("Set-Cookie: cookie=value; Max-Age");
    }

    @Test
    public void testParsingSetCookieHeaderPathNull1() {
        HttpCookie cookie = HttpCookie.fromSetCookieHeader("Set-Cookie: cookie=value; Path=");
        assertTrue(cookie.containsKey("cookie"));
        assertEquals("value", cookie.get("cookie"));
        assertEquals("", cookie.getPath());
    }

    @Test
    public void testParsingSetCookieHeaderPathNull2() {
        HttpCookie cookie = HttpCookie.fromSetCookieHeader("Set-Cookie: cookie=value; Path");
        assertTrue(cookie.containsKey("cookie"));
        assertEquals("value", cookie.get("cookie"));
        assertNull(cookie.getPath());
    }

    @Test
    public void testParsingSetCookieHeaderDomainNull1() {
        HttpCookie cookie = HttpCookie.fromSetCookieHeader("Set-Cookie: cookie=value; Domain=");
        assertTrue(cookie.containsKey("cookie"));
        assertEquals("value", cookie.get("cookie"));
        assertEquals("", cookie.getDomain());
    }

    @Test
    public void testParsingSetCookieHeaderDomainNull2() {
        HttpCookie cookie = HttpCookie.fromSetCookieHeader("Set-Cookie: cookie=value; Domain");
        assertTrue(cookie.containsKey("cookie"));
        assertEquals("value", cookie.get("cookie"));
        assertNull(cookie.getDomain());
    }

    @Test
    public void testParsingSetCookieHeaderAllAttributes() {
        HttpCookie cookie = HttpCookie.fromSetCookieHeader("Set-Cookie: cookie=value; Max-Age=3600; Path=/; Domain=example.com; Secure; HttpOnly");
        assertTrue(cookie.containsKey("cookie"));
        assertEquals("value", cookie.get("cookie"));
        assertEquals(3600, (int)cookie.getMaxAge());
        assertEquals("/", cookie.getPath());
        assertEquals("example.com", cookie.getDomain());
        assertTrue(cookie.isSecure());
        // 以下は実行できない
        // assertEquals(true, Cookie.class.getMethod("isHttpOnly").invoke(cookie));
    }

    @Test
    public void testThrowsErrorWhenParsingInvalidSetCookieMaxAge() {
        expectedException.expect(Matchers.instanceOf(IllegalArgumentException.class));

        HttpCookie.fromSetCookieHeader("Set-Cookie: cookie01=ok; Max-Age=hoge");
    }

    @Test
    public void testThrowsErrorWhenSetCookieStringIsNull() {
        expectedException.expect(Matchers.allOf(
            Matchers.instanceOf(IllegalArgumentException.class),
            Matchers.hasProperty("message", Matchers.is("Cookie string must not be null."))
        ));

        HttpCookie.fromSetCookieHeader(null);
    }

    @Test
    public void testThrowsErrorWhenSetCookieStringNotStartWithSetCookie() {
        expectedException.expect(Matchers.allOf(
            Matchers.instanceOf(IllegalArgumentException.class),
            Matchers.hasProperty("message", Matchers.is("Cookie string must start with 'Set-Cookie: '."))
        ));

        HttpCookie.fromSetCookieHeader("testName=testValue");

    }

    @Test
    public void testConvertingServletCookieToHttpCookie() {

        Cookie cookie = new Cookie("cookie", "value");
        cookie.setMaxAge(3600);
        cookie.setDomain("example.com");
        cookie.setPath("/");
        cookie.setSecure(true);

        HttpCookie httpCookie = HttpCookie.fromServletCookie(cookie);

        assertEquals("value", httpCookie.get("cookie"));
        assertEquals(3600, (int) httpCookie.getMaxAge());
        assertEquals("example.com", httpCookie.getDomain());
        assertEquals("/", httpCookie.getPath());
        assertTrue(httpCookie.isSecure());
    }

    @Test
    public void testThrowsErrorWhenEmptyServletCookie() {
        expectedException.expect(Matchers.allOf(
            Matchers.instanceOf(IllegalArgumentException.class),
            Matchers.hasProperty("message", Matchers.is("Cookie value must not be null."))
        ));

        Cookie cookie = new Cookie("test", null);
        HttpCookie.fromServletCookie(cookie);
    }
}
