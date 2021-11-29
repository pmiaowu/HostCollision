import Bootstrap.*;
import com.github.kevinsawicki.http.HttpRequest;

import java.util.ArrayList;
import java.util.List;

public class HostCollision implements Runnable {
    private ProgramHelpers programHelpers;

    private Statistics statistics;

    private List<List<String>> collisionSuccessList;

    private List<String> scanProtocols;
    private List<String> ipList;
    private List<String> hostList;

    public HostCollision(ProgramHelpers programHelpers, Statistics statistics,
                         List<List<String>> collisionSuccessList, List<String> scanProtocols,
                         List<String> ipList, List<String> hostList) {
        this.programHelpers = programHelpers;
        this.statistics = statistics;
        this.collisionSuccessList = collisionSuccessList;
        this.scanProtocols = scanProtocols;
        this.ipList = ipList;
        this.hostList = hostList;
    }

    @Override
    public void run() {
        for (String ip : ipList) {
            for (String protocol : scanProtocols) {
                try {
                    // 数据样本
                    List<HttpCustomRequest> dataSample = new ArrayList<>();

                    // 基础请求
                    HttpCustomRequest baseRequest = programHelpers.sendHttpGetRequest(protocol, ip, "");

                    // 绝对错误请求
                    HttpCustomRequest errorHostRequest = programHelpers.sendHttpGetRequest(protocol, ip, programHelpers.getErrorHost());

                    // 请求长度判断
                    List<HttpCustomRequest> hcr = new ArrayList<>();
                    hcr.add(baseRequest);
                    hcr.add(errorHostRequest);

                    ReturnFormat requestLengthMatchingReturn = requestLengthMatching(hcr);
                    if (!requestLengthMatchingReturn.result()) {
                        statistics.add("numOfRequest", hostList.size());
                        if (programHelpers.isOutputErrorLog()) {
                            String str = String.format("协议:%s, ip:%s, host:%s 该请求长度为%s 有异常,不进行碰撞-1",
                                    protocol, ip,
                                    requestLengthMatchingReturn.request().host(),
                                    requestLengthMatchingReturn.request().contentLength());
                            System.out.println(str);
                        }
                        continue;
                    }

                    for (String host : hostList) {
                        statistics.add("numOfRequest", 1);

                        try {
                            // 正式进行host碰撞
                            collision(dataSample, baseRequest, errorHostRequest, protocol, ip, host);
                        } catch (HttpRequest.HttpRequestException hre) {
                            if (programHelpers.isOutputErrorLog()) {
                                String str = String.format("协议:%s, ip:%s, host:%s 匹配失败-1", protocol, ip, host);
                                System.out.println(str);
                            }
                            continue;
                        }
                    }
                } catch (HttpRequest.HttpRequestException hre) {
                    statistics.add("numOfRequest", hostList.size());
                    if (programHelpers.isOutputErrorLog()) {
                        String str = String.format("error: 站点 %s 访问失败,不进行host碰撞", protocol + ip);
                        System.out.println(str);
                    }
                }
            }
        }
    }

    /**
     * host碰撞开始
     *
     * @param dataSample
     * @param baseRequest
     * @param errorHostRequest
     * @param protocol
     * @param ip
     * @param host
     */
    private void collision(
            List<HttpCustomRequest> dataSample,
            HttpCustomRequest baseRequest,
            HttpCustomRequest errorHostRequest,
            String protocol, String ip, String host) {
        // 碰撞请求
        HttpCustomRequest newRequest = programHelpers.sendHttpGetRequest(protocol, ip, host);

        // 相对错误请求
        HttpCustomRequest newRequest2 = programHelpers.sendHttpGetRequest(protocol, ip, programHelpers.getRelativeHostName() + host);

        // 请求之间的长度判断
        List<HttpCustomRequest> hcr = new ArrayList<>();
        hcr.add(newRequest);
        hcr.add(newRequest2);

        ReturnFormat requestLengthMatchingReturn = requestLengthMatching(hcr);
        if (!requestLengthMatchingReturn.result()) {
            if (programHelpers.isOutputErrorLog()) {
                String str = String.format("协议:%s, ip:%s, host:%s 该请求长度为%s 有异常,不进行碰撞-2",
                        protocol, ip,
                        requestLengthMatchingReturn.request().host(),
                        requestLengthMatchingReturn.request().contentLength());
                System.out.println(str);
            }
            return;
        }

        // 请求之间的内容匹配
        ReturnFormat requestContentMatchingReturn = requestContentMatching(baseRequest, errorHostRequest, newRequest, newRequest2);
        if (!requestContentMatchingReturn.result()) {
            if (programHelpers.isOutputErrorLog()) {
                String str = String.format("协议:%s, ip:%s, host:%s 匹配失败-2", protocol, ip, host);
                System.out.println(str);
            }
            return;
        }

        // 请求之间的title内容匹配
        ReturnFormat requestTitleMatchingReturn = requestTitleMatching(baseRequest, errorHostRequest, newRequest, newRequest2);
        if (!requestTitleMatchingReturn.result()) {
            if (programHelpers.isOutputErrorLog()) {
                String str = String.format("协议:%s, ip:%s, host:%s 匹配失败-3", protocol, ip, host);
                System.out.println(str);
            }
            return;
        }

        // 相似度匹配
        double htmlSimilarityRatio1 = DiffPage.getRatio(baseRequest.bodyFormat(), newRequest.bodyFormat());
        double htmlSimilarityRatio2 = DiffPage.getRatio(errorHostRequest.bodyFormat(), newRequest.bodyFormat());
        double htmlSimilarityRatio3 = DiffPage.getRatio(newRequest2.bodyFormat(), newRequest.bodyFormat());
        if (htmlSimilarityRatio1 >= programHelpers.getSimilarityRatio() ||
                htmlSimilarityRatio2 >= programHelpers.getSimilarityRatio() ||
                htmlSimilarityRatio3 >= programHelpers.getSimilarityRatio()) {
            if (programHelpers.isOutputErrorLog()) {
                String str = String.format("协议:%s, ip:%s, host:%s 匹配失败-4", protocol, ip, host);
                System.out.println(str);
            }
            return;
        }

        // 数据样本生成
        // 注: 同一ip,相同协议只会生成一次,方便后面进行复用
        if (programHelpers.getDataSampleNumber() > 0 && dataSample.size() == 0) {
            for (int i = 0; i < programHelpers.getDataSampleNumber(); i++) {
                HttpCustomRequest sampleRequest = programHelpers.sendHttpGetRequest(protocol, ip, programHelpers.getErrorHost());
                dataSample.add(sampleRequest);
            }
        }

        // 数据样本比对
        if (dataSample.size() > 0) {
            if (sampleSimilarityCheck(newRequest.bodyFormat(), dataSample)) {
                if (programHelpers.isOutputErrorLog()) {
                    String str = String.format("协议:%s, ip:%s, host:%s, title:%s, 数据包大小:%s, 状态码:%s 数据样本匹配成功,可能为误报,忽略处理",
                            protocol, ip, host, newRequest.title(), newRequest.contentLength(), newRequest.code());
                    System.out.println(str);
                }
                return;
            }
        }

        // 额外样本保存
        // 用于循环时,可以加强数据样本
        // 注: 一定要放在 数据样本比对 的后面,不然会导致漏报
        if (programHelpers.getDataSampleNumber() > 0 && dataSample.size() <= 30) {
            dataSample.add(newRequest);
            dataSample.add(newRequest2);
        }

        // http状态码检查
        if (!httpStatusCodeCheck(String.valueOf(newRequest.code()))) {
            if (programHelpers.isOutputErrorLog()) {
                String str = String.format("协议:%s, ip:%s, host:%s, title:%s, 数据包大小:%s, 状态码:%s 不是白名单状态码,忽略处理",
                        protocol, ip, host, newRequest.title(), newRequest.contentLength(), newRequest.code());
                System.out.println(str);
            }
            return;
        }

        // waf检查
        if (programHelpers.getDataSampleNumber() > 0 && dataSample.size() > 0) {
            ReturnFormat wafFeatureMatchingReturn = wafFeatureMatching(dataSample);
            if (!wafFeatureMatchingReturn.result()) {
                if (programHelpers.isOutputErrorLog()) {
                    String str = String.format("协议:%s, ip:%s, host:%s, title:%s, 数据包大小:%s, 状态码:%s 匹配到waf特征,忽略处理",
                            protocol,
                            ip,
                            wafFeatureMatchingReturn.request().host(),
                            wafFeatureMatchingReturn.request().title(),
                            wafFeatureMatchingReturn.request().contentLength(),
                            wafFeatureMatchingReturn.request().code());
                    System.out.println(str);
                }
                return;
            }
        }

        // host碰撞成功的数据写入
        List<String> data = new ArrayList<>();
        data.add(protocol);
        data.add(ip);
        data.add(host);
        data.add(newRequest.title());

        data.add(String.valueOf(newRequest.contentLength()));
        data.add(String.valueOf(baseRequest.contentLength()));
        data.add(String.valueOf(errorHostRequest.contentLength()));
        data.add(String.valueOf(newRequest2.contentLength()));

        data.add(String.valueOf(newRequest.code()));
        data.add(String.valueOf(baseRequest.code()));
        data.add(String.valueOf(errorHostRequest.code()));
        data.add(String.valueOf(newRequest2.code()));

        // 保存host碰撞成功的数据
        collisionSuccessList.add(data);

        // 实时输出host碰撞成功的日志数据
        String successLog = String.format(
                "协议:%s, ip:%s, host:%s, title:%s, 匹配成功的数据包大小:%s, 状态码:%s 匹配成功",
                protocol, ip, host, newRequest.title(), newRequest.contentLength(), newRequest.code());
        System.out.println(successLog);
    }

    /**
     * 请求之间的长度判断
     * 用途: 初步的误报检测
     * ReturnFormat.result() = true 表示通过, false 表示未通过
     *
     * @param hcr
     * @return
     */
    private ReturnFormat requestLengthMatching(List<HttpCustomRequest> hcr) {
        for (HttpCustomRequest r : hcr) {
            if (r.location() == null && r.contentLength() <= 0) {
                return new ReturnFormat(false, r);
            }
        }
        return new ReturnFormat(true, null);
    }

    /**
     * 请求之间的内容匹配
     * 用途: 初步的误报检测
     * ReturnFormat.result() = true 表示通过, false 表示未通过
     *
     * @param baseRequest
     * @param errorHostRequest
     * @param newRequest
     * @param newRequest2
     * @return
     */
    private ReturnFormat requestContentMatching(
            HttpCustomRequest baseRequest, HttpCustomRequest errorHostRequest,
            HttpCustomRequest newRequest, HttpCustomRequest newRequest2) {
        if (newRequest.filteredPageContent().length() > 0) {
            if (newRequest.filteredPageContent().contains(baseRequest.filteredPageContent())) {
                return new ReturnFormat(false, null);
            }
            if (baseRequest.filteredPageContent().contains(newRequest.filteredPageContent())) {
                return new ReturnFormat(false, null);
            }
        }

        if (errorHostRequest.filteredPageContent().length() > 0) {
            if (newRequest.filteredPageContent().contains(errorHostRequest.filteredPageContent())) {
                return new ReturnFormat(false, null);
            }
            if (errorHostRequest.filteredPageContent().contains(newRequest.filteredPageContent())) {
                return new ReturnFormat(false, null);
            }
        }

        if (newRequest2.filteredPageContent().length() > 0) {
            if (newRequest.filteredPageContent().contains(newRequest2.filteredPageContent())) {
                return new ReturnFormat(false, null);
            }
            if (newRequest2.filteredPageContent().contains(newRequest.filteredPageContent())) {
                return new ReturnFormat(false, null);
            }
        }

        return new ReturnFormat(true, null);
    }

    /**
     * 请求之间的title匹配
     * 用途: 初步的误报检测
     * ReturnFormat.result() = true 表示通过, false 表示未通过
     *
     * @param baseRequest
     * @param errorHostRequest
     * @param newRequest
     * @param newRequest2
     * @return
     */
    private ReturnFormat requestTitleMatching(
            HttpCustomRequest baseRequest, HttpCustomRequest errorHostRequest,
            HttpCustomRequest newRequest, HttpCustomRequest newRequest2) {
        if (newRequest.title().trim().length() > 0) {
            if (newRequest2.title().equals(newRequest.title())) {
                return new ReturnFormat(false, null);
            }

            if (baseRequest.title().equals(newRequest.title())) {
                return new ReturnFormat(false, null);
            }

            if (errorHostRequest.title().equals(newRequest.title())) {
                return new ReturnFormat(false, null);
            }
        }
        return new ReturnFormat(true, null);
    }

    /**
     * 样本相似度检查
     * 用于判断当前字符串与样本数组是否有相似的数据出现
     * true 表示有相似数据出现, false 表示没有相似数据出现
     *
     * @param str
     * @param lr
     * @return Boolean
     */
    private Boolean sampleSimilarityCheck(String str, List<HttpCustomRequest> lr) {
        if (lr.size() == 0) {
            return false;
        }

        for (HttpCustomRequest r : lr) {
            double similarityRatio = DiffPage.getRatio(r.bodyFormat(), str);
            if (similarityRatio >= programHelpers.getSimilarityRatio()) {
                return true;
            }
        }

        return false;
    }

    /**
     * http状态码检查
     * 如果不为白名单里面的状态码,则表示验证失败
     * true 表示通过, false 表示不通过
     *
     * @param str
     * @return Boolean
     */
    private Boolean httpStatusCodeCheck(String str) {
        List<String> collisionSuccessStatusCode = programHelpers.getCollisionSuccessStatusCode();
        if (collisionSuccessStatusCode.size() == 0) {
            return true;
        }

        for (String cssc : collisionSuccessStatusCode) {
            if (cssc.equals(str)) {
                return true;
            }
        }

        return false;
    }

    /**
     * waf特征匹配
     * true 表示通过, false 表示不通过
     *
     * @param lr
     * @return
     */
    private ReturnFormat wafFeatureMatching(List<HttpCustomRequest> lr) {
        for (HttpCustomRequest r : lr) {
            if (httpHeaderServiceWafFeatureMatching(r.server())) {
                return new ReturnFormat(false, r);
            }
            if (httpBodyWafFeatureMatching(r.appBody())) {
                return new ReturnFormat(false, r);
            }
            if (httpHeaderXPoweredByWafFeatureMatching(r.XPoweredBy())) {
                return new ReturnFormat(false, r);
            }
        }
        return new ReturnFormat(true, null);
    }

    /**
     * http请求,header头,Service字段的waf特征匹配
     * true 表示匹配到waf特征, false表示没匹配到waf特征
     *
     * @param s
     * @return Boolean
     */
    private Boolean httpHeaderServiceWafFeatureMatching(String s) {
        if (s != null && !s.equals("") && programHelpers.getHttpServiceBlacklists().size() > 0) {
            for (String sbl : programHelpers.getHttpServiceBlacklists()) {
                s = s.trim().toLowerCase().replace(" ", "");
                sbl = sbl.replace(" ", "");
                if (s.contains(sbl)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * http请求,body的waf特征匹配
     * true 表示匹配到waf特征, false表示没匹配到waf特征
     *
     * @param s
     * @return Boolean
     */
    private Boolean httpBodyWafFeatureMatching(String s) {
        if (s != null && !s.equals("") && programHelpers.getHttpBodyBlacklists().size() > 0) {
            for (String bbl : programHelpers.getHttpBodyBlacklists()) {
                s = s.trim().toLowerCase().replace(" ", "");
                bbl = bbl.replace(" ", "");
                if (s.contains(bbl)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * http请求,header头,X-Powered-By字段的waf特征匹配
     * true 表示匹配到waf特征, false表示没匹配到waf特征
     *
     * @param s
     * @return Boolean
     */
    private Boolean httpHeaderXPoweredByWafFeatureMatching(String s) {
        if (s != null && !s.equals("") && programHelpers.getHttpXPoweredByBlacklists().size() > 0) {
            for (String xl : programHelpers.getHttpXPoweredByBlacklists()) {
                s = s.trim().toLowerCase().replace(" ", "");
                xl = xl.replace(" ", "");
                if (s.contains(xl)) {
                    return true;
                }
            }
        }
        return false;
    }
}
