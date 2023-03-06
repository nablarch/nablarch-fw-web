package nablarch.fw.web.upload;

import nablarch.core.util.FilePathSetting;

import java.io.File;
import java.net.URL;

/**
 * ファイルアップロードに関する各種設定値を保持するクラス。<br/>
 *
 * @author T.Kawasaki
 */
public class UploadSettings {

    /** アップロードファイル一時保存ディレクトリ */
    static final String UPLOAD_FILE_TMP_DIR = "uploadFileTmpDir";

    /** デフォルトの一時保存ディレクトリ */
    private static final File DEFAULT_SAVE_DIR = new File(System.getProperty("java.io.tmpdir"));

   /** 許容するContent-Lengthの最大値 */
    private int contentLengthLimit = Integer.MAX_VALUE;
    
    /** 許容するファイル数の最大値 */
    private int maxFileCount = -1;

    /** ファイルの自動クリーニングを行うかどうか */
    private boolean autoCleaning = true;


    /**
     * 保存ディレクトリを取得する。<br/>
     * 保存ディレクトリの物理パスは{@link FilePathSetting}から取得する。
     * {@link FilePathSetting}に指定されていない場合、
     * テンポラリディレクトリ("java.io.tmpdir")を使用する。
     *
     * @return 保存ディレクトリ
     */
    File getSaveDir() {
        FilePathSetting filePathSetting = FilePathSetting.getInstance();

        // 保存ディレクトリがコンポーネント設定ファイルで指定されているか
        URL saveDir
                = filePathSetting.getBasePathSettings().get(UPLOAD_FILE_TMP_DIR);
        return  (saveDir == null)
                ? DEFAULT_SAVE_DIR
                : new File(saveDir.getFile());
    }

    /**
     * Content-Length許容最大値を取得する。
     *
     * @return Content-Length許容最大値
     */
    public int getContentLengthLimit() {
        return this.contentLengthLimit;
    }

    /**
     * Content-Length許容最大値を設定する。
     *
     * @param contentLengthLimit Content-Length許容最大値
     */
    public void setContentLengthLimit(int contentLengthLimit) {
        if (contentLengthLimit < 0) {
            throw new IllegalArgumentException(
                    "contentLengthLimit must not be negative.");
        }
        this.contentLengthLimit = contentLengthLimit;
    }

    /**
     * アップロードファイル数の上限を取得する。
     * @return アップロードファイル数の上限
     */
    public int getMaxFileCount() {
        return maxFileCount;
    }

    /**
     * アップロードファイル数の上限を設定する。
     * <p>
     * 0以下の値を設定した場合は無制限となる。
     * デフォルトは-1。
     * </p>
     * @param maxFileCount アップロードファイル数の上限
     */
    public void setMaxFileCount(int maxFileCount) {
        this.maxFileCount = maxFileCount;
    }

    /**
     * 自動クリーニングを行うかどうか。
     *
     * @return 自動クリーニングする場合は、真（デフォルトは真）
     */
    public boolean isAutoCleaning() {
        return autoCleaning;
    }

    /**
     * 自動クリーニング要否を設定する。
     *
     * @param autoCleaning 自動クリーニング要否
     */
    public void setAutoCleaning(boolean autoCleaning) {
        this.autoCleaning = autoCleaning;
    }

}
