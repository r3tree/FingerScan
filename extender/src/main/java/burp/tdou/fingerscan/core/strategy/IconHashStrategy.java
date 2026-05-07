package burp.tdou.fingerscan.core.strategy;

import burp.api.montoya.http.message.HttpRequestResponse;
import burp.api.montoya.http.message.responses.HttpResponse;
import burp.tdou.common.log.Logger;
import burp.tdou.fingerscan.core.ScanRequest;
import burp.tdou.fingerscan.core.ScanTask;
import burp.tdou.fingerscan.core.iconhash.FaviconDetector;
import burp.tdou.fingerscan.core.iconhash.FaviconLinkExtractor;
import burp.tdou.fingerscan.core.iconhash.FaviconRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IconHashStrategy implements ScanStrategy {

    private final FaviconRegistry faviconRegistry;

    public IconHashStrategy(FaviconRegistry faviconRegistry) {
        this.faviconRegistry = faviconRegistry;
    }

    @Override
    public boolean shouldApply(ScanRequest request) {
        if (!request.hasResponse()) {
            return false;
        }

        HttpRequestResponse reqResp = request.getHttpReqResp();
        HttpResponse response = reqResp.response();
        String contentType = extractContentType(response);

        if (FaviconDetector.isHtmlContentType(contentType)) {
            extractAndRegisterFavicons(request, response);
            return false;
        }

        return faviconRegistry.isFavicon(request.getHost(), request.getPath());
    }

    @Override
    public List<ScanTask> generateTasks(ScanRequest request) {
        if (!shouldApply(request)) {
            return Collections.emptyList();
        }

        String faviconUrl = request.getHost() + request.getPath();
        String dedupKey = "iconhash:" + faviconUrl;
        HttpRequestResponse reqResp = request.getHttpReqResp();

        List<ScanTask> tasks = new ArrayList<>();
        tasks.add(ScanTask.iconHash(reqResp, request.getService(), dedupKey, request.getFrom()));
        return tasks;
    }

    @Override
    public String getName() {
        return "IconHash";
    }

    private void extractAndRegisterFavicons(ScanRequest request, HttpResponse response) {
        try {
            String body = response.bodyToString();
            if (body == null || body.isEmpty()) {
                return;
            }

            List<FaviconLinkExtractor.FaviconLink> links =
                    FaviconLinkExtractor.extract(body, request.getPath());
            String pageHost = request.getHost();
            for (FaviconLinkExtractor.FaviconLink link : links) {
                String host = link.host != null ? link.host : pageHost;
                faviconRegistry.register(host, link.path);
                Logger.debug("IconHashStrategy: registered favicon %s for host %s", link.path, host);
            }
        } catch (Exception e) {
            Logger.error("IconHashStrategy: failed to extract favicon links: %s", e.getMessage());
        }
    }

    private String extractContentType(HttpResponse response) {
        if (response == null) {
            return "";
        }
        try {
            return response.headerValue("Content-Type");
        } catch (Exception e) {
            return "";
        }
    }
}
