package burp.tdou.fingerscan.bean;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 任务数据
 */
public class TaskData {

    private int id;
    private String from;
    private String method;
    private String host;
    private String url;
    private String title;
    private String ip;
    private int status;
    private int length;
    private String fingerprint;
    private Map<String, String> params;
    private Object reqResp;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public int getLength() { return length; }
    public void setLength(int length) { this.length = length; }

    public String getFingerprint() { return fingerprint; }
    public void setFingerprint(String fingerprint) { this.fingerprint = fingerprint; }

    public Map<String, String> getParams() {
        if (params == null) {
            params = new LinkedHashMap<>();
        }
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params != null ? new LinkedHashMap<>(params) : new LinkedHashMap<>();
    }

    public Object getReqResp() { return reqResp; }
    public void setReqResp(Object reqResp) { this.reqResp = reqResp; }
}
