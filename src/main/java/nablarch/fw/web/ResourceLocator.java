package nablarch.fw.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nablarch.core.log.Logger;
import nablarch.core.log.LoggerManager;
import nablarch.core.util.Builder;
import nablarch.core.util.annotation.Published;

/**
 * 各種リソースを識別する為の文字列（リソースロケータ）をパースして格納するクラス。
 * <p/>
 * <pre>
 * リソースロケータとは、本フレームワークにおいて、何らかのリソースを指定する際に用いられる汎用的書式である。
 * 以下のように定義される。
 *   (スキーム名)://(ディレクトリパス)/(リソース名)
 *
 * リソースロケータの使用場面例
 * - HTTPレスポンスの内容を格納したリソースを指定するケース
 *
 *   // 業務Actionから"jsp/success.jsp" に対してフォーワード
 *   new HttpResponse("200", "servlet://jsp/success.jsp");
 *
 * 現時点でサポートされるスキームは以下の5つである。
 *
 * 1. 静的ファイル
 *    ファイルシステム上の静的ファイルの内容を出力する。
 *    絶対パスもしくは相対パスで指定することが可能である。
 *     (書式)
 *         file://(コンテンツファイルへのパス)
 *     (例)
 *         file://./webapps/style/common.css    (相対パス)
 *         file:///www/docroot/style/common.css (絶対パス)
 *
 * 2. Javaコンテキストクラスローダ上のリソース
 *    コンテキストクラスローダ上のリソースの内容を出力する。
 *     (書式)
 *         classpath://(Javaリソース名)
 *     (例)
 *         classpath://nablarch/sample/webapp/common.css
 *
 * 3. 内部フォーワード
 *    リクエストプロセッサに対して、指定したリクエストURIでの再処理を要求する。
 *    HttpRequest・ExecutionContextはそのまま流用される。
 *     (書式)
 *         forward://(フォーワード名)
 *     (例)
 *         forward://registerForm.html           (現在のURIからの相対パス)
 *         forward:///app/user/registerForm.html (絶対パス)
 *
 * 4. サーブレットフォーワード
 *    サーブレットコンテナに対してフォーワードを行う。
 *    この場合、レスポンスの出力処理自体がフォーワード先のサーブレットに委譲される。
 *     (書式)
 *         servlet://(フォーワード名)
 *     (例)
 *         servlet://jsp/index.jsp   (現在のページからの相対パス)
 *         servlet:///jsp/index.jsp  (サーブレットコンテキストを起点とする相対パス)
 *
 *
 * 5. リダイレクト
 *    この場合は、指定されたパスへのリダイレクションを指示するレスポンスを行う。
 *     (書式)
 *         redirect://(リダイレクト先パス)
 *         http(s)://(リダイレクト先URL)
 *     (例)
 *         redirect://login             (現在のページからの相対パス)
 *         redirect:///UserAction/login (サーブレットコンテキストを起点とする相対パス)
 *         http://www.example.com/login (外部サイトのURL)
 *
 * このクラスは不変クラスである。
 * </pre>
 * @author Iwauo Tajima <iwauo@tis.co.jp>
 */
@Published
public final class ResourceLocator {

    /** ログ */
    private static final Logger LOG = LoggerManager.get(ResourceLocator.class);

    /** デフォルトスキーム */
    private static final String DEFAULT_SCHEME = "servlet";
    /**
     * リソースの文字列表現から{@code ResourceLocator}オブジェクトを生成する。
     * <p/>
     * {@value SCHEMES}に含まれないスキームを指定した場合、スキームは常に「servlet」となる。
     *
     * @param path リソースの文字列表現
     * @return 生成されたオブジェクト
     * @throws HttpErrorResponse リソースパスが無効な書式である場合
     */
    public static ResourceLocator valueOf(String path) {
        return new ResourceLocator(path);
    }

    /**
     * リソースの文字列表現からオブジェクトを生成する。
     *
     * @param path リソースの文字列表現
     */
    private ResourceLocator(String path) {
        Matcher m = SYNTAX.matcher(path);
        if (!m.matches()) {
            LOG.logInfo("malformed resource path. resource path = " + path);
            throw new HttpErrorResponse(400);
        }
        scheme       = (m.group(1) != null) ? m.group(1)
                     : (m.group(2) != null) ? m.group(2)
                     : DEFAULT_SCHEME;

        hostname     = (m.group(3) == null) ? ""
                                            : m.group(3);
        directory    = (m.group(4) == null) ? ""
                                            : m.group(4);
        resourceName = m.group(5);
    }

    /**
     * 渡された文字列が有効なリソースパスの書式であれば{@code true}を返す。
     *
     * @param path リソースパス文字列
     * @return 渡された文字列が有効なリソースパスの書式であれば{@code true}
     */
    public static boolean isValidPath(String path) {
        return SYNTAX.matcher(path).matches();
    }

    /** 対応するスキーム名 */
    public static final String SCHEMES = "file|classpath|forward|servlet|redirect";

    /**
     * コンテンツパス中のディレクトリ、リソース名として許容される文字列。
     * <p/>
     * <pre>
     * "/", "~" はNG。
     * 2以上連続する"."もNG。
     * </pre>
     */
    public static final String ALLOWED_CHAR = Builder.linesf(
      "(?:"
    , "  [^./~]"
    , "  |"
    , "  \\.(?=[^.])"
    , ")"
    );

    /** ホスト名 */
    private static final String
    HOSTNAME = "[-0-9a-zA-Z]{1,63}(?:\\.[-0-9a-zA-Z]{1,63}){0,10}(?:\\:\\d+)?(?:(?=[?/\\#])|$)";

    /** リソースパスの書式 */
    public static final Pattern SYNTAX = Pattern.compile(Builder.linesf(
      "^                       "
    , "(?:                     "
    , "  (%s)\\://             ", SCHEMES  // キャプチャ#1: スキーム名
    , "| (https?)\\://(%s)     ", HOSTNAME // キャプチャ#2 #3:
    , ")?                      "           //       HTTP(S) + ホスト名
    , "(                       "           // キャプチャ#4: ディレクトリ
    , "  (?:[A-Z]\\:)?         "           // Drive Letter(for Windows/DOS)
    , "  [\\\\/]?               "
    , "  (?:%s+[\\\\/])*?      ", ALLOWED_CHAR
    , ")?                      "
    , "(                       "  // キャプチャ#5: リソース名(including query parameters)
    , "  %s*                   ", ALLOWED_CHAR
    , "  (                     "  // キャプチャ#6: query parameters
    , "    \\?[^=]+=[^&]*      "
    , "    (?:\\&[^=]+=[^&]*)* "
    , "  )?                    "
    , ")?                      "
    , "$                       "
    ), Pattern.COMMENTS);

    /**
     * このリソースパスのスキーム名を返す。
     *
     * @return スキーム名
     */
    public String getScheme() {
        return this.scheme;
    }

    /** リソースパスのスキーム名 */
    private final String scheme;

    /**
     * リソースパスのディレクトリ部分に相当する文字列を返す。
     *
     * @return リソースパスのディレクトリ部分
     */
    public String getDirectory() {
        return this.directory;
    }

    /** リソースパスのディレクトリ部分 */
    private final String directory;

    /**
     * 設定されたパスが相対パスかどうか。
     *
     * @return 相対パス表記であれば{@code true}。
     *          コンテキストクラスローダ上のリソースである場合、常に{@code false}
     */
    public boolean isRelative() {
        if (scheme.equals("classpath")) {
            return false;
        }
        return !directory.startsWith("/");
    }

    /**
     * リソース名を返す。
     *
     * @return リソース名
     */
    public String getResourceName() {
        return resourceName;
    }

    /** リソース名 */
    private final String resourceName;

    /**
     * ホスト名を返す。
     *
     * @return ホスト名
     */
    public String getHostname() {
        return hostname;
    }

    /** ホスト名 */
    private final String hostname;



    /**
     * パス文字列を返す。
     *
     * @return パス文字列
     */
    public String getPath() {
        return directory + resourceName;
    }

    /**
     * {@inheritDoc}
     * リソースパスの文字列表現そのものを返す。
     */
    @Override
    public String toString() {
        return scheme + "://" + hostname + directory + resourceName;
    }

    /**
     * リソースのファイルシステム上での絶対パスを返す。
     *
     * @return 絶対パスを返す。絶対パスが取得できない場合{@code null}を返す
     * @throws UnsupportedOperationException 静的ファイルでもクラスローダ上のリソースでもない場合
     */
    public String getRealPath() throws UnsupportedOperationException {
        if (scheme.equals("file")) {
            return new File(getPath()).getAbsolutePath();
        }
        if (scheme.equals("classpath")) {
            URL resourceUrl = Thread.currentThread()
                              .getContextClassLoader()
                              .getResource(getPath().replaceFirst("^/", ""));
            return (resourceUrl != null) ? resourceUrl.toExternalForm()
                                                      .replaceFirst("file:", "")
                                         : null;
        }
        throw new UnsupportedOperationException();
    }


    /**
     * このリソースの実体が存在するかどうか判定する。
     *
     * @return このリソースの実体が存在する場合は{@code true}。
     *          内部フォーワード/サーブレットフォーワードの場合は常に{@code true}。
     *          リダイレクトである場合は常に{@code false}
     */
    public boolean exists() {
        if (scheme.equals("servlet") || scheme.equals("forward")) {
            return true;
        }
        if (scheme.equals("redirect") || scheme.equals("http") || scheme.equals("https")) {
            return false;
        }
        String path = getRealPath();
        if (path == null) {
            return false;
        }
        File file = new File(path);
        if (!file.exists() || !file.isFile()) {
            return false;
        }
        return true;
    }

    /**
     * リソースの内容を読み出すための{@link Reader}を返す。
     *
     * @return リソースの内容を読み出すためのReader
     * @throws FileNotFoundException 静的ファイルでもクラスローダ上のリソースでもない場合か、
     *                                静的ファイルかクラスローダ上のリソースだが絶対パスを取得できなかった場合
     */
    public Reader getReader() throws FileNotFoundException {
        if (scheme.equals("file") || scheme.equals("classpath")) {
            String path = getRealPath();
            if (path == null) {
                throw new FileNotFoundException(this.toString());
            }
            return new FileReader(path);
        }
        throw new FileNotFoundException(this.toString());
    }

    /**
     * リソースの内容をストリームで読み出すための{@link InputStream}を返す。
     *
     * @return リソースの内容を読み出すためのInputStream
     * @throws FileNotFoundException 静的ファイルでもクラスローダ上のリソースでもない場合か、
     *                                静的ファイルかクラスローダ上のリソースだが絶対パスを取得できなかった場合
     */
    public InputStream getInputStream() throws FileNotFoundException {
        if (scheme.equals("file") || scheme.equals("classpath")) {
            String path = getRealPath();
            if (path == null) {
                throw new FileNotFoundException(this.toString());
            }
            return new FileInputStream(path);
        }
        throw new FileNotFoundException(this.toString());
    }

    /**
     * レスポンスがリダイレクトかどうか判定する。
     *
     * @return レスポンスがリダイレクトであれば{@code true}
     */
    public boolean isRedirect() {
        return scheme.matches("redirect|https?");
    }
}
