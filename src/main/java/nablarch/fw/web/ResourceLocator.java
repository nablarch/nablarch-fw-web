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
 *   classpath指定は、フィルシステム上に存在しているファイルのみ指定できる。
 *   このため、jarなどでアーカイブされたファイルについてはclasspathを指定することは出来ない。
 *   また、バーチャルファイルシステムを用いてファイルを管理するようなWebアプリケーションサーバの場合、
 *   ファイルシステム上に存在しているファイルの場合でも、classpathの指定は出来ない。
 *   
 *   このため、classpathではなく静的ファイル(file://)の使用を推奨する。
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

    /** 対応するスキーム名 */
    public static final String SCHEMES = "file|classpath|forward|servlet|redirect|https|http";

    /** 許可スキームパターン */
    private static final Pattern ALLOWED_SCHEMES = Pattern.compile('^' + SCHEMES + '$');

    /** スキームを抽出するための正規表現 */
    private static final Pattern EXTRACT_SCHEME_PATTERN = Pattern.compile("^(?:(.+)://)");
    
    /** ホスト名を抽出するための正規表現 */
    private static final Pattern EXTRACT_HOSTNAME_PATTERN = Pattern.compile("^([^/?#]+)");

    /**
     * コンテンツパス中のディレクトリとして許容される文字列。
     * <p/>
     * <pre>
     * "/", "~" はNG。
     * 2以上連続する"."もNG。
     * </pre>
     */
    public static final Pattern ALLOWED_CHAR = Pattern.compile("^([^./~]|\\.(?=[^.]))*/$");

    /** コンテンツのパス */
    private final String contentPath;

    /** リソースパスのスキーム名 */
    private final String scheme;

    /** パス */
    private final String path;

    /** リソース名 */
    private final String resourceName;

    /** ホスト名 */
    private final String hostname;

    /** ディレクトリ */
    private final String directory;

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
    private ResourceLocator(final String path) {

        final Matcher matcher = EXTRACT_SCHEME_PATTERN.matcher(path);
        final String pathWithoutScheme;
        if (matcher.find()) {
            scheme = matcher.group(1);
            if (!ALLOWED_SCHEMES.matcher(scheme)
                               .matches()) {
                LOG.logInfo("malformed resource path. resource path = " + path);
                throw new HttpErrorResponse(400);
            }
            pathWithoutScheme = path.substring(matcher.end());
            contentPath = path;
        } else {
            pathWithoutScheme = path;
            scheme = DEFAULT_SCHEME;
            contentPath = DEFAULT_SCHEME + "://" + path;
        }

        final Resource resource;
        if (isHttpScheme()) {
            final Matcher hostnameMatcher = EXTRACT_HOSTNAME_PATTERN.matcher(pathWithoutScheme);
            if (hostnameMatcher.find()) {
                resource = new Resource(pathWithoutScheme.substring(hostnameMatcher.end()));
                hostname = hostnameMatcher.group(1);
            } else {
                resource = new Resource("");
                hostname = "";
            }
        } else {
            resource = new Resource(pathWithoutScheme);
            hostname = "";
        }
        this.path = resource.getDirectory() + resource.getResourceName();
        directory = resource.getDirectory();
        resourceName = resource.getResourceName();
    }

    /**
     * 自身のスキームがhttp(https)スキームかどうかを返す。
     * @return http(https)の場合は{@code true}
     */
    private boolean isHttpScheme() {
        return scheme.equals("https") || scheme.equals("http");
    }

    /**
     * このリソースパスのスキーム名を返す。
     *
     * @return スキーム名
     */
    public String getScheme() {
        return this.scheme;
    }

    /**
     * 設定されたパスが相対パスかどうか。
     *
     * @return 相対パス表記であれば{@code true}。
     *          コンテキストクラスローダ上のリソースである場合、常に{@code false}
     */
    public boolean isRelative() {
        if (scheme.equals("classpath") || isHttpScheme()) {
            return false;
        }
        return !path.startsWith("/");
    }

    /**
     * リソース名を返す。
     *
     * @return リソース名
     */
    public String getResourceName() {
        return resourceName;
    }

    /**
     * パス文字列を返す。
     * 
     * パスにクエリーパラメータやフラグメントがある場合、
     * これらを含んだ値をパスとして返す。
     *
     * @return パス文字列
     */
    public String getPath() {
        return path;
    }

    /**
     * {@inheritDoc}
     * リソースパスの文字列表現そのものを返す。
     */
    @Override
    public String toString() {
        return contentPath;
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

    /**
     * パスのホスト部を返す。
     * 
     * ポート番号が設定されている場合には、ポート番号を含んだ値を返す。
     * 
     * @return パスのホスト部
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * パスからディレクトリを表す部分を返す。
     * @return パスのディレクトリ部
     */
    public String getDirectory() {
        return directory;
    }

    private static class Resource {

        private final String directory;

        private final String resourceName;

        public Resource(final String path) {
            final int last = path.lastIndexOf('/');
            if (last != -1) {
                directory = path.substring(0, last + 1);
                resourceName = path.substring(last + 1);
            } else {
                directory = "";
                resourceName = path;
            }

            for (final String s : directory.split("/")) {
                if (!ALLOWED_CHAR.matcher(s + '/')
                                 .matches()) {
                    LOG.logInfo("malformed resource path. resource path = " + path);
                    throw new HttpErrorResponse(400);
                }
            }
        }

        public String getDirectory() {
            return directory;
        }

        public String getResourceName() {
            return resourceName;
        }
    }
}
