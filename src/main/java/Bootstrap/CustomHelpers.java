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
