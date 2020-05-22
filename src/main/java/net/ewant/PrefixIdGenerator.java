package net.ewant;

import java.util.stream.IntStream;

/**
 *  可加一个字符前缀，结果输出固定12位
 *  Max: 2 ^ 60 - 1
 *  功能：
 *  1、最多支持2048个instance（应用 或者 CPU Cores）
 *  2、单个instance TPS ：100w/s
 *  3、可用86.78年不重复，增大USING_CHARS_LENGTH可以增加使用年限（条件：length <= 48）
 */
public class PrefixIdGenerator implements IdGenerator{

    /**
     * base 62
     * 将20个最不想使用的字符放到最后 0124IKMOPZijklmnopwz
     */
    private static final String BASE_CHARS = "8abcdefghJLNQRSTUVWXY35679ABCDEFGHqrstuvxy01234IKMOPZijklmnopwz";

    private static final int USING_CHARS_LENGTH = 42;//结果输出12位，并且包含1个固定字符前缀的情况下，最大USING_CHARS_LENGTH只能使用48

    private static final int codeLength = 11;

    /**
     * 最多 2 ^ 11 = 2048 个instance
     */
    private static final int machineBits = 11;

    private char prefix;

    private byte[] charsTable;

    /**
     * 雪花算法实例
     */
    private SnowFlake snowFlake;

    public PrefixIdGenerator(int instanceId, char prefix){
        if(instanceId < 0 || instanceId > ((long)Math.pow(2, machineBits) - 1)){
            throw new IllegalArgumentException("Invalid instance id!");
        }
        this.prefix = prefix;
        this.snowFlake = new SnowFlake(-1, 0, instanceId, machineBits);
        String replace = BASE_CHARS.replace(String.valueOf(prefix), "");
        charsTable = replace.substring(0, USING_CHARS_LENGTH).getBytes();
    }

    public String nextCode(){
        return encode(snowFlake.nextId());
    }
    public long nextSerial(){
        return snowFlake.nextId();
    }

    public long getSerial(String code){
        return decode(code);
    }

    public String serialToCode(long serial){
        return encode(serial);
    }

    private long decode(String code) {
        long ret = 0;
        long count = 1;
        long sign = 1;
        if (code.charAt(0) == '-') {
            sign = -1;
            code = code.substring(1);
        }
        code = code.substring(1);// ignore prefix
        for (int i = code.length() - 1; i > -1; i--) {
            char ch = code.charAt(i);
            ret += indexOf(ch) * count;
            count *= USING_CHARS_LENGTH;
        }
        return sign * ret;
    }

    private String encode(long serial) {
        StringBuffer buff = new StringBuffer();
        char ch = 0;
        if (serial < 0) {
            serial = -1 * serial;
            ch = '-';
        }
        int index=0;
        while (serial > 0 || index < codeLength) {
            buff.append((char)charsTable[(int) (serial % USING_CHARS_LENGTH)]);
            serial /= USING_CHARS_LENGTH;
            index++;
        }
        buff.append(prefix);
        if (ch > 0) {
            buff.append('-');
        }
        return buff.reverse().toString();
    }

    private int indexOf(char ch){
        for(int i=0; i<charsTable.length;i++){
            if(ch == charsTable[i]){
                return i;
            }
        }
        return -1;
    }

    public static void main(String[] args) {
        PrefixIdGenerator tradeIdGenerator = new PrefixIdGenerator(0, 'T');
        PrefixIdGenerator orderIdGenerator = new PrefixIdGenerator(0, 'O');
        IntStream.range(0, 10).forEach(i->{
            String code = tradeIdGenerator.nextCode();
            long serial = tradeIdGenerator.getSerial(code);
            System.out.println("Trade: " + code + "->" + serial);
        });
        IntStream.range(0, 10).forEach(i->{
            String code = orderIdGenerator.nextCode();
            long serial = orderIdGenerator.getSerial(code);
            System.out.println("Order: " + code + "->" + serial);
        });
    }
}
