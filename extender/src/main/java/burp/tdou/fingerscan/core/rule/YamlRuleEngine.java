package burp.tdou.fingerscan.core.rule;

import burp.tdou.common.log.Logger;
import burp.tdou.fingerscan.config.YamlConfigLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * YAML 规则引擎
 * 基于 YAML 配置的正则匹配指纹识别，替代原来的 FingerprintScanner 中分散的匹配逻辑
 *
 * 改进:
 * - Pattern 缓存避免重复编译
 * - 通过 YamlConfigLoader 使用带缓存的配置读取
 * - 线程安全的 Pattern 缓存
 */
public class YamlRuleEngine implements RuleEngine {

    private final YamlConfigLoader configLoader;
    private final ConcurrentHashMap<String, Pattern> patternCache = new ConcurrentHashMap<>();

    public YamlRuleEngine(YamlConfigLoader configLoader) {
        this.configLoader = configLoader;
    }

    @Override
    public List<MatchResult> match(byte[] request, byte[] response) {
        if (response == null || response.length == 0) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> rules = configLoader.getEnabledRules();
        if (rules.isEmpty()) {
            return Collections.emptyList();
        }

        String responseStr = new String(response);
        List<MatchResult> results = new ArrayList<>();

        for (Map<String, Object> rule : rules) {
            try {
                String regex = getStringField(rule, "re");
                String name = getStringField(rule, "name");

                if (regex == null || regex.isEmpty() || name == null) {
                    continue;
                }

                if (matchesRegex(responseStr, regex)) {
                    results.add(MatchResult.fromYamlRule(name, regex));
                }
            } catch (Exception e) {
                Logger.debug("YamlRuleEngine match error: %s", e.getMessage());
            }
        }

        return results;
    }

    @Override
    public List<String> getScanPaths() {
        return configLoader.getScanPaths();
    }

    /**
     * 正则匹配（带 Pattern 缓存）
     */
    private boolean matchesRegex(String content, String regex) {
        try {
            Pattern pattern = patternCache.computeIfAbsent(regex, r -> {
                try {
                    return Pattern.compile(r, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
                } catch (Exception e) {
                    Logger.debug("Invalid regex: %s, error: %s", r, e.getMessage());
                    return null;
                }
            });

            if (pattern == null) {
                return false;
            }

            Matcher matcher = pattern.matcher(content);
            return matcher.find();
        } catch (Exception e) {
            Logger.debug("Regex match error: %s", e.getMessage());
            return false;
        }
    }

    private String getStringField(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * 清空 Pattern 缓存
     */
    public void clearPatternCache() {
        patternCache.clear();
    }

    /**
     * 获取缓存大小
     */
    public int getPatternCacheSize() {
        return patternCache.size();
    }
}
