import Bootstrap.ConsoleProgressBar;
import Bootstrap.CustomHelpers;
import Bootstrap.ProgramHelpers;
import Bootstrap.RequestStatistics;
import com.csvreader.CsvWriter;
import org.apache.commons.cli.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Main {
    private static String VERSION = "2.0.0";

    private static ProgramHelpers programHelpers;

    // 用于多线程下 统计发送请求 的类
    private static RequestStatistics requestStatistics = new RequestStatistics();

    // 保存着所有碰撞成功的数据
    private static List<List<String>> collisionSuccessList = Collections.synchronizedList(new ArrayList<>());

    private static List<String> scanProtocols;
    private static String ipData;
    private static String hostData;

    private static Boolean isOutputCsv;
    private static Boolean isOutputTxt;

    private static CsvWriter csvWriter = null;
    private static BufferedWriter txtWriter = null;

    public static void main(String[] args) throws ParseException {
        // 必须加不然会导致无法修改 Host
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");

        // 基本信息输出
        // 作者拿来臭美用的 ╰(*°▽°*)╯
        System.out.println(basicInformationOutput());

        // 程序初始化操作
        init(args);

        // 程序运行的入口函数
        run();
    }

    /**
     * 获取命令行参数
     *
     * @param args
     * @return CommandLine
     */
    private static CommandLine getCommandLine(String[] args) throws ParseException {
        CommandLineParser parser = new DefaultParser();

        Options options = new Options();
        options.addOption("h", "help", false, "帮助");
        options.addOption("sp", "scanProtocol", true, "允许的扫描协议<例如:http,https>");
        options.addOption("ifp", "ipFilePath", true, "ip数据来源地址<例如:./dataSource/ipList.txt>");
        options.addOption("hfp", "hostFilePath", true, "host数据来源地址<例如:./dataSource/hostList.txt>");
        options.addOption("t", "threadTotal", true, "程序运行的最大线程总数<例如:6>");
        options.addOption("o", "output", true, "导出格式,使用逗号分割<例如:csv,txt>");

        CommandLine commandLine = parser.parse(options, args);
        return commandLine;
    }

    /**
     * 基本信息输出
     */
    private static String basicInformationOutput() {
        String str1 = "=======================基 本 信 息=======================\n";
        String str2 = String.format("版本: %s\n", VERSION);
        String str3 = String.format("下载地址: %s\n", "https://github.com/pmiaowu/HostCollision");
        String str4 = "请尽情享用本程序吧 ヾ(≧▽≦*)o";
        String detail = str1 + str2 + str3 + str4;
        return detail;
    }

    /**
     * 程序帮助信息
     */
    private static void help() {
        System.out.println("=======================使 用 文 档=======================");
        System.out.println("-h/-help            使用文档");
        System.out.println("-sp/-scanProtocol   允许的扫描协议<例如:http,https>");
        System.out.println("-ifp/-ipFilePath    ip数据来源地址<例如:./dataSource/ipList.txt>");
        System.out.println("-hfp/-hostFilePath  host数据来源地址<例如:./dataSource/hostList.txt>");
        System.out.println("-t/-threadTotal     程序运行的最大线程总数<例如:6>");
        System.out.println("-o/-output          导出格式,使用逗号分割<例如:csv,txt>");
    }

    /**
     * 程序初始化操作
     */
    private static void init(String[] args) throws ParseException {
        // 初始化 - 请求数设置为0
        requestStatistics.add("numOfRequest", 0);

        CommandLine commandLine = getCommandLine(args);
        if (commandLine.hasOption("h")) {
            help();
            System.exit(0);
        }

        programHelpers = new ProgramHelpers(commandLine);

        scanProtocols = programHelpers.getScanProtocols();

        isOutputCsv = programHelpers.isOutputCsv();
        isOutputTxt = programHelpers.isOutputTxt();

        try {
            ipData = CustomHelpers.getFileData(programHelpers.getIpPath()).trim();
            hostData = CustomHelpers.getFileData(programHelpers.getHostPath()).trim();

            if (scanProtocols.size() == 0) {
                System.out.println(" ");
                System.out.println("扫描协议空, 退出程序 :(");
                System.exit(0);
            }

            if (ipData.length() == 0) {
                System.out.println(" ");
                System.out.println("error: ip数据来源, 获取为空数据, 退出程序 :(");
                System.exit(0);
            }

            if (hostData.length() == 0) {
                System.out.println(" ");
                System.out.println("error: host数据来源, 获取为空数据, 退出程序 :(");
                System.exit(0);
            }
        } catch (IOException e) {
            System.out.println(" ");
            System.out.println("error: 文件读/写出错 :(");
            e.printStackTrace();
            System.exit(0);
        }
    }

    /**
     * 程序运行的入口函数
     */
    private static void run() {
        // 控制台进度条类
        Integer requestTotal = (getIpList().size() * scanProtocols.size() * getHostList().size());
        ConsoleProgressBar consoleProgressBar = new ConsoleProgressBar(0, requestTotal);

        // 创建csv实例
        if (isOutputCsv) {
            csvWriter = new CsvWriter(CustomHelpers.getResultOutputFilePath() + ".csv", ',', Charset.forName("GBK"));
            try {
                // 写入csv表头
                String[] headers = {
                        "协议", "ip", "host", "标题",
                        "匹配成功的数据包大小", "原始的数据包大小", "绝对错误的数据包大小"};
                csvWriter.writeRecord(headers);
            } catch (IOException e) {
                System.out.println(" ");
                System.out.println("error: csv文件写入表头出错 :(");
                e.printStackTrace();
                return;
            }
        }

        // 创建txt实例
        if (isOutputTxt) {
            try {
                txtWriter = new BufferedWriter(
                        new FileWriter(CustomHelpers.getResultOutputFilePath() + ".txt"));
            } catch (IOException e) {
                System.out.println(" ");
                System.out.println("error: txt创建失败 :(");
                e.printStackTrace();
                return;
            }
        }

        // 在程序准备退出时执行
        // ps: 就放这里别动,放这里挺好的,环境优美
        Thread t = new Thread(() -> {
            if (isOutputCsv) {
                csvWriter.close();
            }

            if (isOutputTxt) {
                try {
                    txtWriter.close();
                } catch (IOException e) {
                    System.out.println(" ");
                    System.out.println("error: txt文件释放资源出错 :(");
                    e.printStackTrace();
                }
            }
        });
        Runtime.getRuntime().addShutdownHook(t);

        // 建立线程池
        System.out.println("=======================建 立 线 程 池=======================");
        List<Thread> threadPool = new ArrayList<>();
        for (List<String> ipList : getIpChunk()) {
            threadPool.add(
                    new Thread(
                            new HostCollision(
                                    programHelpers,
                                    requestStatistics,
                                    collisionSuccessList,
                                    scanProtocols,
                                    ipList,
                                    getHostList())));
        }

        // 线程启动
        for (int i = 0; i < threadPool.size(); i++) {
            System.out.println(String.format("线程 %s 开始运行", (i + 1)));
            threadPool.get(i).start();
        }

        System.out.println("=======================开 始 碰 撞=======================");

        try {
            // 文件写入的下标
            Integer csvIndex = 0;
            Integer txtIndex = 0;

            // 老的请求数
            // 用来判断是否数据有更新的
            // 如果有就输出一条新的进度条
            Integer oldNumOfRequest = 0;

            // 监控线程/处理数据
            while (true) {
                // 显示当前进度
                if (requestStatistics.getData("numOfRequest") != oldNumOfRequest) {
                    oldNumOfRequest = requestStatistics.getData("numOfRequest");
                    consoleProgressBar.show(requestStatistics.getData("numOfRequest"));
                    System.out.println(" ");
                }

                // csv格式数据的保存
                if (isOutputCsv) {
                    for (int i = csvIndex; i < collisionSuccessList.size(); i++) {
                        csvIndex++;
                        String[] data = {
                                collisionSuccessList.get(i).get(0),
                                collisionSuccessList.get(i).get(1),
                                collisionSuccessList.get(i).get(2),
                                collisionSuccessList.get(i).get(3),
                                collisionSuccessList.get(i).get(4),
                                collisionSuccessList.get(i).get(5),
                                collisionSuccessList.get(i).get(6)};
                        try {
                            csvWriter.writeRecord(data);
                        } catch (IOException e) {
                            System.out.println(" ");
                            System.out.println("error: csv文件写入内容出错 :(");
                            e.printStackTrace();
                            return;
                        }
                    }
                }

                // txt格式数据的保存
                if (isOutputTxt) {
                    try {
                        for (int i = txtIndex; i < collisionSuccessList.size(); i++) {
                            txtIndex++;
                            String data = String.format(
                                    "协议:%s, ip:%s, host:%s, title:%s, 匹配成功的数据包大小:%s 匹配成功 \r\n",
                                    collisionSuccessList.get(i).get(0),
                                    collisionSuccessList.get(i).get(1),
                                    collisionSuccessList.get(i).get(2),
                                    collisionSuccessList.get(i).get(3),
                                    collisionSuccessList.get(i).get(4));
                            txtWriter.write(data);
                        }
                    } catch (IOException e) {
                        System.out.println(" ");
                        System.out.println("error: txt文件写入内容出错 :(");
                        e.printStackTrace();
                        return;
                    }
                }

                // 任务最后面运行完毕时的额外处理
                if (isTaskComplete(threadPool)) {
                    System.out.println(" ");
                    System.out.println(" ");
                    System.out.println(" ");
                    System.out.println(" ");
                    System.out.println(" ");
                    System.out.println("====================碰 撞 成 功 列 表====================");
                    if (collisionSuccessList.size() > 0) {
                        for (List<String> log : collisionSuccessList) {
                            String successLog = String.format(
                                    "协议:%s, ip:%s, host:%s, title:%s, 匹配成功的数据包大小:%s 匹配成功",
                                    log.get(0), log.get(1), log.get(2),
                                    log.get(3), log.get(4));
                            System.out.println(successLog);
                        }
                    } else {
                        System.out.println("没有碰撞成功的数据");
                    }
                    System.out.println("执行完毕 ヾ(≧▽≦*)o");
                    System.out.println(" ");
                    return;
                }

                // 单纯的等待～
                Thread.sleep(500);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取host列表
     *
     * @return List<String>
     */
    private static List<String> getHostList() {
        return CustomHelpers.dataCleaning(CustomHelpers.convertStringToList(hostData, "\n"));
    }

    /**
     * 获取ip列表
     *
     * @return List<String>
     */
    private static List<String> getIpList() {
        return CustomHelpers.dataCleaning(CustomHelpers.convertStringToList(ipData, "\n"));
    }

    /**
     * 获取ip数据分块
     *
     * @return List<List < String>>
     */
    private static List<List<String>> getIpChunk() {
        List<List<String>> ipChunk = CustomHelpers.listChunkSplit(getIpList(), programHelpers.getThreadTotal());
        return ipChunk;
    }

    /**
     * 判断任务是否完成
     *
     * @param threadPool
     * @return Boolean
     */
    private static Boolean isTaskComplete(List<Thread> threadPool) {
        // 开启的线程总数
        Integer threadCcount = threadPool.size();

        // 线程完成数量
        Integer threadNum = 0;

        for (Thread t : threadPool) {
            if (!t.isAlive()) {
                threadNum++;
            }
        }

        if (threadNum.equals(threadCcount)) {
            return true;
        }

        return false;
    }
}