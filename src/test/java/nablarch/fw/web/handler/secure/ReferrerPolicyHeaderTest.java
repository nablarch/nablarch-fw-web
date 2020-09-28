package nablarch.fw.web.handler.secure;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Arrays;

/**
 * {@link ReferrerPolicyHeader}のテスト。
 */
public class ReferrerPolicyHeaderTest {

    @Test
    public void testDefaultSettings() {

        ReferrerPolicyHeader sut = new ReferrerPolicyHeader();

        assertThat(sut.isOutput(null, null), is(Boolean.TRUE));
        assertThat(sut.getName(), is("Referrer-Policy"));
        assertThat(sut.getValue(), is("strict-origin-when-cross-origin"));
    }

    @Test
    public void testCustomSettings() {

        for (String value : Arrays.asList(
                "no-referrer",
                "no-referrer-when-downgrade",
                "origin",
                "origin-when-cross-origin",
                "same-origin",
                "strict-origin",
                "strict-origin-when-cross-origin",
                "unsafe-url")) {

            ReferrerPolicyHeader sut = new ReferrerPolicyHeader();
            sut.setValue(value);

            assertThat(sut.isOutput(null, null), is(Boolean.TRUE));
            assertThat(sut.getName(), is("Referrer-Policy"));
            assertThat(sut.getValue(), is(value));
        }
    }

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testInvalidSettings() {

        expectedException.expect(IllegalArgumentException.class);
        expectedException.expectMessage("The value specified in the Referrer-Policy header is invalid. value = [invalid]");

        ReferrerPolicyHeader sut = new ReferrerPolicyHeader();
        sut.setValue("invalid");
    }
}
