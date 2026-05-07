package burp.tdou.fingerscan.core.iconhash;

public class IconHashRule {

    private final String name;
    private final String murmurHash;
    private final String md5;
    private final String type;
    private final String info;

    public IconHashRule(String name, String murmurHash, String md5, String type, String info) {
        this.name = name;
        this.murmurHash = murmurHash;
        this.md5 = md5;
        this.type = type;
        this.info = info;
    }

    public String getName() { return name; }
    public String getMurmurHash() { return murmurHash; }
    public String getMd5() { return md5; }
    public String getType() { return type; }
    public String getInfo() { return info; }

    public boolean hasMurmurHash() { return murmurHash != null && !murmurHash.isEmpty(); }
    public boolean hasMd5() { return md5 != null && !md5.isEmpty(); }
}
