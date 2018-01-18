package nablarch.common.web.handler;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.Map;

/**
 * @author Kiyohito Itoh
 */
public final class WebTestUtil {
    
    public static final String CONTEXT_PATH = "/nablarch_test";
    
    /**
     *  MockServletResponse で encodeURL した際に付与するサフィックス。<br/>
     *  HttpServletResponse#encodeURL が呼ばれたかどうかの確認に使用する。
     */
    public static final String ENCODE_URL_SUFFIX = "_encode_suffix";
    
    private WebTestUtil() {
    }
    
    public static void assertParams(Map<String, List<String>> actualParams, Map<String, List<String>> expectedParams) {
        assertThat(actualParams.size(), is(expectedParams.size()));
        for (Map.Entry<String, List<String>> expectedParam : expectedParams.entrySet()) {
            List<String> actualValue = actualParams.get(expectedParam.getKey());
            List<String> expectedValue = expectedParam.getValue();
            assertThat(actualValue.size(), is(expectedValue.size()));
            for (int i = 0; i < expectedValue.size(); i++) {
                assertThat(actualValue.get(i), is(expectedValue.get(i)));
            }
        }
    }
}
