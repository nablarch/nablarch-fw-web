package nablarch.fw.web.handler;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import nablarch.fw.web.HttpResponse;

/**
 * {@link ContentDispositionRawValue}のテスト。
 * 
 * @author Taichi Uragami
 *
 */
@RunWith(Parameterized.class)
public class ContentDispositionRawValueTest {

    @Parameter(0)
    public Fixture fixture;

    private ContentDispositionRawValue sut;

    @Before
    public void setUp() {
        sut = new ContentDispositionRawValue(fixture.rawValue);
    }

    @Test
    public void needsToBeEncoded() {
        assertThat(sut.needsToBeEncoded(), is(fixture.expectedNeedsToBeEncoded));
    }

    @Test
    public void getRawFileName() {
        Assume.assumeTrue(fixture.rawValue + " はエンコード不要", sut.needsToBeEncoded());
        assertThat(sut.getRawFileName(), is(fixture.expectedGetRawFileName));
    }

    @Test
    public void buildEncodedValue() {
        Assume.assumeTrue(fixture.rawValue + " はエンコード不要", sut.needsToBeEncoded());
        assertThat(sut.buildEncodedValue("エンコード済み.txt"), is(fixture.expectedBuildEncodedValue));
    }

    @Parameters(name = "{0}")
    public static List<Fixture> parameters() {

        String nothing = null;

        List<Fixture> fixtures = new ArrayList<Fixture>();

        fixtures.add(new Fixture("attachment; filename=\"ファイル名.txt\"",
                true, "ファイル名.txt", "attachment; filename=\"エンコード済み.txt\""));

        fixtures.add(new Fixture("inline; filename=\"ファイル名.txt\"",
                true, "ファイル名.txt", "inline; filename=\"エンコード済み.txt\""));

        fixtures.add(new Fixture("attachment; filename=hoge.txt",
                false, nothing, nothing));

        fixtures.add(new Fixture("attachment; filename=\"hoge.txt\"",
                true, "hoge.txt", "attachment; filename=\"エンコード済み.txt\""));

        fixtures.add(new Fixture("attachment",
                false, nothing, nothing));

        return fixtures;
    }

    private static class Fixture {

        /**
         * Content-Dispositionの値（ただしfilenameはエンコードしていない）。
         * 
         * <p>
         * {@link HttpResponse#setContentDisposition(String, boolean)}で設定される値を想定。
         * </p>
         */
        final String rawValue;

        /** {@link ContentDispositionRawValue#needsToBeEncoded()}の期待値 */
        final boolean expectedNeedsToBeEncoded;
        /** {@link ContentDispositionRawValue#getRawFileName()}の期待値 */
        final String expectedGetRawFileName;
        /** {@link ContentDispositionRawValue#buildEncodedValue(String)}の期待値 */
        final String expectedBuildEncodedValue;

        public Fixture(String rawValue, boolean expectedNeedsToBeEncoded,
                String expectedGetRawFileName, String expectedBuildEncodedValue) {
            this.rawValue = rawValue;
            this.expectedNeedsToBeEncoded = expectedNeedsToBeEncoded;
            this.expectedGetRawFileName = expectedGetRawFileName;
            this.expectedBuildEncodedValue = expectedBuildEncodedValue;
        }

        /**
         * テストケースの名前。
         */
        @Override
        public String toString() {
            return rawValue;
        }
    }
}
