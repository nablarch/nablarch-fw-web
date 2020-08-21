package nablarch.fw.web.handler;

import nablarch.fw.ExecutionContext;
import nablarch.fw.web.HttpRequest;
import nablarch.fw.web.HttpResponse;
import nablarch.fw.web.handler.health.HealthCheckResponseBuilder;
import nablarch.fw.web.handler.health.HealthCheckResult;
import nablarch.fw.web.handler.health.HealthChecker;
import org.junit.Test;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class HealthCheckEndpointHandlerTest {

    @Test
    public void defaultSettings() {

        HealthCheckEndpointHandler sut = new HealthCheckEndpointHandler();
        HttpResponse response = sut.handle(null, null);

        assertThat(response.getStatusCode(), is(200));
        assertThat(response.getContentType(), is("application/json"));
        assertThat(response.getBodyString(), is("{\"status\":\"UP\"}"));
    }

    @Test
    public void targets() {

        HealthCheckEndpointHandler sut = new HealthCheckEndpointHandler();
        sut.setHealthCheckers(Arrays.asList(
                new HealthChecker() {
                    { setName("target1"); }
                    @Override
                    protected boolean tryOut(HttpRequest request, ExecutionContext context) {
                        return true;
                    }
                },
                new HealthChecker() {
                    { setName("target2"); }
                    @Override
                    protected boolean tryOut(HttpRequest request, ExecutionContext context) {
                        return false;
                    }
                }
        ));
        HttpResponse response = sut.handle(null, null);

        assertThat(response.getStatusCode(), is(503));
        assertThat(response.getContentType(), is("application/json"));
        assertThat(response.getBodyString(), is("{\"status\":\"DOWN\",\"targets\":[{\"name\":\"target1\",\"status\":\"UP\"},{\"name\":\"target2\",\"status\":\"DOWN\"}]}"));
    }

    @Test
    public void mergedHealthy() {

        HealthCheckEndpointHandler sut = new HealthCheckEndpointHandler();
        sut.setHealthCheckers(Arrays.asList(
                new HealthChecker() {
                    { setName("target1"); }
                    @Override
                    protected boolean tryOut(HttpRequest request, ExecutionContext context) {
                        return false;
                    }
                },
                new HealthChecker() {
                    { setName("target2"); }
                    @Override
                    protected boolean tryOut(HttpRequest request, ExecutionContext context) {
                        return true;
                    }
                }
        ));
        HttpResponse response = sut.handle(null, null);

        assertThat(response.getStatusCode(), is(503));
        assertThat(response.getContentType(), is("application/json"));
        assertThat(response.getBodyString(), is("{\"status\":\"DOWN\",\"targets\":[{\"name\":\"target1\",\"status\":\"DOWN\"},{\"name\":\"target2\",\"status\":\"UP\"}]}"));
    }

    @Test
    public void customHealthCheckResponseBuilder() {

        HealthCheckEndpointHandler sut = new HealthCheckEndpointHandler();
        sut.setHealthCheckResponseBuilder(new HealthCheckResponseBuilder() {
            @Override
            protected String buildResponseBody(HttpRequest request, ExecutionContext context, HealthCheckResult result) {
                return "{test}";
            }
        });
        HttpResponse response = sut.handle(null, null);

        assertThat(response.getStatusCode(), is(200));
        assertThat(response.getContentType(), is("application/json"));
        assertThat(response.getBodyString(), is("{test}"));
    }
}