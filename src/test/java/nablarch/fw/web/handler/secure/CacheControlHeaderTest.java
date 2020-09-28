package nablarch.fw.web.handler.secure;

import nablarch.fw.web.HttpResponse;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * {@link CacheControlHeader}のテスト。
 */
public class CacheControlHeaderTest {

    @Test
    public void testDefaultSettings() {

        CacheControlHeader sut = new CacheControlHeader();

        assertThat(sut.isOutput(new HttpResponse(), null), is(Boolean.TRUE));
        assertThat(sut.getName(), is("Cache-Control"));
        assertThat(sut.getValue(), is("no-store"));
    }

    @Test
    public void testCustomSettings() {

        CacheControlHeader sut = new CacheControlHeader();
        sut.setValue("test");

        assertThat(sut.isOutput(new HttpResponse(), null), is(Boolean.TRUE));
        assertThat(sut.getName(), is("Cache-Control"));
        assertThat(sut.getValue(), is("test"));
    }

    @Test
    public void testNoOutput() {

        CacheControlHeader sut = new CacheControlHeader();

        HttpResponse response = new HttpResponse();
        response.setHeader("Cache-Control", "no-store");
        assertThat(sut.isOutput(response, null), is(Boolean.FALSE));
    }
}
