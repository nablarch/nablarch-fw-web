package nablarch.fw.web.useragent;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * {@link UserAgent}のテストクラス。
 *
 * @author T.Kawasaki
 */
public class UserAgentTest {

    private UserAgent target;

    /** コンストラクタのテスト。*/
    @Test
    public void testConstructor() {
        target = new UserAgent("UA Text");
        assertThat("コンストラクタで指定した文字列が取得できること",
                target.getText(), is("UA Text"));
        assertDefaultValue(target);
    }

    /** コンストラクタのテスト。*/
    @Test
    public void testConstructorNull() {
        target = new UserAgent((String) null);
        // nullが返却されないこと。
        assertThat(target.getText(), is(""));
        assertDefaultValue(target);
    }

    /** コピーコンストラクタとアクセサのテスト。*/
    @Test
    public void testCopyConstructor() {
        UserAgent orig = getFilled();
        target = new UserAgent(orig);
        // 値がコピーされていること
        assertThat(target.getOsType(), is("osType"));
        assertThat(target.getOsName(), is("osName"));
        assertThat(target.getOsVersion(), is("osVersion"));
        assertThat(target.getBrowserType(), is("browserType"));
        assertThat(target.getBrowserName(), is("browserName"));
        assertThat(target.getBrowserVersion(), is("browserVersion"));
    }

    private UserAgent getFilled() {
        UserAgent ua = new UserAgent("UA text");
        ua.setOsName("osName");
        ua.setOsType("osType");
        ua.setOsVersion("osVersion");
        ua.setBrowserName("browserName");
        ua.setBrowserType("browserType");
        ua.setBrowserVersion("browserVersion");
        return ua;
    }


    private void assertDefaultValue(UserAgent ua) {
        assertThat(ua.getOsType(), is("UnknownType"));
        assertThat(ua.getOsName(), is("UnknownName"));
        assertThat(ua.getOsVersion(), is("UnknownVersion"));
        assertThat(ua.getBrowserType(), is("UnknownType"));
        assertThat(ua.getBrowserName(), is("UnknownName"));
        assertThat(ua.getBrowserVersion(), is("UnknownVersion"));
    }

}