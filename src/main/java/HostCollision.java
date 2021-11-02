import Bootstrap.CustomHelpers;
import Bootstrap.DiffPage;
import Bootstrap.ProgramHelpers;
import Bootstrap.RequestStatistics;
import com.github.kevinsawicki.http.HttpRequest;

import java.util.ArrayList;
import java.util.List;

public class HostCollision implements Runnable {
    private ProgramHelpers programHelpers;

    private RequestStatistics requestStatistics;

    private List<List<String>> collisionSuccessList;

    private List<String> scanProtocols;
    private List<String> ipList;
    private List<String> hostList;

    public HostCollision(
            ProgramHelpers programHelpers,
            RequestStatistics requestStatistics,
            List<List<String>> collisionSuccessList,
            List<String> scanProtocols,
            List<String> ipList,
            List<String> hostList) {
        this.programHelpers = programHelpers;
        this.requestStatistics = requestStatistics;
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
                    HttpRequest baseRequest = programHelpers.sendHttpGetRequest(protocol, ip, "");
                    HttpRequest errorHostRequest = programHelpers.sendHttpGetRequest(protocol, ip, programHelpers.getErrorHost());

                    String baseRequestBody = baseRequest.body();
                    String baseRequestContent = DiffPage.getFilteredPageContent(baseRequestBody.replace(ip,""));
                    Integer baseRequestLength = baseRequestBody.length();

                    String errorHostRequestBody = errorHostRequest.body();
                    String errorHostRequestContent = DiffPage.getFilteredPageContent(errorHostRequestBody.replace(programHelpers.getErrorHost(),""));
                    Integer errorHostRequestLength = errorHostRequestBody.length();

                    for (String host : hostList) {
                        requestStatistics.add("numOfRequest", 1);

                        // 正式进行host碰撞
                        try {
                            HttpRequest newRequest = programHelpers.sendHttpGetRequest(protocol, ip, host);

                            String newRequestBody = newRequest.body();
                            String newRequestContent = DiffPage.getFilteredPageContent(newRequestBody.replace(host,""));
                            Integer newRequestLength = newRequestBody.length();

                            // 进行简单的内容匹配
                            if (newRequestLength > 0 && baseRequestLength > 0) {
                                if (newRequestContent.contains(baseRequestContent)) {
                                    String str = String.format("协议:%s, ip:%s, host:%s 匹配失败", protocol, ip, host);
                                    System.out.println(str);
                                    continue;
                                }
                            }
                            if (newRequestLength > 0 && errorHostRequestLength > 0) {
                                if (newRequestContent.contains(errorHostRequestContent)) {
                                    String str = String.format("协议:%s, ip:%s, host:%s 匹配失败", protocol, ip, host);
                                    System.out.println(str);
                                    continue;
                                }
                            }

                            // 相似度匹配
                            double htmlSimilarityRatio1 = DiffPage.getRatio(baseRequestBody, newRequestBody);
                            double htmlSimilarityRatio2 = DiffPage.getRatio(errorHostRequestBody, newRequestBody);
                            if (htmlSimilarityRatio1 >= programHelpers.getSimilarityRatio()) {
                                String str = String.format("协议:%s, ip:%s, host:%s 匹配失败", protocol, ip, host);
                                System.out.println(str);
                                continue;
                            }
                            if (htmlSimilarityRatio2 >= programHelpers.getSimilarityRatio()) {
                                String str = String.format("协议:%s, ip:%s, host:%s 匹配失败", protocol, ip, host);
                                System.out.println(str);
                                continue;
                            }

                            // host碰撞成功的数据写入
                            List<String> data = new ArrayList<>();
                            data.add(protocol);
                            data.add(ip);
                            data.add(host);
                            data.add(CustomHelpers.getBodyTitle(newRequestBody));
                            data.add(String.valueOf(newRequestLength));
                            data.add(String.valueOf(baseRequestLength));
                            data.add(String.valueOf(errorHostRequestLength));

                            // 保存host碰撞成功的数据
                            collisionSuccessList.add(data);

                            // 实时输出host碰撞成功的日志数据
                            String successLog = String.format(
                                    "协议:%s, ip:%s, host:%s, title:%s, 匹配成功的数据包大小:%s 匹配成功",
                                    protocol, ip, host, CustomHelpers.getBodyTitle(newRequestBody), newRequestLength);
                            System.out.println(successLog);
                        } catch (HttpRequest.HttpRequestException hre) {
                            String str = String.format("协议:%s, ip:%s, host:%s 匹配失败", protocol, ip, host);
                            System.out.println(str);
                            continue;
                        }
                    }
                } catch (HttpRequest.HttpRequestException hre) {
                    requestStatistics.add("numOfRequest", hostList.size());

                    String str = String.format("error: 站点 %s 访问失败,不进行host碰撞", protocol + ip);
                    System.out.println(str);
                }
            }
        }
    }
}
