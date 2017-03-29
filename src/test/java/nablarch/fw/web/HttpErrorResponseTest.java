package nablarch.fw.web;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * {@link HttpErrorResponse}のテスト。
 */
public class HttpErrorResponseTest {

    /**
     * コンストラクタに指定した値が正しく設定されること。
     */
    @Test
    public void constructor() {

        final int statusCode = HttpResponse.Status.REQUEST_ENTITY_TOO_LARGE.getStatusCode();
        final String contentPath = "log.properties";
        final Throwable e = new RuntimeException("test");
        final HttpResponse response = new HttpResponse(HttpResponse.Status.CREATED.getStatusCode());

        HttpErrorResponse sut = new HttpErrorResponse();
        assertThat(sut.getResponse().getStatusCode(), is(HttpResponse.Status.BAD_REQUEST.getStatusCode()));

        sut = new HttpErrorResponse(e);
        assertThat(sut.getResponse().getStatusCode(), is(HttpResponse.Status.BAD_REQUEST.getStatusCode()));
        assertThat(sut.getCause(), is(e));

        sut = new HttpErrorResponse(contentPath);
        assertThat(sut.getResponse().getStatusCode(), is(HttpResponse.Status.BAD_REQUEST.getStatusCode()));
        assertThat(sut.getResponse().getContentPath().getPath(), is(contentPath));

        sut = new HttpErrorResponse(contentPath, e);
        assertThat(sut.getResponse().getStatusCode(), is(HttpResponse.Status.BAD_REQUEST.getStatusCode()));
        assertThat(sut.getResponse().getContentPath().getPath(), is(contentPath));
        assertThat(sut.getCause(), is(e));

        sut = new HttpErrorResponse(statusCode, e);
        assertThat(sut.getResponse().getStatusCode(), is(statusCode));
        assertThat(sut.getCause(), is(e));

        sut = new HttpErrorResponse(statusCode, contentPath);
        assertThat(sut.getResponse().getStatusCode(), is(statusCode));
        assertThat(sut.getResponse().getContentPath().getPath(), is(contentPath));

        sut = new HttpErrorResponse(statusCode, contentPath, e);
        assertThat(sut.getResponse().getStatusCode(), is(statusCode));
        assertThat(sut.getResponse().getContentPath().getPath(), is(contentPath));
        assertThat(sut.getCause(), is(e));

        sut = new HttpErrorResponse(response);
        assertThat(sut.getResponse(), is(response));

        sut = new HttpErrorResponse(response, e);
        assertThat(sut.getResponse(), is(response));
        assertThat(sut.getCause(), is(e));

        sut = new HttpErrorResponse().setResponse(response);
        assertThat(sut.getResponse(), is(response));
    }
}
