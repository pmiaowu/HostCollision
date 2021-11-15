package Bootstrap;

import com.github.kevinsawicki.http.HttpRequest;
import org.apache.commons.cli.CommandLine;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ProgramHelpers {
    private static YamlReader yamlReader = YamlReader.getInstance();
    private static CommandLine commandLine;

    public ProgramHelpers(CommandLine commandLine) {
        this.commandLine = commandLine;
    }

    /**
     * 获取程序运行的最大线程总数
     *
     * @return Integer
     */
    public Integer getThreadTotal() {
        Integer threadTotal;

        if (commandLine.hasOption("t")) {
            String t = commandLine.getOptionValue("t");
            threadTotal = Integer.valueOf(t);
        } else {
            threadTotal = yamlReader.getInteger("threadTotal");
        }

        if (threadTotal <= 0) {
            threadTotal = 1;
        }

        return threadTotal;
    }

    /**
     * 获取相似度比例
     *
     * @return Double
     */
    public Double getSimilarityRatio() {
        return yamlReader.getDouble("similarityRatio");
    }

    /**
     * 获取绝对错误的host地址
     *
     * @return String
     */
    public String getErrorHost() {
        return yamlReader.getString("http.errorHost");
    }

    /**
     * 路径格式化
     *
     * @return String
     */
    private String pathFormat(String path) {
        path = path.replace("/", File.separator);
        path = path.replace("\\", File.separator);

        String pathFormat;
        if (path.substring(0, 2).equals("." + File.separator)) {
            pathFormat = CustomHelpers.getResourcePath() + path.substring(1);
        } else if (path.substring(0, 1).equals(File.separator)) {
            pathFormat = path;
        } else {
            pathFormat = CustomHelpers.getResourcePath() + File.separator + path;
        }
        return pathFormat;
    }

    /**
     * 获取ip文件路径
     *
     * @return String
     */
    public String getIpPath() {
        String ipPath;

        if (commandLine.hasOption("ifp")) {
            String ifp = commandLine.getOptionValue("ifp");
            ipPath = pathFormat(ifp);
        } else {
            ipPath = pathFormat(yamlReader.getString("dataSource.ipFilePath"));
        }

        return ipPath;
    }

    /**
     * 获取host文件路径
     *
     * @return String
     */
    public String getHostPath() {
        String hostPath;

        if (commandLine.hasOption("hfp")) {
            String hfp = commandLine.getOptionValue("hfp");
            hostPath = pathFormat(hfp);
        } else {
            hostPath = pathFormat(yamlReader.getString("dataSource.hostFilePath"));
        }

        return hostPath;
    }

    /**
     * 是否将结果保存为csv
     *
     * @return Boolean
     */
    public Boolean isOutputCsv() {
        Boolean isOutputCsv = false;

        if (commandLine.hasOption("o")) {
            String[] oArr = commandLine.getOptionValue("o").trim().toLowerCase().split(",");
            for (String o : oArr) {
                if (o.trim().equals("csv")) {
                    isOutputCsv = true;
                    break;
                }
            }
        } else {
            isOutputCsv = yamlReader.getBoolean("defaultResultOutput.isOutputCsv");
        }

        return isOutputCsv;
    }

    /**
     * 是否将结果保存为txt
     *
     * @return Boolean
     */
    public Boolean isOutputTxt() {
        Boolean isOutputTxt = false;

        if (commandLine.hasOption("o")) {
            String[] oArr = commandLine.getOptionValue("o").trim().toLowerCase().split(",");
            for (String o : oArr) {
                if (o.trim().equals("txt")) {
                    isOutputTxt = true;
                    break;
                }
            }
        } else {
            isOutputTxt = yamlReader.getBoolean("defaultResultOutput.isOutputTxt");
        }

        return isOutputTxt;
    }

    /**
     * 获取扫描协议
     *
     * @return List<String>
     */
    public List<String> getScanProtocols() {
        List<String> protocols = new ArrayList();

        if (commandLine.hasOption("sp")) {
            String sp = commandLine.getOptionValue("sp");
            String[] spArr = sp.trim().toLowerCase().split(",");
            for (String s : spArr) {
                if (s.trim().equals("http")) {
                    protocols.add("http://");
                }

                if (s.trim().equals("https")) {
                    protocols.add("https://");
                }
            }
        } else {
            if (yamlReader.getBoolean("http.scanProtocol.isScanHttp")) {
                protocols.add("http://");
            }

            if (yamlReader.getBoolean("http.scanProtocol.isScanHttps")) {
                protocols.add("https://");
            }
        }

        return protocols;
    }

    /**
     * 发送Http Get请求
     *
     * @param ip
     * @param host
     * @return HttpRequest
     */
    public HttpCustomRequest sendHttpGetRequest(String protocol, String ip, String host) {
        // 一定要固定一个 userAgent 头
        // 有的请求不一样的 userAgent 返回的内容也不一样,容易导致误报
        String userAgent = "Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.198 Safari/537.36";

        String url = protocol + ip;
        HttpRequest request = HttpRequest.get(url);
        if (yamlReader.getBoolean("http.proxy.isStart")) {
            request.useProxy(
                    yamlReader.getString("http.proxy.host"),
                    yamlReader.getInteger("http.proxy.port"));
            request.proxyBasic(
                    yamlReader.getString("http.proxy.username"),
                    yamlReader.getString("http.proxy.password"));
        }
        request.trustAllCerts();
        request.trustAllHosts();
        request.followRedirects(false);
        request.header("User-Agent", userAgent);
        request.header("Accept", "*/*");
        if (host.length() > 0) {
            request.header("Host", host);
        }
        request.readTimeout(yamlReader.getInteger("http.readTimeout") * 1000);
        request.connectTimeout(yamlReader.getInteger("http.connectTimeout") * 1000);
        return new HttpCustomRequest(request, host);
    }

    /**
     * 是否将错误日志输出
     *
     * @return Boolean
     */
    public Boolean isOutputErrorLog() {
        if (commandLine.hasOption("ioel")) {
            String ioel = commandLine.getOptionValue("ioel").trim().toLowerCase();
            if (ioel.equals("false")) {
                return false;
            } else {
                return true;
            }
        }
        return yamlReader.getBoolean("isOutputErrorLog");
    }

    /**
     * 获取认为碰撞成功的状态码列表
     *
     * @return List<String>
     */
    public List<String> getCollisionSuccessStatusCode() {
        List<String> statusCodeList = new ArrayList();

        String[] csscArr;
        if (commandLine.hasOption("cssc")) {
            csscArr = commandLine.getOptionValue("cssc").trim().toLowerCase().split(",");
        } else {
            csscArr = yamlReader.getString("collisionSuccessStatusCode").trim().toLowerCase().split(",");
        }

        for (String cssc : csscArr) {
            if (cssc.trim().length() > 0) {
                statusCodeList.add(cssc.trim());
            }
        }

        return statusCodeList;
    }

    /**
     * 获取相对主机名
     *
     * @return String
     */
    public String getRelativeHostName() {
        return yamlReader.getString("http.relativeHostName");
    }

    /**
     * 获取数据样本请求次数
     *
     * @return Integer
     */
    public Integer getDataSampleNumber() {
        if (commandLine.hasOption("dsn")) {
            String dsn = commandLine.getOptionValue("dsn");
            return Integer.valueOf(dsn);
        }

        return yamlReader.getInteger("dataSample.number");
    }

    /**
     * 获取http请求,header头,Service字段的黑名单列表
     *
     * @return List<String>
     */
    public List<String> getHttpServiceBlacklists() {
        List<String> blacklists = new ArrayList();

        List<String> services = yamlReader.getStringList("blacklists.httpServices");

        for (String service : services) {
            if (service.trim().length() > 0) {
                blacklists.add(service.trim().toLowerCase());
            }
        }

        return blacklists;
    }

    /**
     * 获取http请求,body的黑名单列表
     *
     * @return List<String>
     */
    public List<String> getHttpBodyBlacklists() {
        List<String> blacklists = new ArrayList();

        List<String> bodies = yamlReader.getStringList("blacklists.httpBodies");

        for (String body : bodies) {
            if (body.trim().length() > 0) {
                blacklists.add(body.trim().toLowerCase());
            }
        }

        return blacklists;
    }

    /**
     * 获取http请求,header头,X-Powered-By字段的黑名单列表
     *
     * @return
     */
    public List<String> getHttpXPoweredByBlacklists() {
        List<String> blacklists = new ArrayList();

        List<String> xl = yamlReader.getStringList("blacklists.httpXPoweredBy");

        for (String x : xl) {
            if (x.trim().length() > 0) {
                blacklists.add(x.trim().toLowerCase());
            }
        }

        return blacklists;
    }
}