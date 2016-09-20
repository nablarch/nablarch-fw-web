package nablarch.fw.web;


/**
 * HTTPレスポンスの設定情報を格納するクラス。
 * <br>
 * 本クラスのプロパティを設定する場合はコンポーネント設定ファイルに本クラスの定義を行い、
 * 個別のプロパティ設定を行うこと。
 * 
 * @see ResponseBody
 * @author Iwauo Tajima <iwauo@tis.co.jp>
 */
public class HttpResponseSetting {
    /**
     * ボディバッファサイズの上限値を設定する。(単位:KB)
     * <p/>
     * ここで設定した上限値を越えてボディへの書き込みが行われた場合
     * バッファの内容を一時ファイルに出力する。
     * 以降の書き込みはこの一時ファイルに対して行われる。
     * デフォルトは 1024 (1MB)
     * 
     * @param size バッファサイズの上限値(KB)。
     * @return このオブジェクト自体
     */
    public HttpResponseSetting setBufferLimitSizeKb(Integer size) {
       bufferLimitSize = size;
       return this;
    }
    /**
     * ボディバッファサイズの上限値を返す。
     * @return バッファサイズ(KB)
     */
    public int getBufferLimitSizeKb() {
        return bufferLimitSize;
    }
    /** バッファサイズの上限値(KB)。 */
    private static int bufferLimitSize = 1024;
    
    /**
     * 一時ファイルの出力先フォルダのパスを設定する。
     * <pre>
     * nullを設定した場合はシステム既定の一時ファイルフォルダを使用する。
     * (Unix系OSであれば/tmp 以下)
     * デフォルトではnull。
     * </pre>
     * @param path 一時ディレクトリのパス
     * @return このオブジェクト自体 
     */
    public HttpResponseSetting setTempDirPath(String path) {
        tempDirPath = path;
        return this;
    }
    /**
     * 一時ファイルの出力先フォルダのパスを返す。
     * @return 一時ディレクトリのパス
     */
    public String getTempDirPath() {
        return tempDirPath;
    }
    /** 一時ファイルの出力先フォルダのパス */
    private static String tempDirPath = null;
}

