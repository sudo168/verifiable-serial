package net.ewant;

import java.util.stream.IntStream;

/**
 * 最高位符号位不用
 * 时间戳占41位
 * 剩下22位分别给：数据中心 + 机器 + 序列号
 * 1 00000000000000000000000000000000000000000 00000 00000 000000000000
 *
 * 默认的雪花算法是41 + 22，可以根据自己的需求进行调整。
 * 时间戳位数决定使用年限，
 * 数据中心和机器ID位数决定横向扩展性，
 * 序列号位数决定适用性能（TPS）
 */
public class SnowFlake {
    // 起始的时间戳
    private final static long START_TIMESTAMP = 1586765555888L;
    // 每一部分占用的位数
    private int SEQUENCE_BIT;// 序列号占用的位数
    private int MACHINE_BIT; // 机器标识占用的位数
    private int DATA_CENTER_BIT;// 数据中心占用的位数
    // 每一部分最大值
    private long MAX_DATA_CENTER;
    private long MAX_MACHINE;
    private long MAX_SEQUENCE;
    // 每一部分向左的位移
    private int MACHINE_LEFT_SHIFT;
    private int DATA_CENTER_LEFT_SHIFT;
    private int TIMESTAMP_LEFT_SHIFT;

    private long dataCenterId; // 数据中心
    private long machineId; // 机器标识
    private long sequence = 0L; // 序列号
    private long lastTimestamp = -1L; // 上一次时间戳

    private long minStep;

    public SnowFlake(long dataCenterId, long machineId) {
        this(dataCenterId, 4, machineId, 4);
    }

    public SnowFlake(long dataCenterId, int dataCenterBits, long machineId, int machineBits) {
        if(dataCenterBits < 0 || (dataCenterBits == 0 && dataCenterId >= 0)) throw new IllegalArgumentException("Invalid 'dataCenterId' and 'dataCenterBits' setting.");
        if(machineBits < 0 || (machineBits == 0 && dataCenterId >= 0)) throw new IllegalArgumentException("Invalid 'machineId' and 'machineBits' setting.");
        if(dataCenterId >= 0) DATA_CENTER_BIT = dataCenterBits;
        if(machineId >= 0) MACHINE_BIT = machineBits;
        // SEQUENCE_BIT做如下限制以后，建议DATA_CENTER_BIT + MACHINE_BIT <= 8
        SEQUENCE_BIT = Math.min(22 - DATA_CENTER_BIT - MACHINE_BIT, 10);//最大10位够用了（100w/s TPS）。不要怀疑，这里是min函数没错
        if(SEQUENCE_BIT < 7){// 最小7位，保证同一毫秒有足够的空间（10w/s TPS）
            throw new IllegalArgumentException("Invalid SEQUENCE_BIT size: " + SEQUENCE_BIT);
        }
        // 每一部分最大值
        MAX_DATA_CENTER = -1L ^ (-1L << DATA_CENTER_BIT);
        MAX_MACHINE = -1L ^ (-1L << MACHINE_BIT);
        MAX_SEQUENCE = -1L ^ (-1L << SEQUENCE_BIT);
        // 每一部分向左的位移
        MACHINE_LEFT_SHIFT = SEQUENCE_BIT;
        DATA_CENTER_LEFT_SHIFT = SEQUENCE_BIT + MACHINE_BIT;
        TIMESTAMP_LEFT_SHIFT = DATA_CENTER_LEFT_SHIFT + DATA_CENTER_BIT;

        if (dataCenterId > MAX_DATA_CENTER) {
            throw new IllegalArgumentException("Argument 'dataCenterId' can't be greater than MAX_DATA_CENTER[" + MAX_DATA_CENTER + "]");
        }
        if (machineId > MAX_MACHINE) {
            throw new IllegalArgumentException("Argument 'machineId' can't be greater than MAX_MACHINE[" + MAX_MACHINE + "]");
        }
        this.dataCenterId = dataCenterId < 0 ? 0 : dataCenterId;
        this.machineId = machineId < 0 ? 0 : machineId;
    }

    /**
     * 注意：为保证性能这里不使用锁同步
     * @return
     */
    public long nextId() {
        long currentTime = System.currentTimeMillis();
        if (currentTime < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards.  Refusing to generate id");
        }

        if (currentTime == lastTimestamp) {
            //if条件里表示当前调用和上一次调用落在了相同毫秒内，只能通过第三部分，序列号自增来判断为唯一，所以+1.
            sequence = (sequence + 1) & MAX_SEQUENCE;
            //同一毫秒的序列数已经达到最大，只能等待下一个毫秒
            if (sequence == 0L) {
                currentTime = getNextMill();
            }
        } else {
            //不同毫秒内，序列号置为0
            //执行到这个分支的前提是currTimestamp > lastTimestamp，说明本次调用跟上次调用对比，已经不再同一个毫秒内了，这个时候序号可以重新回置0了。
            sequence = 0L;
        }

        lastTimestamp = currentTime;
        //用相对毫秒数、数据中心、机器ID和自增序号拼接
        return (currentTime - START_TIMESTAMP + minStep) << TIMESTAMP_LEFT_SHIFT //时间戳部分
                | dataCenterId << DATA_CENTER_LEFT_SHIFT               //数据中心部分
                | machineId << MACHINE_LEFT_SHIFT                      //机器标识部分
                | sequence;                                            //序列号部分
    }

    /**
     * 解析ID构成
     * @param id
     * @return 时间戳毫秒数、数据中心、机器ID、序号
     */
    public long[] parseIdComposition(long id){
        long seqNum = ((1L << SEQUENCE_BIT) - 1) & id;
        long machine = ((1L << MACHINE_BIT) - 1) & (id >> MACHINE_LEFT_SHIFT);
        long dataCenter = ((1L << DATA_CENTER_BIT) - 1) & (id >> DATA_CENTER_LEFT_SHIFT);
        long timestamp = (id >> TIMESTAMP_LEFT_SHIFT)  + START_TIMESTAMP - minStep;
        return new long[]{timestamp, dataCenter, machine, seqNum};
    }

    private long getNextMill() {
        long mill = System.currentTimeMillis();
        while (mill <= lastTimestamp) {
            mill = System.currentTimeMillis();
        }
        return mill;
    }

    public void setMinStep(long minStep) {
        if(minStep < 0){
            throw new IllegalArgumentException("Invalid min time step: " + minStep);
        }
        this.minStep = (minStep >> TIMESTAMP_LEFT_SHIFT);
    }

    public static void main(String[] args) {
        SnowFlake snowFlake = new SnowFlake(0, 0);
        System.out.println(System.currentTimeMillis());
        IntStream.range(0, 10).forEach(i->{
            long l = snowFlake.nextId();
            String encode = Base62.encode(l);
            System.out.println(l + "->" + encode + "->" + Base62.decodeToLong(encode));
        });
        calcUsable(snowFlake, 62, 10);
        calcUsable(snowFlake, 32, 12);
        /**
         * ==>时间位占用42bits，低位占用18bits的前提下:
         * 采用雪花算法与Base62生成最多10位字符，最大使用年限：101.52438470376713
         * 采用雪花算法与Base62生成恒定10位字符，最大使用年限：99.8868946279173
         * 采用雪花算法与Base32生成最多12位字符，最大使用年限：139.4611400019977
         * 采用雪花算法与Base32生成恒定12位字符，最大使用年限：135.1029793769343
         *
         * 时间位数减1，以上使用年限将减半
         * 要想增加使用年限，使TIMESTAMP_LEFT_SHIFT变小即可
         * 但是TIMESTAMP_LEFT_SHIFT变小，可扩展性、适用性能也随之减少。。。需要综合考虑使用！
         *
         * 42 + 18 = 60，高位还有3位空闲，可考虑移到低位使用，在保证使用年限的同时，增加可扩展性、适用性能
         */
    }

    private static void calcUsable(SnowFlake snowFlake, int baseChars, int maxChars) {
        int maxBits = Long.toBinaryString((long) Math.pow(baseChars, maxChars) - 1).length();
        int leftShift = snowFlake.TIMESTAMP_LEFT_SHIFT;
        System.out.println();
        System.out.println("==>时间位占用"+(maxBits - leftShift)+"bits，低位占用"+leftShift+"bits的前提下:");
        long timeMax = ((long) Math.pow(baseChars, maxChars) - 1 ) >> leftShift;
        double maxYears = timeMax / (365.00 * 24 * 3600 * 1000);
        System.out.println("采用雪花算法与Base"+baseChars+"生成最多" + maxChars + "位字符，最大使用年限：" + maxYears);

        timeMax = ((long) Math.pow(baseChars, maxChars) - 1  - ((long) Math.pow(baseChars, maxChars-1))) >> leftShift;
        maxYears = timeMax / (365.00 * 24 * 3600 * 1000);
        System.out.println("采用雪花算法与Base"+baseChars+"生成恒定" + maxChars + "位字符，最大使用年限：" + maxYears);

        System.out.println("            最大值：" + ((long) Math.pow(baseChars, maxChars) - 1) + "（公式：Math.pow("+baseChars+", "+maxChars+") - 1）");
        System.out.println("       最大值二进制：" + Long.toBinaryString((long) Math.pow(baseChars, maxChars) - 1));
        System.out.println("最大值二进制有效占位：" + maxBits);
        System.out.println("       高位空余位数：" + (Long.toBinaryString((long) Math.pow(2, 63) - 1).length() - Long.toBinaryString((long) Math.pow(baseChars, maxChars) - 1).length()));
    }
}
