package net.ewant;

import java.util.stream.IntStream;

/**
 * 最高位符号位不用
 * 时间戳占41位
 * 剩下22位分别给：数据中心 + 机器 + 序列号
 * 1 00000000000000000000000000000000000000000 00000 00000 000000000000
 */
public class SnowFlake {
    // 起始的时间戳(时间戳占41位)
    private final static long START_TIMESTAMP = 1586765555888L;
    // 每一部分占用的位数，就三个（共22位）
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
    private long lastTimestamp = -1L;// 上一次时间戳

    public SnowFlake(long dataCenterId, long machineId) {
        this(dataCenterId, 5, machineId, 5);
    }

    public SnowFlake(long dataCenterId, int dataCenterBits, long machineId, int machineBits) {
        DATA_CENTER_BIT = dataCenterBits;
        MACHINE_BIT = machineBits;
        SEQUENCE_BIT = Math.min(22 - DATA_CENTER_BIT - MACHINE_BIT, 14);//最大14位够用了，不要怀疑，这里是min函数没错
        if(SEQUENCE_BIT < 8){// 最小8位，保证同一毫秒有足够的空间
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

        if (dataCenterId > MAX_DATA_CENTER || dataCenterId < 0) {
            throw new IllegalArgumentException("Argument 'dataCenterId' can't be greater than MAX_DATA_CENTER[" + MAX_DATA_CENTER + "] or less than 0");
        }
        if (machineId > MAX_MACHINE || machineId < 0) {
            throw new IllegalArgumentException("Argument 'machineId' can't be greater than MAX_MACHINE[" + MAX_MACHINE + "] or less than 0");
        }
        this.dataCenterId = dataCenterId;
        this.machineId = machineId;
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
        //就是用相对毫秒数、机器ID和自增序号拼接
        return (currentTime - START_TIMESTAMP) << TIMESTAMP_LEFT_SHIFT //时间戳部分
                | dataCenterId << DATA_CENTER_LEFT_SHIFT      //数据中心部分
                | machineId << MACHINE_LEFT_SHIFT            //机器标识部分
                | sequence;                            //序列号部分
    }

    private long getNextMill() {
        long mill = System.currentTimeMillis();
        while (mill <= lastTimestamp) {
            mill = System.currentTimeMillis();
        }
        return mill;
    }



    public static void main(String[] args) {
        SnowFlake snowFlake = new SnowFlake(0, 0);
        System.out.println(System.currentTimeMillis());
        IntStream.range(0, 100).forEach(i->{
            long l = snowFlake.nextId();
            System.out.println(l + "->" + Base62.encode(l));
        });

        int maxChars = 10;
        long timeMax = ((long) Math.pow(62, maxChars) - 1) >> snowFlake.TIMESTAMP_LEFT_SHIFT;
        double maxYears = timeMax / (365.00 * 24 * 3600 * 1000);
        System.out.println("采用雪花算法与Base62生成" + maxChars + "位字符，最大使用年限：" + maxYears);
        // 要想增加使用年限，使TIMESTAMP_LEFT_SHIFT变小即可
    }
}
