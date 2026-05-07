package burp.tdou.fingerscan.core.rule;

import java.util.Map;

/**
 * 规则匹配结果
 * 统一表示 JSON 规则引擎和 YAML 规则引擎的匹配结果
 */
public class MatchResult {

    /**
     * 匹配来源
     */
    public enum Source {
        /** JSON 规则引擎 (FpManager) */
        JSON_RULE,
        /** YAML 规则引擎 (正则匹配) */
        YAML_RULE,
        /** Icon Hash 引擎 (favicon哈希匹配) */
        ICON_HASH
    }

    private final Source source;
    private final String ruleName;
    private final String matchedContent;
    private final int color;
    private final Map<String, String> params;

    public MatchResult(Source source, String ruleName, String matchedContent,
                       int color, Map<String, String> params) {
        this.source = source;
        this.ruleName = ruleName;
        this.matchedContent = matchedContent;
        this.color = color;
        this.params = params;
    }

    public Source getSource() { return source; }
    public String getRuleName() { return ruleName; }
    public String getMatchedContent() { return matchedContent; }
    public int getColor() { return color; }
    public Map<String, String> getParams() { return params; }

    /**
     * 从 JSON 规则引擎结果创建
     */
    public static MatchResult fromJsonRule(String ruleName, int color, Map<String, String> params) {
        return new MatchResult(Source.JSON_RULE, ruleName, null, color, params);
    }

    /**
     * 从 YAML 规则引擎结果创建
     */
    public static MatchResult fromYamlRule(String ruleName, String matchedRegex) {
        return new MatchResult(Source.YAML_RULE, ruleName, matchedRegex, 0, null);
    }

    /**
     * 从 Icon Hash 引擎结果创建
     */
    public static MatchResult fromIconHash(String ruleName, String matchedHash) {
        return new MatchResult(Source.ICON_HASH, ruleName, matchedHash, 0, null);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s", source, ruleName);
    }
}
