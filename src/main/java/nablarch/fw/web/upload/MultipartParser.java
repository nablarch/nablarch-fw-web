package nablarch.fw.web.upload;


import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jakarta.servlet.ServletInputStream;

import nablarch.core.log.Logger;
import nablarch.core.log.LoggerManager;
import nablarch.core.util.FileUtil;
import nablarch.core.util.StringUtil;
import nablarch.fw.results.BadRequest;
import nablarch.fw.results.RequestEntityTooLarge;

/**
 * multipart/form-dataの解析クラス。<br/>
 * マルチパートの解析、アップロードファイルの一時保存を行う。
 *
 * @author T.Kawasaki
 */
class MultipartParser {

    /** コンテキスト情報 */
    private final MultipartContext ctx;

    /** 入力ストリーム */
    private final MultipartInputStream in;

    /** HTTPリクエストパラメータ */
    private final Map<String, String[]> paramMap;

    /** 一時ファイル保存ディレクトリ */
    private final File saveDir;

    /** ロガー */
    private static final Logger LOGGER = LoggerManager.get(MultipartParser.class);

    /**
     * コンストラクタ。
     *
     * @param in       入力ストリーム
     * @param paramMap HTTPリクエストパラメータ
     * @param settings 各種設定値
     * @param ctx      コンテキスト
     */
    MultipartParser(ServletInputStream in, Map<String, String[]> paramMap,
                    UploadSettings settings, MultipartContext ctx) {
        this.in = new MultipartInputStream(in, ctx, settings.getContentLengthLimit());
        this.ctx = ctx;
        this.paramMap = paramMap;
        this.saveDir = settings.getSaveDir();
    }

    /**
     * Content-Typeがmultipart/form-dataがどうかを識別する。<br/>
     *
     * @param contentType Content-Typeの値
     * @return multipart/form-dataである場合、真
     */
    static boolean isMultipart(String contentType) {
        return contentType != null
                && contentType.toLowerCase().startsWith("multipart/form-data");
    }


    /**
     * リクエストの解析処理を行う。<br/>
     *
     * @return 解析結果
     */
    PartInfoHolder parse() {
        if (ctx.getContentLength() == 0) {
            // Content-Lengthが0の場合は、空のマップを返却
            return PartInfoHolder.getEmptyInstance();
        }

        PartInfoHolder parts = new PartInfoHolder();
        try {
            parse(parts);
        } catch (BadRequest e) {
            // 例外発生時は保存したファイルを削除する
            try {
                parts.cleanup();
            } catch (Throwable t) {
                LOGGER.logWarn("failed to delete temp file.", t);
            }
            throw e;
        } catch (RequestEntityTooLarge e) {
            // 例外発生時は保存したファイルを削除する
            try {
                parts.cleanup();
            } catch (Throwable t) {
                LOGGER.logWarn("failed to delete temp file.", t);
            }
            throw e;
        } catch (Exception e) {   // IOException or RuntimeException
            // 例外発生時、ストリームを読み切って、それまで保存したファイルを全削除する。
            try {
                in.consume();
                parts.cleanup();
            } catch (Throwable t) {
                LOGGER.logWarn("failed to delete temp file.", t);
            }
            throw new nablarch.fw.results.InternalError(e);
        }
        return parts;
    }

    /**
     * リクエストの解析を行う。
     *
     * @param parts マルチパート情報を格納するクラス
     * @throws IOException 入出力例外
     */
    void parse(PartInfoHolder parts) throws IOException {

        // 境界文字列までスキップ
        String boundary = extractBoundary(ctx.getContentType());
        in.skipTillBoundary(boundary);

        // マルチパートを読み込み
        readPart(parts);
    }

    /**
     * 境界文字列を抽出する。
     *
     * @param contentType Content-Type
     * @return 境界文字列
     * @throws nablarch.fw.Result.BadRequest 境界文字列が見つからない場合
     */
    static String extractBoundary(String contentType) throws BadRequest {

        int index = contentType.lastIndexOf("boundary=");
        if (index == -1) {
            throw new BadRequest("can't find boundary in multipart.");
        }
        String boundary = contentType.substring(index + 9);
        if (boundary.startsWith("\"")) {
            boundary = boundary.replace('"', ' ').trim();
        }
        if (boundary.contains(";")) {
            final int pos = boundary.indexOf(';');
            boundary = boundary.substring(0, pos);
        }
        return "--" + boundary;
    }

    /**
     * マルチパートの読み込みを行う。
     *
     * @param result 読み込んだパートを格納するコンテナ
     * @throws IOException 入出力例外
     */
    private void readPart(PartInfoHolder result) throws IOException {
        String line = in.readLine();
        while (!StringUtil.isNullOrEmpty(line)) {
            // 1パートごとの処理
            in.reset();
            // パート内のヘッダ行を読みとる。
            List<String> headers = getHeaderLinesInPart(line);
            PartInfo part = PartInfo.newInstance(headers);
            String name = part.getName();
            String value;
            if (part.isFile()) {
                // ファイルアップロードのパート
                try {
                    write(part);             // ファイルに出力
                } finally {
                    // 例外が発生した場合でも、クリーニング用にマルチパート情報を追加する
                    result.addPart(part);
                }
                value = part.getFileName();
            } else {
                // ファイルアップロード以外のパート
                value = in.readParam();
            }
            // HTTPリクエストパラメータに追加
            addParam(name, value);
            line = in.readLine();
        }
    }

    /**
     * パート内のヘッダ行を取得する。<br/>
     *
     * @param line 最初の行
     * @return ヘッダ行
     * @throws IOException 入出力例外
     */
    private List<String> getHeaderLinesInPart(String line) throws IOException {
        List<String> headers = new ArrayList<String>();
        while (line != null && line.length() > 0) {
            String nextLine = null;
            StringBuilder sb = new StringBuilder(line);
            boolean continuous = true;  // 継続行であるか
            do {
                nextLine = in.readLine();
                if (nextLine != null && (nextLine.startsWith(" ")
                        || nextLine.startsWith("\t"))) {
                    sb.append(nextLine);
                } else {
                    continuous = false;
                }
            } while (continuous);
            headers.add(sb.toString());
            line = nextLine;
        }
        return headers;
    }

    /**
     * HTTPリクエストパラメータに値を追加する。
     *
     * @param name   名前
     * @param values 値
     */
    private void addParam(String name, String... values) {
        String[] newValue = values;
        if (paramMap.containsKey(name)) {
            String[] orig = paramMap.get(name);
            newValue = StringUtil.merge(orig, newValue);
        }
        paramMap.put(name, newValue);
    }


    /**
     * ファイルへの書き込みを行う。<br/>
     *
     * @param part 保存対象のパート
     * @throws IOException 入出力例外
     */
    private void write(PartInfo part) throws IOException {
        OutputStream out = null;
        try {
            out = part.getOutputStream(saveDir);
            int sum = 0;
            int read = 0;
            byte[] buf8k = new byte[8 * 1024];
            while ((read = in.read(buf8k)) != -1) {
                sum += read;
                out.write(buf8k, 0, read);
            }
            part.setSize(sum);
        } finally {
            FileUtil.closeQuietly(out);
        }
    }
}
