package net.ewant;

import java.util.stream.IntStream;

/**
 *  Max: 2 ^ 60 - 1
 *  功能：
 *  1、最多支持2048个instance（应用 或者 CPU Cores）
 *  2、单个instance TPS ：100w/s
 *  3、可用139.46年不重复
 */
public class FixedLengthIdGenerator implements IdGenerator{

    /**
     * 基准字符串必须保证是2的指数级，当前取2^5，32个字符（顺序不定，保证各个字符唯一即可）, 去掉容易混淆的数值 0 1 与 字母 I O
     * 6Qab5RcG7dSe3HfT4ghW2jXk9mYn8FpE、6ZBT5NUF7PHV8RXM9DGQ2KSC3EJW4LYA
     * abcdef h j mn   rsty 14
     * ABCDEFGH J MN  QRSTWY 18
     */
    private static final String BASE_CHARS = "6Qab5RcG7dSe3HfT4ghW2jXk9mYn8FpE";
    /**
     * 用于 位与 运算，映射到BASE_CHARS
     */
    private static final int CHAR_AND = BASE_CHARS.length() - 1;

    /**
     * 字符对齐位数（基准字符串的对数），当2^5，32个字符时，此值为 5
     */
    private static final int charBitAlign = (int)(Math.log(BASE_CHARS.length())/Math.log(2));
    /**
     * 有且只有12位是最合适的，多了不支持（溢出）; 少了，在雪花算法下可用年限太短 @see SnowFlake.main()
     */
    private static final int codeLength = 12;

    /**
     * 最多 2 ^ 11 = 2048 个instance
     */
    private static final int machineBits = 11;

    /**
     * 雪花算法实例
     */
    private SnowFlake snowFlake;

    public OrderNoGenerator(int instanceId){
        if(instanceId < 0 || instanceId > ((long)Math.pow(2, machineBits) - 1)){
            throw new IllegalArgumentException("Invalid instance id!");
        }
        this.snowFlake = new SnowFlake(-1, 0, instanceId, machineBits);
    }

    public String nextCode(){
        // 将结果按对齐位映射到基准字符表
        return serialToCode(snowFlake.nextId());
    }

    public long nextSerial(){
        return snowFlake.nextId();
    }

    public String serialToCode(long serial){
        StringBuffer codeSerial = new StringBuffer();
        long tmpValue = serial;
        for (int i = 0; i < codeLength; i++) {
            int code = (int) (tmpValue & CHAR_AND);
            codeSerial.append(BASE_CHARS.charAt(code));
            tmpValue = tmpValue >> charBitAlign;
        }
        return codeSerial.reverse().toString();// 低位反转(如不反转，解码时需倒着解)
    }

    public long getSerial(String code){
        long sum = 0;
        int baseLen = BASE_CHARS.length();
        int codeLength = code.length();
        int startIndex = 0;
        for (int i = startIndex; i < codeLength; i++) {
            long originNum = BASE_CHARS.indexOf(code.charAt(i));
            if (originNum >= baseLen) {
                return -1; // 字符非法
            }
            sum = sum << charBitAlign;
            sum += originNum;
        }
        return sum;
    }

    public static void main(String[] args) {
        FixedLengthIdGenerator orderNoGenerator = new FixedLengthIdGenerator(0);
        IntStream.range(0, 100).forEach(i->{
            String code = orderNoGenerator.nextCode();
            long serial = orderNoGenerator.getSerial(code);
            System.out.println(code + "->" + serial);
        });
    }
}
