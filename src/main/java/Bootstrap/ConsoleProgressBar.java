package Bootstrap;

import java.text.DecimalFormat;

/**
 * 控制台字符型进度条
 */
public class ConsoleProgressBar {
    // 进度条-起始值 建议默认为0
    private long minimum = 0;

    // 进度条-最大值 建议默认为100别改
    private long maximum = 100;

    // 进度条-长度
    private long length = 50;

    // 用于进度条显示的字符
    private char showString = '█';

    // 百分比的显示样式
    private DecimalFormat format = new DecimalFormat("#.##%");

    /**
     * 使用系统默认输出, 显示进度条,字符,百分比
     */
    public ConsoleProgressBar() {
    }

    /**
     * 使用系统默认输出, 显示进度条,字符,百分比
     *
     * @param minimum 进度条-起始值
     * @param maximum 进度条-最大值
     */
    public ConsoleProgressBar(long minimum, long maximum) {
        this(minimum, maximum, 50, '█');
    }

    /**
     * 使用系统默认输出, 显示进度条,字符,百分比
     *
     * @param minimum 进度条-起始值
     * @param maximum 进度条-最大值
     * @param length  进度条-长度
     */
    public ConsoleProgressBar(long minimum, long maximum, long length) {
        this(minimum, maximum, length, '█');
    }

    /**
     * 使用系统标准输出，显示字符进度条及其百分比。
     *
     * @param minimum    进度条-起始值
     * @param maximum    进度条-最大值
     * @param length     进度条-长度
     * @param showString 用于进度条显示的字符
     */
    public ConsoleProgressBar(long minimum, long maximum, long length, char showString) {
        this.minimum = minimum;
        this.maximum = maximum;
        this.length = length;
        this.showString = showString;
    }

    /**
     * 显示进度条
     *
     * @param value 当前进度
     */
    public void show(long value) {
        if (value < minimum || value > maximum) {
            return;
        }
        minimum = value;
        reset();
        float rate = (float) (minimum * 1.0 / maximum);
        long len = (long) (rate * length);
        draw(len, rate);
        if (minimum == maximum) {
            afterComplete();
        }
    }

    private void reset() {
        System.out.print("\r");
        System.out.print(String.format("当前进度 %s/%s: ", minimum, maximum));
        System.out.print("[");
    }

    private void draw(long len, float rate) {
        for (int i = 0; i < len; i++) {
            System.out.print(showString);
        }
        System.out.print("]");
        System.out.print(" ");
        System.out.print(format(rate));
    }

    private void afterComplete() {
        System.out.print("\n");
    }

    private String format(float num) {
        return format.format(num);
    }
}