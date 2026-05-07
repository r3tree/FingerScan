package burp.tdou.fingerscan.core.iconhash;

import burp.tdou.common.helper.IconHash;
import burp.tdou.fingerscan.core.rule.MatchResult;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class IconHashMatcher {

    private final IconHashRuleLoader ruleLoader;

    public IconHashMatcher(IconHashRuleLoader ruleLoader) {
        this.ruleLoader = ruleLoader;
    }

    public List<MatchResult> match(byte[] faviconBytes) {
        if (faviconBytes == null || faviconBytes.length == 0) {
            return new ArrayList<>();
        }

        List<MatchResult> results = new ArrayList<>();

        String murmurHash = IconHash.hash(faviconBytes);
        List<IconHashRule> murmurMatches = ruleLoader.findByMurmurHash(murmurHash);
        for (IconHashRule rule : murmurMatches) {
            results.add(MatchResult.fromIconHash(rule.getName(), "murmur3:" + murmurHash));
        }

        String md5 = computeMd5(faviconBytes);
        List<IconHashRule> md5Matches = ruleLoader.findByMd5(md5);
        for (IconHashRule rule : md5Matches) {
            if (!containsRuleName(results, rule.getName())) {
                results.add(MatchResult.fromIconHash(rule.getName(), "md5:" + md5));
            }
        }

        return results;
    }

    public String computeMurmurHash(byte[] bytes) {
        return IconHash.hash(bytes);
    }

    public String computeMd5(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    private boolean containsRuleName(List<MatchResult> results, String name) {
        for (MatchResult r : results) {
            if (r.getRuleName().equals(name)) {
                return true;
            }
        }
        return false;
    }
}
