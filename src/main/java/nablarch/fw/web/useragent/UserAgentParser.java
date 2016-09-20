package nablarch.fw.web.useragent;

import nablarch.core.util.annotation.Published;

/**
 * UserAgentの解析を行うインタフェース。
 *
 * @author TIS
 */
@Published(tag = "architect")
public interface UserAgentParser {

    /**
     * UserAgentの解析を行う。
     *
     * @param userAgentText UserAgent文字列
     * @return UserAgentの解析結果
     */
    UserAgent parse(String userAgentText);
}
