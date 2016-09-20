package nablarch.fw.web.upload;

import nablarch.core.log.Logger;
import nablarch.core.log.LoggerManager;
import nablarch.core.util.map.MapWrapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * パート情報{@link PartInfo}を一時的に集約し保持するMapクラス。<br/>
 * 同名のキーに複数のパートを保持することができる。
 *
 * @author T.Kawasaki
 */
public class PartInfoHolder extends MapWrapper<String, List<PartInfo>> {

    /** ロガー */
    private static final Logger LOGGER = LoggerManager.get(PartInfoHolder.class);

    /** 空のインスタンス */
    private static final PartInfoHolder EMPTY_INSTANCE
            = new PartInfoHolder(Collections.<String, List<PartInfo>>emptyMap());  // Collections.emptyMap()はunmodifiable

    /** パート情報を格納するMap */
    private final Map<String, List<PartInfo>> delegate;

    /** コンストラクタ。 */
    public PartInfoHolder() {
        this(new HashMap<String, List<PartInfo>>());
    }

    /**
     * コンストラクタ。
     *
     * @param baseMap パート情報を格納するMap
     */
    PartInfoHolder(Map<String, List<PartInfo>> baseMap) {
        delegate = baseMap;
    }

    /**
     * 空のインスタンス（パート情報を持たない）を取得する。
     *
     * @return 空のインスタンス
     */
    static PartInfoHolder getEmptyInstance() {
        return EMPTY_INSTANCE;
    }

    /**
     * パート情報を追加する。<br/>
     * キーには、そのパート情報が保持する名称を使用する。
     *
     * @param part パート情報
     */
    public void addPart(PartInfo part) {
        addPart(part.getName(), part);
    }

    /**
     * パート情報を追加する。
     *
     * @param name キー名
     * @param part パート情報
     */
    void addPart(String name, PartInfo part) {
        if (containsKey(name)) {
            get(name).add(part);
        } else {
            List<PartInfo> list = new ArrayList<PartInfo>();
            list.add(part);
            put(name, list);
        }
    }

    /** 格納したパートの一時ファイルを削除する。 */
    void cleanup() {
        for (List<PartInfo> list : values()) {
            for (PartInfo e : list) {
                e.clean();
            }
        }
    }

    /** 自身が保持する全てのパート情報をログ出力する。 */
    void logAllPart() {

        if (!LOGGER.isInfoEnabled()) {
            return;
        }
        List<String> logMessages = new ArrayList<String>();
        for (List<PartInfo> list : values()) {
            for (PartInfo part : list) {
                logMessages.add(part.toString());
            }
        }
        LOGGER.logInfo(logMessages.size() + " file(s) uploaded.");
        for (String msg : logMessages) {
            LOGGER.logInfo("\t" + msg);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, List<PartInfo>> getDelegateMap() {
        return delegate;
    }
}
