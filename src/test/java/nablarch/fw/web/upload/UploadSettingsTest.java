package nablarch.fw.web.upload;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * {@link UploadSettings}のテストクラス。<br/>
 *
 * @author T.Kawasaki
 */
public class UploadSettingsTest {

    /** テスト対象 */
    private UploadSettings target = new UploadSettings();



    /** Content-Length最大許容値の設定取得ができること。 */
    @Test
    public void testSetContentLengthLimit() {
        // 初期値は最大（制限なし）
        assertThat(target.getContentLengthLimit(), is(Integer.MAX_VALUE));

        // 設定した値が取得できること
        target.setContentLengthLimit(99);
        assertThat(target.getContentLengthLimit(), is(99));
    }

    /** 負数を設定したとき、例外が発生すること。 */
    @Test(expected = IllegalArgumentException.class)
    public void testSetContentLengthLimitFail() {
        target.setContentLengthLimit(-1);
    }

    /** 自動クリーニングの設定取得ができること。 */
    @Test
    public void testSetAndIsAutoCleaning() {
        // 初期値は真
        assertThat(target.isAutoCleaning(), is(true));
        // 偽を設定
        target.setAutoCleaning(false);
        assertThat(target.isAutoCleaning(), is(false));
        // 真を設定
        target.setAutoCleaning(true);
        assertThat(target.isAutoCleaning(), is(true));
    }
}
