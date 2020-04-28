package nablarch.fw.web.handler.csrf;

import java.util.UUID;

import nablarch.core.util.annotation.Published;

/**
 * バージョン4のUUIDを使用してCSRFトークンの生成を行うクラス。
 * 
 * @author Uragami Taichi
 *
 */
@Published(tag = "architect")
public class UUIDv4CsrfTokenGenerator implements CsrfTokenGenerator {

    @Override
    public String generateToken() {
        return UUID.randomUUID().toString();
    }
}
