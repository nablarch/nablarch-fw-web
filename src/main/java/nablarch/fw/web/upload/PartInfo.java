package nablarch.fw.web.upload;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import nablarch.core.log.Logger;
import nablarch.core.log.LoggerManager;
import nablarch.core.util.Builder;
import nablarch.core.util.FileUtil;
import nablarch.core.util.StringUtil;
import nablarch.core.util.annotation.Published;
import nablarch.fw.results.BadRequest;
import nablarch.fw.results.InternalError;
import static nablarch.core.util.Builder.concat;

/**
 * マルチパートの情報を保持するクラス。<br/>
 *
 * @author T.Kawasaki
 */
public final class PartInfo {

    /** ロガー */
    private static final Logger LOGGER = LoggerManager.get(PartInfo.class);

    /** 文字列が見つからなかったことを表す定数 */
    private static final int NOT_FOUND = -1;

    /** inputタグに付与されたname属性の値 */
    private String name;

    /** アップロードされたファイル名 */
    private String fileName;

    /** content-type */
    private String contentType = "application/octet-stream";

    /** 一時保存されたファイル */
    private File savedFile;

    /** ファイルサイズ */
    private int size = 0;

    /** 一時ファイルの状態 */
    private Status status = Status.NOT_YET_SAVED;

    /** コンストラクタ */
    private PartInfo() {
    }

    /**
     * 新しいインスタンスを取得する。</br>
     *
     * @param headers ヘッダ行
     * @return 新しいインスタンス
     */
    static PartInfo newInstance(List<String> headers) {
        PartInfo info = new PartInfo();
        for (String header : headers) {
            String lower = header.toLowerCase();
            if (lower.startsWith("content-disposition:")) {
                info.parseContentDisposition(header, lower);
            } else if (lower.startsWith("content-type:")) {
                info.setContentType(header);
            }
        }
        return info;
    }

    /**
     * 新しいインスタンスを取得する。<br/>
     * 自動テスト時に使用されることを想定している。
     *
     * @param name inputタグに付与されたname属性の値
     * @return 新しいインスタンス
     */
    @Published(tag = "architect")
    public static PartInfo newInstance(String name) {
        PartInfo info = new PartInfo();
        info.name = name;
        return info;
    }

    /**
     * アップロードファイルを開く。<br/>
     * 入力ストリームはクライアント側でcloseする必要がある。
     *
     * @return 入力ストリーム
     */
    @Published
    public InputStream getInputStream() {
        try {
            return new FileInputStream(savedFile);
        } catch (FileNotFoundException e) {
            // 通常発生しない。
            throw new InternalError(
                    "opening upload file failed. [" + toString() + "]", e);
        }
    }

    /**
     * Content-Dispositionの解析、設定を行う。
     *
     * @param orig  Content-Dispositionの行
     * @param lower 小文字化したContent-Dispositionの行
     */
    private void parseContentDisposition(String orig, String lower) {

        // Content-disposition
        int start = lower.indexOf("content-disposition: ");
        int end = lower.indexOf(";");

        if (end == NOT_FOUND) {
            // ;がない
            throw new BadRequest(concat(
                    "invalid Content-Disposition. ; not found. ",
                    "[", orig, "]"));
        }
        String disposition = lower.substring(start + 21, end);


        if (!"form-data".equals(disposition)) {
            // form-dataが無い
            throw new BadRequest(concat(
                    "invalid Content-Disposition. form-data expected. ",
                    "but was [", disposition, "]."));
        }

        // 名前
        start = lower.indexOf("name=\"", end); // name=" の長さは6
        end = lower.indexOf("\"", start + 7);
        int startOffset = 6;
        if (start == NOT_FOUND || end == NOT_FOUND) {
            // name属性がない
            start = lower.indexOf("name=", end);  // name=の長さは5
            end = lower.indexOf(";", start + 6);
            if (start == NOT_FOUND) {
                throw new BadRequest(concat(
                        "invalid content disposition. name not found. ",
                        "[", orig, "]"));
            } else if (end == NOT_FOUND) {
                end = orig.length();
            }
            startOffset = 5;
        }
        String name = orig.substring(start + startOffset, end);

        // ファイル名
        String fileName = null;
        start = lower.indexOf("filename=\"", end + 2);  // filename=" の長さは10
        end = lower.indexOf("\"", start + 10);
        if (start != NOT_FOUND && end != NOT_FOUND) {
            fileName = orig.substring(start + 10, end);
            int slash = Math.max(
                    fileName.lastIndexOf('/'),
                    fileName.lastIndexOf('\\'));
            if (slash > NOT_FOUND) {
                fileName = fileName.substring(slash + 1);
            }
        }

        this.name = name;
        this.fileName = fileName;
    }

    /**
     * Content-Typeを解析し、文字列を取得する。<br>
     *
     * @param line 行データ
     */
    private void setContentType(String line) {
        int end = line.indexOf(";");
        if (end == NOT_FOUND) {
            end = line.length();
        }
        this.contentType = line.substring(13, end).trim();
    }

    /**
     * このインスタンスが表すマルチパートがアップロードファイルであるかどうか判定する。
     *
     * @return アップロードファイルの場合は真
     */
    boolean isFile() {
        return StringUtil.hasValue(fileName);
    }

    /**
     * 名前を取得する。<br/>
     * 例えば、{@literal <input type="file" name="picture"/>}という
     * HTMLタグでアップロードされた場合、本メソッドの戻り値は"picture"となる。
     *
     * @return POSTされたときのname属性
     */
    public String getName() {
        return name;
    }

    /**
     * ファイル名を取得する。<br/>
     * 例えば、ユーザが"C:\doc\myPicture.jpg"というファイルをアップロードした場合、
     * 本メソッドの戻り値は"myPicture.jpg"となる。
     *
     * @return アップロード元のファイル名
     */
    @Published
    public String getFileName() {
        return fileName;
    }

    /**
     * Content-Typeを取得する。
     *
     * @return Content-Type
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * ファイル名を生成する。
     *
     * @return ファイル名
     */
    String generateFileName() {
        long current = System.currentTimeMillis();
        int sequence = generateSequence();
        String ext = extractFileSuffix(fileName);
        return Builder.concat(current, sequence, getRandomToken(), ext);
    }

    /**
     * 5byteのランダムデータのヘキサ表現(10文字)を返す。
     * @return ランダム文字列
     */
    private String getRandomToken() {
        int length = 5;
        byte[] random = new byte[length];
        char[] hex    = new char[length * 2];
        RANDOM_GEN.nextBytes(random);
        for (int i = 0; i < length; i++) {
            byte b = random[i];
            hex[2 * i]     = HEX_REPS[(b & 0xFF) >>> 4];
            hex[2 * i + 1] = HEX_REPS[(b & 0x0F)];
        }
        return new String(hex);
    }
    /** ヘキサ表現文字 */
    private static final char[] HEX_REPS = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        'a', 'b', 'c', 'd', 'e', 'f'
    };
    /** 乱数 */
    private static final Random RANDOM_GEN = new Random();

    /**
     * 出力ストリームを開く。<br/>
     * 出力ストリームはクライアント側でcloseする必要がある。
     *
     * @param saveDir 出力先ディレクトリ
     * @return 出力先ストリーム
     */
    OutputStream getOutputStream(File saveDir) {
        savedFile = createNewFile(saveDir);
        status = Status.SAVED;
        try {
            return new BufferedOutputStream(new FileOutputStream(savedFile));
        } catch (FileNotFoundException e) {
            // 保存先にwrite権が無い等の理由で出力ストリームが開けない（通常、発生しない）
            throw new IllegalStateException(
                    "unexpected exception occurred. file=[" + savedFile + "]"
                    , e);
        }
    }

    /**
     * 保存用に新しいファイルを作成する。
     *
     * @param saveDir 保存先ディレクトリ
     * @return 新しいファイル
     */
    private File createNewFile(File saveDir) {
        if (status != Status.NOT_YET_SAVED) {
            // 同じパートが二回保存されようとした場合（通常、発生しない）
            throw new IllegalStateException(
                    "already saved. [" + savedFile.getAbsolutePath() + "]");
        }
        String newFileName = generateFileName();
        return new File(saveDir, newFileName);
    }

    /** 保存したファイルを削除する。 */
    void clean() {
        if (status != Status.SAVED) {
            return;  // 保存してなければ何もしない。
        }
        if (!FileUtil.deleteFile(savedFile)) {
            LOGGER.logWarn(
                    "could not delete a temporary file: "
                            + savedFile.getAbsolutePath());
        }
    }

    /** ファイル保存に使用するデフォルト拡張子 */
    private static final String DEFAULT_FILE_SUFFIX = ".dat";

    /**
     * 拡張子を抽出する。
     *
     * @param fileName ファイル名
     * @return 拡張子（拡張子が無い場合はデフォルト値）
     */
    String extractFileSuffix(String fileName) {
        String suffix = FileUtil.extractSuffix(fileName);
        return (suffix.length() == 0)
                ? DEFAULT_FILE_SUFFIX
                : suffix;
    }


    /** ファイル名を作る時に使用するシーケンス番号。 */
    private static AtomicInteger seq = new AtomicInteger(0);

    /**
     * シーケンス番号を生成する。
     *
     * @return シーケンス番号
     */
    private static synchronized int generateSequence() {
        seq.compareAndSet(Integer.MAX_VALUE, 0);  // 最大値に達したら戻す
        return seq.getAndIncrement();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return concat("PartInfo {",
                      "name='", name, '\'',
                      ", fileName='", fileName, '\'',
                      ", contentType='", contentType, '\'',
                      ", size=", size,
                      ", savedFile=", savedFile,
                      '}');
    }

    /**
     * アップロードされたファイルのサイズを取得する（単位はバイト）。
     *
     * @return サイズ（バイト）
     */
    @Published
    public int size() {
        return size;
    }

    /**
     * アップロードされたファイルのサイズを設定する(単位はバイト)。
     *
     * @param size サイズ（バイト）
     */
    @Published(tag = "architect")
    public void setSize(int size) {
        if (size < 0) {
            throw new IllegalArgumentException(
                    "size must not be negative. [" + size + "]");
        }
        this.size = size;
    }

    /**
     * 保存ファイルを設定する。<br/>
     * 自動テスト時に使用されることを想定している。
     *
     * @param file ファイル
     */
    @Published(tag = "architect")
    public void setSavedFile(File file) {
        this.savedFile = file;
        fileName = file.getName();
        status = Status.SAVED;
    }

    /**
     * 一時保存ファイルを取得する。<br/>
     * 取得したファイルが存在しなかったり、削除される可能性もあるので
     * 使用する際は、nullチェック、ファイルの存在チェックなどの事前チェックを必ず行うこと。
     *
     * @return 一時保存されたファイル
     */
    @Published(tag = "architect")
    public File getSavedFile() {
        return this.savedFile;
    }

    /**
     * ファイルを移動する。<br/>
     * 本メソッドに対するヘルパーメソッドとして
     * {@link nablarch.fw.web.upload.util.UploadHelper#moveFileTo(String, String)}を利用することもできる。
     *
     * @param dir  移動先ディレクトリ
     * @param name 移動後のファイル名
     */
    @Published(tag = "architect")
    public void moveTo(File dir, String name) {
        switch (status) {
            case NOT_YET_SAVED:
                throw new IllegalStateException("upload file not saved yet.");
            case SAVED:
                File dest = new File(dir, name);
                FileUtil.move(savedFile, dest);
                status = Status.REMOVED;
                return;
            case REMOVED:
                throw new IllegalStateException("upload already removed.");
            default:
                throw new IllegalStateException("unexpected state.");  // never occurs
        }
    }


    /** 一時ファイルの状態を表す列挙型 */
    private enum Status {
        /** 未保存 */
        NOT_YET_SAVED,
        /** 保存済み */
        SAVED,
        /** 移動済み */
        REMOVED
    }
}
