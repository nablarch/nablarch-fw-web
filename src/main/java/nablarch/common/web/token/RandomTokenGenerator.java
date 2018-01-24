package nablarch.common.web.token;

import java.util.Random;

import nablarch.core.util.Base64Util;

/**
 * ランダムなトークンを生成するクラス。
 * @author Kiyohito Itoh
 */
public class RandomTokenGenerator implements TokenGenerator {

    /** ランダムな値を生成するオブジェクト */
    private static final Random RANDOM = new Random(System.nanoTime());
    
    /**
     * {@inheritDoc}
     * <per>
     * 16文字のランダムな文字列を生成する。
     * </per>
     */
    public String generate() {
        byte[] b = new byte[12];
        RANDOM.nextBytes(b);
        return Base64Util.encode(b);
    }
}
