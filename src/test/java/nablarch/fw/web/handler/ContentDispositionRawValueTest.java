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

    /**
     * {@link ContentDispositionRawValue#needsToBeEncoded()}のテスト。
     */
    @Test
    public void needsToBeEncoded() {
        assertThat(sut.needsToBeEncoded(), is(fixture.expectedNeedsToBeEncoded));
    }

    /**
     * {@link ContentDispositionRawValue#getRawFileName()}のテスト。
     * 
     * {@link ContentDispositionRawValue#needsToBeEncoded()}がfalseの場合はエンコード不要なのでテストをスキップする。
     */
    @Test
    public void getRawFileName() {
        Assume.assumeTrue(fixture.rawValue + " はエンコード不要", sut.needsToBeEncoded());
        assertThat(sut.getRawFileName(), is(fixture.expectedGetRawFileName));
    }

    /**
     * {@link ContentDispositionRawValue#buildEncodedValue(String)}のテスト。
     * 
     * {@link ContentDispositionRawValue#needsToBeEncoded()}がfalseの場合はエンコード不要なのでテストをスキップする。
     */
    @Test
    public void buildEncodedValue() {
        Assume.assumeTrue(fixture.rawValue + " はエンコード不要", sut.needsToBeEncoded());
        assertThat(sut.buildEncodedValue("ENCODEDFILENAME"), is(fixture.expectedBuildEncodedValue));
    }

    @Parameters(name = "{0}")
    public static List<Fixture> parameters() {

        List<Fixture> fixtures = new ArrayList<Fixture>();

        Fixture.testCase("attachment; filename=\"ファイル名.txt\"")
                .expect("ファイル名.txt",
                        "attachment; filename*=UTF-8''%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB%E5%90%8D.txt; filename=\"ENCODEDFILENAME\"")
                .addTo(fixtures, "filename*パラメーターが追加されてエンコードする");

        Fixture.testCase("inline; filename=\"ファイル名.txt\"")
                .expect("ファイル名.txt",
                        "inline; filename*=UTF-8''%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB%E5%90%8D.txt; filename=\"ENCODEDFILENAME\"")
                .addTo(fixtures, "disposition-typeがinlineの場合もエンコードする");

        Fixture.testCase("attachment; filename=hoge.txt")
                .expectNoEncoding()
                .addTo(fixtures, "ダブルクォートで囲まれていない場合はエンコードしない");

        Fixture.testCase("attachment; filename=\"hoge.txt\"")
                .expect("hoge.txt",
                        "attachment; filename*=UTF-8''hoge.txt; filename=\"ENCODEDFILENAME\"")
                .addTo(fixtures, "アルファベットとピリオドからなるファイル名でもダブルクォートで囲んだ場合はエンコードする");

        Fixture.testCase("attachment")
                .expectNoEncoding()
                .addTo(fixtures, "disposition-typeしかない場合はエンコードしない");

        //これ以降は念のためテスト

        Fixture.testCase("attachment; filename=\"ファイル名.txt\";ext1=foo  ;    ext2=bar")
                .expect("ファイル名.txt",
                        "attachment; filename*=UTF-8''%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB%E5%90%8D.txt; filename=\"ENCODEDFILENAME\"; ext1=foo; ext2=bar")
                .addTo(fixtures, "filename以外のパラメーターが設定されている場合");

        Fixture.testCase("attachment; filename=\"ファイル名.txt\"; filename*=UTF-8''ALREADY.txt")
                .expect("ファイル名.txt",
                        "attachment; filename=\"ENCODEDFILENAME\"; filename*=UTF-8''ALREADY.txt")
                .addTo(fixtures, "filename*パラメーターが既に設定されている場合は設定値を優先する");

        Fixture.testCase("attachment; filename*=UTF-8''ALREADY.txt")
                .expectNoEncoding()
                .addTo(fixtures, "filename*パラメーターしかない場合はエンコードしない");

        Fixture.testCase("attachment; filename=\"ファイル 名.txt\"")
                .expect("ファイル 名.txt",
                        "attachment; filename*=UTF-8''%E3%83%95%E3%82%A1%E3%82%A4%E3%83%AB%20%E5%90%8D.txt; filename=\"ENCODEDFILENAME\"")
                .addTo(fixtures, "ファイル名に半角空白が含まれる場合は、半角空白を含めてエンコードする");

        return fixtures;
    }

    private static class Fixture {

        private final String testCaseName;

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

        public Fixture(String testCaseName, String rawValue, boolean expectedNeedsToBeEncoded,
                String expectedGetRawFileName, String expectedBuildEncodedValue) {
            this.testCaseName = testCaseName;
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
            return testCaseName;
        }

        static Builder testCase(String rawValue) {
            Builder builder = new Builder();
            builder.rawValue = rawValue;
            return builder;
        }

        private static class Builder {
            String rawValue;
            boolean expectedNeedsToBeEncoded;
            String expectedGetRawFileName;
            String expectedBuildEncodedValue;

            Builder expectNoEncoding() {
                this.expectedNeedsToBeEncoded = false;
                this.expectedGetRawFileName = null;
                this.expectedBuildEncodedValue = null;
                return this;
            }

            Builder expect(String getRawFileName, String buildEncodedValue) {
                this.expectedNeedsToBeEncoded = true;
                this.expectedGetRawFileName = getRawFileName;
                this.expectedBuildEncodedValue = buildEncodedValue;
                return this;
            }

            void addTo(List<Fixture> fixtures, String testCaseName) {
                fixtures.add(new Fixture(testCaseName, rawValue, expectedNeedsToBeEncoded,
                        expectedGetRawFileName, expectedBuildEncodedValue));
            }
        }
    }
}
