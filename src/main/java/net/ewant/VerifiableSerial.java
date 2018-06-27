package net.ewant;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 【高性能（百万/秒），可验证，兑换码、序列号生成器】
 *
 * 生成规则： 标识位（ID位） + 随机数据位 + 校验位
 *
 * 算法规律（成立条件，有意义条件） 公式： 字符对齐位数 * 字符长度 < 64
 * 即总位长（实际位长）不能超出Long类型范围
 *
 * 实际位长 = 字符对齐位数 * 字符长度
 * 随机数据位 = 实际位长 - ID位长 - 校验位长
 *
 * 随机数据位的长度越长，重复的概率就越小（随机数据位跟字符长度成正比，跟检验位、标识位成反比）
 * 校验位长度越长，可猜测性越小
 *
 * 当id>0时，生成的兑换码会在前头添加一个描述ID位长度的字符，因此 实际生成的兑换码 = 传入的字符长度 + 1
 * 当ID过大时，就会压缩随机数据位的空间，即生成的不可重复兑换码越少，因此，此时应该增加字符长度，保证足够的随机空间
 *
 * 所以当ID过大无法保证最小随机空间时，系统会自动将字符长度自增+1，因此，此时实际生成的兑换码长度会比传入的长度大
 *
 */
public class VerifiableSerial {

    /**
     * 基准字符串必须保证是2的指数级，当前取2^5，32个字符（顺序不定，保证各个字符唯一即可）
     */
    private static final String BASE_CHARS = "9768ZBTNUFPHVRXMDGQKSCEJWLYA5342";
    /**
     * 用于 位与 运算，映射到BASE_CHARS
     */
    private static int CHAR_AND = BASE_CHARS.length() - 1;
    /**
     * 校验位长度
     */
    private static final int checkBitLength = 3;

    /**
     * 最少随机空间，保证千万级
     */
    private static final int MIN_RAND_RANGE = 0x1FFFFFF;

    /**
     * 字符对齐位数（基准字符串的对数），当2^5，32个字符时，此值为 5
     */
    private static int charBitAlign = (int)(Math.log(BASE_CHARS.length())/Math.log(2));

    /**
     * 生成可验证序列码
     * @param actId 活动ID
     * @param codeLength
     * @return
     */
    public static String create(int actId, int codeLength){
        long sum = 0L;
        // 总位数
        int totalBitLength = charBitAlign * codeLength;
        if(totalBitLength > 64){
            throw new IllegalArgumentException("The argument [codeLength] must less than 13 , but is " + codeLength + ". You need to set a smaller one or turn down MIN_RAND_RANGE setting.");
        }
        // id 位数
        int idBitLength = 0;
        String idLengthFlag = "";
        if(actId > 0){
            idBitLength = Integer.toBinaryString(actId).length();
            sum += (long)actId << (totalBitLength - idBitLength);      //高位标志位
            idLengthFlag = String.valueOf(BASE_CHARS.charAt(idBitLength & CHAR_AND));
        }
        // 数据位数
        int randBitLength = totalBitLength - idBitLength - checkBitLength;
        // 保证足够大的随机空间
        if((1L << randBitLength) < MIN_RAND_RANGE){
            return create(actId, ++codeLength);
        }
        // 随机数据
        long randData = (long)((1L << randBitLength) * Math.random());
        //System.out.println(Long.toBinaryString(randData));
        sum += randData << checkBitLength;                            // 中位数据位
        // 校验和
        long checkNum = (sum >> checkBitLength) % ((1 << checkBitLength) - 1);
        sum += checkNum;                                              // 低位校验位

        // 将结果按对齐位映射到基准字符表
        return idLengthFlag + serialToCode(sum, codeLength);
    }

    private static String serialToCode(long serial, int codeLength){
        StringBuffer codeSerial = new StringBuffer();
        long tmpValue = serial;
        for (int i = 0; i < codeLength; i++) {
            int code = (int) (tmpValue & CHAR_AND);
            codeSerial.append(BASE_CHARS.charAt(code));
            tmpValue = tmpValue >> charBitAlign;
        }
        return codeSerial.reverse().toString();// 低位反转(如不反转，解码时需倒着解)
    }

    public static boolean verify(String code, boolean hasId){
        long sum = getSerial(code, hasId);
        return verify(sum);
    }

    private static boolean verify(long sum){
        if(sum < 0){
            return false;
        }
        int checkModData = (1 << checkBitLength) - 1;

        long data = sum >> checkBitLength;
        long checkNum = sum & checkModData; // 截取后面几位校验码

        if (data % checkModData == checkNum) {// 判断截取的检验码跟计算出的校验码是否一致
            return true;
        }

        return false;
    }

    private static long getSerial(String code, boolean hasId){
        long sum = 0;
        int baseLen = BASE_CHARS.length();
        int codeLength = code.length();
        int startIndex = hasId ? 1 : 0;
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

    /**
     * 通过兑换码获取活动ID
     * @param code
     * @return
     */
    public static int getActId(String code){
        long sum = getSerial(code, true);
        if(verify(sum)){
            int idBitLength = BASE_CHARS.indexOf(code.charAt(0));
            int totalBitLength = charBitAlign * (code.length() - 1);
            int randBitLength = totalBitLength - idBitLength - checkBitLength;
            return (int) (sum >> randBitLength + checkBitLength);
        }
        return -1;
    }

    /**
     * 按要求生成不重复兑换码
     * @param historyCodes 历史生成
     * @param number 当前需要生成多少个
     * @param codeLen 单个兑换码长度
     * @param actId 活动ID
     * @return
     */
    public static Set<String> generateCodes(Set<String> historyCodes, int number, int codeLen, int actId){
        Set<String> generatedCodes = new HashSet<>(number * 4 / 3 + 1);// number*4/3+1 避免扩容带来性能消耗
        while(generatedCodes.size() < number){
            String code = create(actId, codeLen);
            if(historyCodes == null || !historyCodes.contains(code)){
                generatedCodes.add(code);
            }
        }
        return generatedCodes;
    }

    /**
     * 按要求生成不重复兑换码（不加活动ID前缀）
     * @param historyCodes 历史生成
     * @param number 当前需要生成多少个
     * @param codeLen 单个兑换码长度
     * @return
     */
    public static Set<String> generateCodes(Set<String> historyCodes, int number, int codeLen){
        return generateCodes(historyCodes, number, codeLen, 0);
    }

    public static void main(String[] args) throws Exception{
        System.out.println(BASE_CHARS.length());
        long start = System.currentTimeMillis();

        int codeLength = 9;
        int actId = 0;

        // 验证重复率
        int repeat = 0;
        int totalx = 0;
        int totaly = 10000;
        Map<String, String> map = new HashMap<>();
        for (int j = 0; j < totalx; j++) {
            for (int i = 0; i < totaly; i++) {
                String code = create(actId, codeLength);
                if(map.containsKey(code)){
                    repeat++;
                    continue;
                }
                map.put(code, code);
                //System.out.println(code);
            }
        }

        // 测试批量生产不重复兑换码
        Set<String> codes = generateCodes(null, 1000000, codeLength, actId);

        System.out.println("生成兑换码"+(totalx*totaly)+"个，重复"+repeat+"个，重复率"+(repeat*1.0D/(totalx*totaly))+"耗时： "+(System.currentTimeMillis()-start)+"ms");

        String code = create(actId, codeLength);
        System.out.println("code: " + code);
        System.out.println("verify: " + verify(code, actId > 0));
        System.out.println("actId: " + getActId(code));

        System.out.println(verify("WSMHDCWH6WYQ", actId > 0));
    }
}
