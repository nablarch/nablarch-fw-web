package nablarch.fw.web.i18n;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * {@link FilenameBasedResourcePathRule}のテスト。
 * @author Kiyohito Itoh
 */
public class FilenameBasedResourcePathRuleTest {

    @Test
    public void testCreatePathForLanguage() {
        ResourcePathRule rule = new FilenameBasedResourcePathRule();
        assertThat(rule.createPathForLanguage("/aaa/bbb/ccc.jsp", "ja"),
                   is("/aaa/bbb/ccc_ja.jsp"));
    }
}