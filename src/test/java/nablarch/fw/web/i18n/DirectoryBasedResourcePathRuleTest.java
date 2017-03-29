package nablarch.fw.web.i18n;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * {@link DirectoryBasedResourcePathRule}のテスト。
 * @author Kiyohito Itoh
 */
public class DirectoryBasedResourcePathRuleTest {

    @Test
    public void testCreatePathForLanguage() {
        ResourcePathRule rule = new DirectoryBasedResourcePathRule();
        assertThat(rule.createPathForLanguage("/aaa/bbb/ccc.jsp", "ja"),
                   is("/ja/aaa/bbb/ccc.jsp"));
    }
}
