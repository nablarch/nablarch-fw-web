package nablarch.fw.web.handler.csrf;

import static org.junit.Assert.*;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

/**
 * {@link UUIDv4CsrfTokenGenerator}のテストクラス。
 *
 */
public class UUIDv4CsrfTokenGeneratorTest {

    private UUIDv4CsrfTokenGenerator sut;

    @Before
    public void init() {
        sut = new UUIDv4CsrfTokenGenerator();
    }

    @Test
    public void testGenerateToken() {
        String csrfToken = sut.generateToken();
        assertNotNull(csrfToken);
        //UUIDを復元できることをテストする
        UUID.fromString(csrfToken);
    }
}
