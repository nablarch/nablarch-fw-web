package nablarch.fw.web.upload;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import jakarta.servlet.ServletInputStream;

import nablarch.core.log.Logger;
import nablarch.core.log.LoggerManager;
import nablarch.core.util.StringUtil;
import nablarch.fw.results.BadRequest;
import nablarch.fw.results.RequestEntityTooLarge;

/**
 * マルチパート用の入力ストリーム。<br/>
 * 境界文字列を認識してストリームを読み込む機能を提供する。
 *
 * @author T.Kawasaki
 */
class MultipartInputStream {

    /** ログ */
    private static final Logger LOG = LoggerManager.get(MultipartInputStream.class);

    /** ストリーム終端に達したことを表す定数 */
    private static final int EOS = -1;

    /** 入力ストリーム */
    private final ServletInputStream in;

    /** コンテキスト */
    private final MultipartContext ctx;

    /** 読み込み許容最大値 */
    private final int limit;

    /** 現在までに読みこまれたバイト数 */
    private int readCount = 0;

    /** キャッシュしているバイト */
    private byte[] cache;

    /** キャッシュへ転送が終わったバイト数(ストリーム→キャッシュ用)。fill()でカウントアップ。 */
    private int count;

    /** 現在のキャッシュ位置(キャッシュ→バッファ用)。read()でカウントアップ。 */
    private int pos;

    /** 境界文字列が見つかった場合(該当Contentが終わりに達した時) true */
    private boolean eof;

    /** 境界文字列 */
    private String boundary;

    /**
     * コンストラクタ。
     *
     * @param in    　入力ストリーム
     * @param ctx   コンテキスト
     * @param limit 読み込み許容最大値
     */
    MultipartInputStream(ServletInputStream in, MultipartContext ctx, int limit) {
        this.in = in;
        this.ctx = ctx;
        this.limit = limit;
    }


    /**
     * ストリームから1行の読み込みを行う。<br/>
     * ストリームから識別子を取り出すときにのみ使用される。
     *
     * @return 読み込み行（読み込むデータがなかった場合はnull）
     * @throws IOException 入出力例外
     */
    String readLine() throws IOException {
        final int bufferSize = 4 * 1024;
        final int limitSize = 10 * 1024;

        ByteArrayOutputStream out = new ByteArrayOutputStream(bufferSize);
        byte[] buf = new byte[bufferSize];
        int readBytes;
        do {
            try {
                readBytes = in.readLine(buf, 0, buf.length);
            } catch (IOException e) {
                // クライアントから切断された場合の処理。
                LOG.logWarn("incomplete upload data.", e);
                throw new BadRequest("incomplete uploading", e);
            }
            if (readBytes != EOS) {
                addReadCount(readBytes);
                out.write(buf, 0, readBytes);
            }
        } while (readBytes == buf.length && out.size() <= limitSize);

        if (out.size() == 0) {
            return null;
        }

        if (out.size() > limitSize) {
            // Boundary内のヘッダ行のサイズが10KBを超えた場合は、リクエストが改竄されていると判断する。
            throw new BadRequest("header record size is too large. header record size = [" + out.size() + ']');
        }

        String line = new String(out.toByteArray(), ctx.getRequestCharacterEncoding());
        line = StringUtil.chomp(line, "\n");
        line = StringUtil.chomp(line, "\r");
        return line;
    }

    /**
     * 境界文字列まで入力ストリームをスキップする。
     *
     * @param boundary 境界文字列
     * @throws IOException 入出力例外
     */
    void skipTillBoundary(String boundary) throws IOException {
        String line;
        do {
            line = readLine();
            if (line == null) {
                throw new BadRequest("no data found.");
            }
        } while (!line.startsWith(boundary));
        this.boundary = boundary;
    }

    /**
     * pbuf.length読み込み終わった時もしくは、count - pos が2以下(
     * つまりキャッシュの最後まで読込み完了)時に本メソッドが終了する。
     *
     * @param pbuf バッファ
     * @return 読み込みバイト数
     * @throws IOException 入出力例外
     */
    int read(byte[] pbuf) throws IOException {
        // バッファの位置。
        int readBytesNum;

        int rest = countRestLength();
        // 読み込みが最初の場合は、下記条件式が成立する。
        // ※count == 0 & pos == 0だから。
        if (rest <= 0) {
            fill(); // ストリームからキャッシュにデータを溜め込む処理。
            rest = countRestLength();
            if (rest <= 0) {
                // キャッシュには何も読込まれなかった。
                return EOS;
            }
        }

        // restはキャッシュ内に残っている転送が済んでいない
        // データのサイズを表している。
        // restが一杯残っている場合は、pbuf.lengthが入る。
        int transferredBytesNum = Math.min(pbuf.length, rest);
        System.arraycopy(cache, pos, pbuf, 0, transferredBytesNum);
        pos += transferredBytesNum; // バッファへの転送が終わったキャッシュの位置を更新。
        readBytesNum = transferredBytesNum;

        // バッファが溜まるまで、残りを読み込む。
        // ※読み込みが途中の場合(キャッシュがバッファに入りきらなかった場合)、
        // 　下記はスキップされる。
        while (readBytesNum < pbuf.length) {
            fill(); // ストリームからキャッシュにデータを溜め込む。
            rest = countRestLength();
            if (rest <= 0) {
                return readBytesNum; // キャッシュには何も読込まれなかった。
            }
            transferredBytesNum = Math.min(pbuf.length - readBytesNum, rest);
            System.arraycopy(cache, pos, pbuf, readBytesNum, transferredBytesNum);
            pos += transferredBytesNum;
            readBytesNum += transferredBytesNum;
        }
        return readBytesNum;
    }

    /**
     * パラメータを読みとる。<br/>
     *
     * @return パラメータ文字列
     * @throws IOException 入出力例外
     */
    String readParam() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(512);
        byte[] buf256 = new byte[256];
        int readBytes;
        while ((readBytes = read(buf256)) != EOS) {
            out.write(buf256, 0, readBytes);
        }
        return out.toString(ctx.getRequestCharacterEncoding());
    }

    /**
     * 読み込まれるべき残りのバイト数を計算する。
     *
     * @return バイト数
     */
    private int countRestLength() {
        // 読み込まれるデータの最後に余分な2バイトのデータが存在する。
        // ので差し引いたバイト数が、残りの読込まなければいけないバイト数。
        return count - pos - 2;
    }

    /**
     * ストリームからboundary文字列までをキャッシュに格納する。<br/>
     * もしくは、キャッシュサイズまでストリームのデータを読込む。
     * 境界文字列に出会っている場合は、何も行わない。
     *
     * @throws IOException 入出力例外
     */
    private void fill() throws IOException {
        // boundaryに出会っていたらeofがtrueになっている。
        if (eof) {
            return;
        }
        // ストリームの続きの読み込みを行う場合は、下記条件式が成立する。
        // ※各パートのリード前にcount = 0を行なっているため、最初の読込みでは
        //   成立しない。
        // この時(count - pos == 2)が成立しない時は、キャッシュへのアップロードが
        // 終了していないことを示す。(ブラウザのストップボタン等)
        if (count > 0) {
            // 連結部分なので、最後の2バイトは切り捨てるのではなく、
            // 次のキャッシュの先頭につけている。
            if (count - pos == 2) {
                System.arraycopy(cache, pos, cache, 0, 2);
                count = 2;
                pos = 0;
            } else {
                throw new BadRequest("incomplete uploading.");
            }
        }
        int read = 0;
        int boundaryLength = boundary.length();
        int maxRead = cache.length - boundaryLength - 2; // 2は\r\n
        // ストリームからキャッシュへの転送を行う。
        while (count < maxRead) {
            try {
                read = in.readLine(cache, count, cache.length - count);
            } catch (IOException e) {
                // データ送信中にクライアントから切断(ブラウザの中止)された場合の処理
                // 発生したエラーをワーニングレベルでログ出力し、不正なリクエストデータであることを示すBadRequestを送出する。
                LOG.logWarn("incomplete upload data.", e);
                throw new BadRequest("incomplete uploading", e);
            }
            // boundary文字列が最後に来ないのに、ストリームが終わった場合は
            // 意図しないエラーである。
            if (read == EOS) {
                throw new BadRequest("input stream unexpectedly ended before boundary appears.");
            }
            addReadCount(read);
            // 読み込んだバッファがboundary文字列だったらeofを立てる。
            if (read >= boundaryLength) {
                eof = true;
                for (int i = 0; i < boundaryLength; i++) {
                    if (boundary.charAt(i) != cache[count + i]) {
                        eof = false;
                        // 直前のfor文から抜ける。
                        break;
                    }
                }
                if (eof) {
                    // while文から抜ける。
                    // キャッシュの最後がboundary文字列である状態。
                    // count +=が実行されないので、キャッシュ内に
                    // boundary文字列があるが、キャッシュからバッファへは
                    // 転送されない。
                    break;
                }
            }
            count += read;   // 読み込んだcountを増やす。
        }
    }


    /**
     * 読み込みバイト数を加算する。
     *
     * @param count 加算するバイト数。
     * @throws RequestEntityTooLarge Content-Length制限値を超過した場合
     */
    private void addReadCount(int count) throws RequestEntityTooLarge {
        readCount += count;
        if (readCount > limit) {
            consume();
            throw new RequestEntityTooLarge();
        }
    }

    /** 何もせずにrequestを消化する。 */
    void consume() {
        try {
            while (in.skip(1024 * 1024) > 0) { // SUPPRESS CHECKSTYLE 読み飛ばしのため
                // NOP
            }
        } catch (IOException ignored) { // SUPPRESS CHECKSTYLE クライアントから接続が切断された場合などに発生するため
            // 正常時には本処理は呼び出されないため、クライアントからの切断されIOExceptionが発生した場合でも何もしない。
        }
    }

    /**
     * 読み込み用の値を初期化する。<br/>
     * パートひとつ読み取る毎に初期化する必要がある。
     */
    void reset() {
        cache = new byte[64 * 1024];
        count = 0;
        pos = 0;
        eof = false;
    }
}
