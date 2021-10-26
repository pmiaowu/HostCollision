import Bootstrap.CustomHelpers;
import Bootstrap.DiffPage;
import Bootstrap.YamlReader;
import com.csvreader.CsvWriter;
import com.github.kevinsawicki.http.HttpRequest;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Run {
    private static YamlReader yamlReader = YamlReader.getInstance();

    private static String errorHost = "error.miaohosttest6666.com";

    private static List<String> collisionSuccessLogs = new ArrayList<String>();

    public static void main(String[] args) {
        // 必须加不然会导致无法修改 Host
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");

        CsvWriter csvWriter = new CsvWriter(getCsvFilePath(), ',', Charset.forName("GBK"));

        String ipPath = CustomHelpers.getDataSourcePath() + yamlReader.getString("dataSource.ipFilePath");
        String hostPath = CustomHelpers.getDataSourcePath() + yamlReader.getString("dataSource.hostFilePath");

        try {
            String ipData = getFileData(ipPath).trim();
            if (ipData.length() == 0) {
                System.out.println("error: ip数据来源, 获取为空数据, 退出程序 :(");
                return;
            }

            String hostData = getFileData(hostPath).trim();
            if (hostData.length() == 0) {
                System.out.println("error: host数据来源, 获取为空数据, 退出程序 :(");
                return;
            }

            if (getScanProtocol().size() == 0) {
                System.out.println("error: 扫描协议空, 退出程序 :(");
                return;
            }

            // 写入csv表头
            String[] headers = {
                    "协议", "ip", "host", "标题",
                    "匹配成功的数据包大小", "原始的数据包大小", "绝对错误的数据包大小"};
            csvWriter.writeRecord(headers);

            System.out.println("=======================开 始 碰 撞=======================");

            for (String ip : convertStringToList(ipData, "\n")) {
                ip = ip.trim();
                if (ip.length() == 0) {
                    continue;
                }

                for (String protocol : getScanProtocol()) {
                    try {
                        HttpRequest baseRequest = sendHttpGetRequest(protocol, ip, "");
                        HttpRequest errorHostRequest = sendHttpGetRequest(protocol, ip, errorHost);
                        String baseRequestBody = baseRequest.body();
                        String errorHostRequestBody = errorHostRequest.body();

                        for (String host : convertStringToList(hostData, "\n")) {
                            host = host.trim();
                            if (host.length() == 0) {
                                continue;
                            }

                            // 进行碰撞
                            try {
                                HttpRequest newRequest = sendHttpGetRequest(protocol, ip, host);
                                String newRequestBody = newRequest.body();

                                // 相似度匹配
                                double htmlSimilarityRatio1 = DiffPage.getRatio(baseRequestBody, newRequestBody);
                                double htmlSimilarityRatio2 = DiffPage.getRatio(errorHostRequestBody, newRequestBody);
                                if (htmlSimilarityRatio1 >= yamlReader.getDouble("similarityRatio")) {
                                    String str = String.format("协议:%s, ip:%s, host:%s 匹配失败", protocol, ip, host);
                                    System.out.println(str);
                                    continue;
                                }
                                if (htmlSimilarityRatio2 >= yamlReader.getDouble("similarityRatio")) {
                                    String str = String.format("协议:%s, ip:%s, host:%s 匹配失败", protocol, ip, host);
                                    System.out.println(str);
                                    continue;
                                }

                                // host碰撞成功的数据写入
                                String[] data = {
                                        protocol, ip, host, getBodyTitle(newRequestBody),
                                        String.valueOf(newRequestBody.length()),
                                        String.valueOf(baseRequestBody.length()),
                                        String.valueOf(errorHostRequestBody.length())};
                                csvWriter.writeRecord(data);

                                // 实时输出host碰撞成功的日志数据
                                String successLog = String.format(
                                        "协议:%s, ip:%s, host:%s, title:%s, 匹配成功的数据包大小:%s 匹配成功",
                                        protocol, ip, host, getBodyTitle(newRequestBody), newRequestBody.length());
                                System.out.println(successLog);

                                // 保存host碰撞成功的日志数据
                                collisionSuccessLogs.add(successLog);
                            } catch (HttpRequest.HttpRequestException hre) {
                                String str = String.format("协议:%s, ip:%s, host:%s 匹配失败", protocol, ip, host);
                                System.out.println(str);
                                continue;
                            }
                        }
                    } catch (HttpRequest.HttpRequestException hre) {
                        String str = String.format("error: 站点 %s 访问失败,不进行host碰撞", protocol + ip);
                        System.out.println(str);
                    }
                }
            }

            // host碰撞成功的日志输出
            if (collisionSuccessLogs.size() > 0) {
                System.out.println(" ");
                System.out.println(" ");
                System.out.println(" ");
                System.out.println(" ");
                System.out.println(" ");
                System.out.println("====================碰 撞 成 功 列 表====================");
                for (String log : collisionSuccessLogs) {
                    System.out.println(log);
                }
                System.out.println(" ");
                System.out.println(" ");
            }
        } catch (IOException e) {
            System.out.println("error: 运行错误 文件读/写出错 :(");
            e.printStackTrace();
            return;
        } finally {
            csvWriter.close();
        }
    }

    /**
     * 生成csv文件路径
     *
     * @return
     */
    private static String getCsvFilePath() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String date = sdf.format(new Date()); // 格式化日期 date: 2021-10-16
        return "." + File.separator + date + "_" + CustomHelpers.randomStr(8) + ".csv";
    }

    /**
     * 获取扫描协议
     *
     * @return List<String>
     */
    private static List<String> getScanProtocol() {
        List<String> protocols = new ArrayList();

        if (yamlReader.getBoolean("http.scanProtocol.isScanHttp")) {
            protocols.add("http://");
        }

        if (yamlReader.getBoolean("http.scanProtocol.isScanHttps")) {
            protocols.add("https://");
        }

        return protocols;
    }

    /**
     * 获取文件数据
     *
     * @param path
     * @return String
     */
    private static String getFileData(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        return new String(bytes);
    }

    /**
     * 字符串转换为列表
     *
     * @param str  要转换为list的数据
     * @param mark 分隔符
     * @return List<String>
     */
    private static List<String> convertStringToList(String str, String mark) {
        List<String> result = Arrays.asList(str.split(mark));
        return result;
    }

    /**
     * 发送Http Get请求
     *
     * @param ip
     * @param host
     * @return HttpRequest
     */
    private static HttpRequest sendHttpGetRequest(String protocol, String ip, String host) {
        String url = protocol + ip;
        HttpRequest request = HttpRequest.get(url);
        if (yamlReader.getBoolean("http.proxy.isStart")) {
            request.useProxy(
                    yamlReader.getString("http.proxy.host"),
                    yamlReader.getInteger("http.proxy.port"));
        }
        request.trustAllCerts();
        request.trustAllHosts();
        request.header("User-Agent", CustomHelpers.getRandomUserAgent());
        request.header("Accept", "*/*");
        if (host.length() > 0) {
            request.header("Host", host);
        }
        request.readTimeout(yamlReader.getInteger("http.readTimeout") * 1000);
        request.connectTimeout(yamlReader.getInteger("http.connectTimeout") * 1000);
        return request;
    }

    /**
     * 获得网页标题
     *
     * @param s
     * @return
     */
    private static String getBodyTitle(String s) {
        String regex;
        String title = "";
        final List<String> list = new ArrayList<String>();
        regex = "<title>.*?</title>";
        final Pattern pa = Pattern.compile(regex, Pattern.CANON_EQ);
        final Matcher ma = pa.matcher(s);
        while (ma.find()) {
            list.add(ma.group());
        }

        for (int i = 0; i < list.size(); i++) {
            title = title + list.get(i);
        }

        return title.replaceAll("<.*?>", "");
    }
}