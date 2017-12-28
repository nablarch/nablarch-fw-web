package nablarch.fw.web;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * {@link ResourceLocator#startsWithScheme(String)}のテスト
 */
@RunWith(Parameterized.class)
public class ResourceLocatorInternalHelperStartsWithSchemeTest {

    @Parameter(0)
    public Fixture fixture;

    @Test
    public void test() {
        boolean actual = ResourceLocatorInternalHelper.startsWithScheme(fixture.path);
        assertThat(actual, is(fixture.expected));
    }

    @Parameters(name = "{0}")
    public static List<Fixture> parameters() {
        List<Fixture> fixtures = new ArrayList<Fixture>();
        Fixture.addTo(fixtures, "絶対URI", "http://foo/bar", true);
        Fixture.addTo(fixtures, "ResourceLocatorが知らないスキーム", "URN:ISBN:978-4-7741-6931-6", true);
        Fixture.addTo(fixtures, "スキームを構成する文字はアルファベット、数字、+、-、.が許可される", "abcXYZ012+-.:foo/bar", true);

        Fixture.addTo(fixtures, "相対URI", "foo/bar", false);
        Fixture.addTo(fixtures, "最初のコロンの前にスキームが無い", ":foo:bar", false);
        Fixture.addTo(fixtures, "スキームに許可されない文字（/）を含んでいる", "foo/bar:baz", false);
        Fixture.addTo(fixtures, "スキームの最初の文字はアルファベットでなければならない", "12factor:app", false);
        Fixture.addTo(fixtures, "空文字列", "", false);
        return fixtures;
    }

    private static class Fixture {

        private final String description;
        private final String path;
        private final boolean expected;

        public Fixture(String description, String path, boolean expected) {
            this.description = description;
            this.path = path;
            this.expected = expected;
        }

        @Override
        public String toString() {
            return description;
        }

        static void addTo(List<Fixture> fixtures, String description, String path,
                boolean expected) {
            fixtures.add(new Fixture(description, path, expected));
        }
    }
}
