package nablarch.common.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nablarch.common.web.hiddenencryption.TamperingDetectedException;

/**
 * パラメータマップとパラメータ文字列の変換を行うクラス。<br>
 * パラメータ文字列の作成時は、コンストラクタで指定されたセパレータを使用し、
 * パラメータに含まれるセパレータはエスケープ処理する。
 * @author Kiyohito Itoh
 */
public class ParamsConvertor {
    
    /** パラメータセパレータ */
    private final char paramSeparator;
    
    /** name/valueセパレータ */
    private final char nameValueSeparator;
    
    /** セパレータのエスケープ文字 */
    private final char escapeChar;
    
    /**
     * コンストラクタ。
     * @param paramSeparator パラメータセパレータ
     * @param nameValueSeparator name/valueセパレータ
     * @param escapeChar セパレータのエスケープ文字
     */
    public ParamsConvertor(char paramSeparator, char nameValueSeparator, char escapeChar) {
        this.paramSeparator = paramSeparator;
        this.nameValueSeparator = nameValueSeparator;
        this.escapeChar = escapeChar;
    }
    
    /**
     * パラメータマップからパラメータ文字列に変換する。<br>
     * パラメータ文字列は、コンストラクタで指定されたセパレータを使用して作成する。
     * @param params パラメータマップ
     * @return パラメータ文字列
     */
    public String convert(Map<String, List<String>> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<String>> param : params.entrySet()) {
            for (String value : param.getValue()) {
                if (sb.length() != 0) {
                    sb.append(paramSeparator);
                }
                escape(sb, param.getKey());
                sb.append(nameValueSeparator);
                escape(sb, value);
            }
        }
        return sb.toString();
    }
    
    /**
     * セパレータをエスケープした文字列を文字列バッファに追加する。
     * @param sb 文字列バッファ
     * @param target ターゲット
     */
    private void escape(StringBuilder sb, String target) {
        char[] str = target.toCharArray();
        for (char c : str) {
            if (c == escapeChar) {
                sb.append(escapeChar).append(escapeChar);
            } else if (c == paramSeparator) {
                sb.append(escapeChar).append(paramSeparator);
            } else if (c == nameValueSeparator) {
                sb.append(escapeChar).append(nameValueSeparator);
            } else {
                sb.append(c);
            }
        }
    }
    
    /**
     * パラメータ文字列からパラメータマップに変換する。<br>
     * パラメータ文字列は、コンストラクタで指定されたセパレータを使用して作成されていることを想定する。
     * @param params パラメータ文字列
     * @return パラメータマップ
     */
    public Map<String, List<String>> convert(String params) {
        Map<String, List<String>> paramsMap = new HashMap<String, List<String>>();
        StringBuilder param = new StringBuilder();
        char[] paramStr = params.toCharArray();
        try {
            for (int i = 0; i < paramStr.length; i++) {
                if (isParamSeparator(paramStr, i)) {
                    unescapeNameValue(paramsMap, param);
                    param = new StringBuilder();
                    continue;
                }
                param.append(paramStr[i]);
            }
            // 最後のパラメータセパレータ以降のアンエスケープ
            unescapeNameValue(paramsMap, param);
            return paramsMap;
        } catch (RuntimeException e) {
            throw new TamperingDetectedException(String.format("failed to convert. params = [%s], param = [%s]", params, param), e);
        }
    }
    
    /**
     * パラメータをアンエスケープし、パラメータを保持するマップに設定する。<br>
     * ただし、想定していないエスケープ文字が見つかった場合、name/valueセパレータが見つからない場合は不正なエスケープのため、
     * {@link IllegalArgumentException}をスローする。
     * @param listParams パラメータを保持するマップ
     * @param param パラメータ文字列
     */
    private void unescapeNameValue(Map<String, List<String>> listParams, StringBuilder param) {
        StringBuilder name = new StringBuilder();
        StringBuilder value = new StringBuilder();
        boolean separatorFound = false;
        char[] nameValueStr = param.toString().toCharArray();
        for (int i = 0; i < nameValueStr.length; i++) {
            char nameValueChar = nameValueStr[i];
            
            // エスケープ文字
            if (nameValueChar == escapeChar) {
                
                // エスケープ文字を捨てて、次の文字のみ使用
                char next  = nameValueStr[i + 1];
                if (next == escapeChar) {
                    nameValueChar = escapeChar;
                } else if (next == paramSeparator) {
                    nameValueChar = paramSeparator;
                } else if (next == nameValueSeparator) {
                    nameValueChar = nameValueSeparator;
                } else {
                    throw new IllegalArgumentException(
                            String.format("invalid escape sequence was found. nameValueStr = [%s]", String.valueOf(nameValueStr)));
                }
                i++;
            
            // name/valueセパレータ
            } else if (nameValueChar == nameValueSeparator) {
                // セパレータの出現有無を切り替え
                separatorFound = true;
                continue;
            }
            
            // セパレータの出現有無によりname又はvalueに追加
            if (separatorFound) {
                value.append(nameValueChar);
            } else {
                name.append(nameValueChar);
            }
        }
        
        if (!separatorFound) {
            throw new IllegalArgumentException(
                    String.format("invalid escape sequence was found. nameValueStr = [%s]", String.valueOf(nameValueStr)));
        }
        
        String key = name.toString();
        if (!listParams.containsKey(key)) {
            listParams.put(key, new ArrayList<String>());
        }
        listParams.get(key).add(value.toString());
    }
    
    /**
     * 文字列中のインデックス位置の文字がパラメータ間のセパレータかを判定する。<br>
     * ただし、先頭又は末尾がセパレータの場合は不正なエスケープのため、{@link IllegalArgumentException}をスローする。
     * @param str 文字列
     * @param index 現在のインデックス位置
     * @return パラメータ間のセパレータの場合はtrue
     */
    private boolean isParamSeparator(char[] str, final int index) {
        
        // インデックス位置の文字がセパレータでない
        if (str[index] != paramSeparator) {
            return false;
        }
        
        // インデックス位置の文字がセパレータ
        boolean isParamSeparator;
        
        // 先頭がセパレータの場合は不正なエスケープ
        if (index == 0) {
            throw new IllegalArgumentException(
                String.format("invalid escape sequence was found. str = [%s]", String.valueOf(str)));
        }
        
        // 1つ前の文字がエスケープ文字
        if (str[index - 1] == escapeChar) {
            
            // 先頭に向かってエスケープ文字がどこまで連続するかカウント
            int escapeCount = 0;
            for (int i = index - 1; i >= 0; i--) {
                if (str[i] == escapeChar) {
                    escapeCount++;
                } else {
                    break;
                }
            }
            
            // カウント値が偶数の場合はセパレータ(エスケープされていないため)
            isParamSeparator = escapeCount % 2 == 0;
            
        // 1つ前の文字がエスケープ文字でない
        } else {
            isParamSeparator = true;
        }
        
        // 末尾がセパレータの場合は不正なエスケープ
        if (isParamSeparator && index == (str.length - 1)) {
            throw new IllegalArgumentException(
                String.format("invalid escape sequence was found. str = [%s]", String.valueOf(str)));
        }
        
        return isParamSeparator;
    }
}
