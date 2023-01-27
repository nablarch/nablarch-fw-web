package nablarch.fw.web;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.http.Cookie;

import nablarch.TestUtil;
import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;

import mockit.Deencapsulation;
import mockit.Mocked;

/**
 * {@link HttpCookie}のテストクラス。
 */
public class HttpCookieTest {

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
     * HttpOnlyを取得できることを確認
     */
    @Test
    public void testIsHttpOnly(@Mocked final Cookie cookie) {
        sut = new HttpCookie();
        Deencapsulation.setField(sut, "httpOnly", true);
        assertThat("HttpOnlyを取得できること", sut.isHttpOnly(), is(true));
    }

    /**
     * HttpOnlyを設定できることを確認
     */
    @Test
    public void testSetHttpOnly(@Mocked final Cookie cookie) {
        sut = new HttpCookie();
        sut.setHttpOnly(false);
        assertThat("HttpOnlyを設定できること", (Boolean) Deencapsulation.getField(sut, "httpOnly"), is(false));
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
        assertThat("HttpOnlyが正しいこと", result.get(0).isHttpOnly(), is(false));
    }

    @Test
    public void testConvertServletCookies_allSetting() {

        sut = new HttpCookie();
        sut.setMaxAge(100);
        sut.setPath("test.com/index.html");
        sut.setDomain("test.com");
        sut.setSecure(true);
        sut.setHttpOnly(true);
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
                assertThat("HttpOnlyが正しいこと", c.isHttpOnly(), is(true));
            } else if ("key2".equals(c.getName())) {
                assertThat("クッキーの名称が正しいこと", c.getName(), is("key2"));
                assertThat("クッキーの値が正しいこと", c.getValue(), is("value2"));
                assertThat("Max-Ageが正しいこと", c.getMaxAge(), is(100));
                assertThat("Pathが正しいこと", c.getPath(), is("test.com/index.html"));
                assertThat("Domainが正しいこと", c.getDomain(), is("test.com"));
                assertThat("Secureが正しいこと", c.getSecure(), is(true));
                assertThat("HttpOnlyが正しいこと", c.isHttpOnly(), is(true));
            } else if ("key3".equals(c.getName())) {
                assertThat("クッキーの名称が正しいこと", c.getName(), is("key3"));
                assertThat("クッキーの値が正しいこと", c.getValue(), is("value3"));
                assertThat("Max-Ageが正しいこと", c.getMaxAge(), is(100));
                assertThat("Pathが正しいこと", c.getPath(), is("test.com/index.html"));
                assertThat("Domainが正しいこと", c.getDomain(), is("test.com"));
                assertThat("Secureが正しいこと", c.getSecure(), is(true));
                assertThat("HttpOnlyが正しいこと", c.isHttpOnly(), is(true));
            }
        }
    }
}
