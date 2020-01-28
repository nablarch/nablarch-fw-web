package nablarch.common.web.token;

import org.junit.Test;

import java.util.regex.Pattern;

import static org.junit.Assert.assertTrue;

/**
 * {@link UUIDV4TokenGenerator}のテスト
 */
public class UUIDV4TokenGeneratorTest {
    private static final Pattern UUID_V4_PATTERN = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");

    /**
     * UUID(version4)のトークンが生成されることを確認する
     */
    @Test
    public void test() {
        final UUIDV4TokenGenerator sut = new UUIDV4TokenGenerator();
        final String generate = sut.generate();
        assertTrue(UUID_V4_PATTERN.matcher(generate).find());
    }
}