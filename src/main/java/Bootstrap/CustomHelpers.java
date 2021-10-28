package Bootstrap;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CustomHelpers {
    /**
     * 判断是否运行在jar环境
     *
     * @return boolean
     */
    public static boolean isStartupFromJar() {
        String protocol = CustomHelpers.class.getResource("CustomHelpers.class").getProtocol();
        boolean runningInJar = "jar".equals(protocol);
        return runningInJar;
    }

    /**
     * 获取资源目录路径
     *
     * @return String
     */
    public static String getResourcePath() {
        String path = System.getProperty("user.dir") + File.separator;
        if (!CustomHelpers.isStartupFromJar()) {
            path = path + "src" + File.separator + "main" + File.separator;
            path = path + "resources";
            return path;
        }
        return path;
    }

    /**
     * 获取随机一个UserAgent
     *
     * @return String
     */
    public static String getRandomUserAgent() {
        String[] userAgents = {
                "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/77.0.3865.90 Safari/537.36",
                "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; AcooBrowser; .NET CLR 1.1.4322; .NET CLR 2.0.50727)",
                "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.0; Acoo Browser; SLCC1; .NET CLR 2.0.50727; Media Center PC 5.0; .NET CLR 3.0.04506)",
                "Mozilla/4.0 (compatible; MSIE 7.0; AOL 9.5; AOLBuild 4337.35; Windows NT 5.1; .NET CLR 1.1.4322; .NET CLR 2.0.50727)",
                "Mozilla/5.0 (Windows; U; MSIE 9.0; Windows NT 9.0; en-US)",
                "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Win64; x64; Trident/5.0; .NET CLR 3.5.30729; .NET CLR 3.0.30729; .NET CLR 2.0.50727; Media Center PC 6.0)",
                "Mozilla/5.0 (compatible; MSIE 8.0; Windows NT 6.0; Trident/4.0; WOW64; Trident/4.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; .NET CLR 1.0.3705; .NET CLR 1.1.4322)",
                "Mozilla/4.0 (compatible; MSIE 7.0b; Windows NT 5.2; .NET CLR 1.1.4322; .NET CLR 2.0.50727; InfoPath.2; .NET CLR 3.0.04506.30)",
                "Mozilla/5.0 (Windows; U; Windows NT 5.1; zh-CN) AppleWebKit/523.15 (KHTML, like Gecko, Safari/419.3) Arora/0.3 (Change: 287 c9dfb30)",
                "Mozilla/5.0 (X11; U; Linux; en-US) AppleWebKit/527+ (KHTML, like Gecko, Safari/419.3) Arora/0.6",
                "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.8.1.2pre) Gecko/20070215 K-Ninja/2.1.1",
                "Mozilla/5.0 (Windows; U; Windows NT 5.1; zh-CN; rv:1.9) Gecko/20080705 Firefox/3.0 Kapiko/3.0",
                "Mozilla/5.0 (X11; Linux i686; U;) Gecko/20070322 Kazehakase/0.4.5",
                "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.8) Gecko Fedora/1.9.0.8-1.fc10 Kazehakase/0.5.6",
                "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.11 (KHTML, like Gecko) Chrome/17.0.963.56 Safari/535.11",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_3) AppleWebKit/535.20 (KHTML, like Gecko) Chrome/19.0.1036.7 Safari/535.20",
                "Opera/9.80 (Macintosh; Intel Mac OS X 10.6.8; U; fr) Presto/2.9.168 Version/11.52",
                "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/536.11 (KHTML, like Gecko) Chrome/20.0.1132.11 TaoBrowser/2.0 Safari/536.11",
                "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.1 (KHTML, like Gecko) Chrome/21.0.1180.71 Safari/537.1 LBBROWSER",
                "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0; .NET4.0C; .NET4.0E; LBBROWSER)",
                "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; QQDownload 732; .NET4.0C; .NET4.0E; LBBROWSER)",
                "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.11 (KHTML, like Gecko) Chrome/17.0.963.84 Safari/535.11 LBBROWSER",
                "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.1; WOW64; Trident/5.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0; .NET4.0C; .NET4.0E)",
                "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0; .NET4.0C; .NET4.0E; QQBrowser/7.0.3698.400)",
                "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; QQDownload 732; .NET4.0C; .NET4.0E)",
                "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1; Trident/4.0; SV1; QQDownload 732; .NET4.0C; .NET4.0E; 360SE)",
                "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; QQDownload 732; .NET4.0C; .NET4.0E)",
                "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.1; WOW64; Trident/5.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0; .NET4.0C; .NET4.0E)",
                "Mozilla/5.0 (Windows NT 5.1) AppleWebKit/537.1 (KHTML, like Gecko) Chrome/21.0.1180.89 Safari/537.1",
                "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.1 (KHTML, like Gecko) Chrome/21.0.1180.89 Safari/537.1",
                "Mozilla/5.0 (iPad; U; CPU OS 4_2_1 like Mac OS X; zh-cn) AppleWebKit/533.17.9 (KHTML, like Gecko) Version/5.0.2 Mobile/8C148 Safari/6533.18.5",
                "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:2.0b13pre) Gecko/20110307 Firefox/4.0b13pre",
                "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:16.0) Gecko/20100101 Firefox/16.0",
                "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.64 Safari/537.11",
                "Mozilla/5.0 (X11; U; Linux x86_64; zh-CN; rv:1.9.2.10) Gecko/20100922 Ubuntu/10.10 (maverick) Firefox/3.6.10"
        };
        List list = new ArrayList(Arrays.asList(userAgents));

        Random random = new Random();
        int n = random.nextInt(userAgents.length);
        return (String) list.get(n);
    }

    /**
     * 随机取若干个字符
     *
     * @param number
     * @return String
     */
    public static String randomStr(int number) {
        StringBuffer s = new StringBuffer();
        char[] stringArray = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i',
                'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u',
                'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6',
                '7', '8', '9'};
        Random random = new Random();
        for (int i = 0; i < number; i++) {
            char num = stringArray[random.nextInt(stringArray.length)];
            s.append(num);
        }
        return s.toString();
    }

    /**
     * 字符串转换为列表
     *
     * @param str  要转换为list的数据
     * @param mark 分隔符
     * @return List<String>
     */
    public static List<String> convertStringToList(String str, String mark) {
        List<String> result = Arrays.asList(str.split(mark));
        return result;
    }

    /**
     * 数据清理
     *
     * @param dataSource 数据源
     * @return List<String>
     */
    public static List<String> dataCleaning(List<String> dataSource) {
        List<String> result = new ArrayList<>();
        for (String d : dataSource) {
            d = d.trim();
            if (d.length() == 0) {
                continue;
            }
            result.add(d);
        }
        return result;
    }

    /**
     * 列表块分割函数
     * 功能: 把列表按照size分割成指定的list快返回
     * 例子1:
     * a = [1, 2, 3, 4, 5, 6, 7, 8, 9]
     * listChunkSplit(a, 2)
     * 返回: [[1, 2, 3, 4, 5], [6, 7, 8, 9]]
     * 例子2:
     * a = [1, 2, 3, 4, 5, 6, 7, 8, 9]
     * listChunkSplit(a, 10)
     * 返回: [[1], [2], [3], [4], [5], [6], [7], [8], [9]]
     *
     * @param dataSource 数据源
     * @param groupSize  一个整数, 规定最多分成几个list
     * @return List<List < String>>
     */
    public static List<List<String>> listChunkSplit(List<String> dataSource, Integer groupSize) {
        List<List<String>> result = new ArrayList<>();

        if (dataSource.size() == 0 || groupSize == 0) {
            return result;
        }

        // 偏移量
        int offset = 0;

        // 计算 商
        int number = dataSource.size() / groupSize;

        // 计算 余数
        int remainder = dataSource.size() % groupSize;

        for (int i = 0; i < groupSize; i++) {
            List<String> value = null;
            if (remainder > 0) {
                value = dataSource.subList(i * number + offset, (i + 1) * number + offset + 1);
                remainder--;
                offset++;
            } else {
                value = dataSource.subList(i * number + offset, (i + 1) * number + offset);
            }

            if (value.size() == 0) {
                break;
            }

            result.add(value);
        }

        return result;
    }

    /**
     * 获取文件数据
     *
     * @param path
     * @return String
     */
    public static String getFileData(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        return new String(bytes);
    }

    /**
     * 获得网页标题
     *
     * @param s
     * @return
     */
    public static String getBodyTitle(String s) {
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

    /**
     * 获取结果输出的文件路径
     *
     * @return
     */
    public static String getResultOutputFilePath() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        // 格式化日期 date: 2021-10-16
        String date = sdf.format(new Date());

        // 生成类似与 ./2021-10-16_a1b2c3d4
        return "." + File.separator + date + "_" + CustomHelpers.randomStr(8);
    }
}
