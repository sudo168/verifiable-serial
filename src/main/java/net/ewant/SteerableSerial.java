package net.ewant;

import java.util.Random;
import java.util.stream.IntStream;

public class SteerableSerial {
    private static final String BASE_NUMS = "23456789";
    private static final String BASE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz";

    private final Random random = new Random();
    private int numCount;
    private int codeLength;

    public SteerableSerial(){
        this(4, 10);
    }

    public SteerableSerial(int numCount, int codeLength){
        this.numCount = numCount;
        this.codeLength = codeLength;
    }

    public String getCode(){
        int numIndex = 0;
        char[] buf = new char[codeLength];
        for(int i=0;i<numCount;i++){
            int index = random.nextInt(codeLength);
            while (buf[index] != 0){// 不能重复
                index = random.nextInt(codeLength);
            }
            buf[index] = BASE_NUMS.charAt(index % BASE_NUMS.length());
            numIndex |= 1 << index;
        }
        int charLen = BASE_CHARS.length();
        long maxRang = (long) Math.pow(charLen, codeLength - numCount);
        maxRang = maxRang >> codeLength;
        maxRang &= ~1;// 将指定bit置为0
        long value = (long) (maxRang * random.nextDouble());
        value = value << codeLength;
        value |= numIndex;
        for(int i=0; i<codeLength; i++) {
            if(buf[i] == 0){
                buf[i] = BASE_CHARS.charAt((int) (value % charLen));
                value /= charLen;
            }
        }
        return new String(buf);
    }

    public boolean verify(String code){
        if(code == null || code.length() != codeLength){
            return false;
        }
        long src = 0;
        int numIndex = 0;
        long value = 1;
        int charLen = BASE_CHARS.length();
        for(int i=0; i<codeLength; i++){
            char ch = code.charAt(i);
            if(ch >= 50 && ch <=57 ){//数字2-9
                numIndex |= 1 << i;
            }else{
                src += BASE_CHARS.indexOf(ch) * value;
                value *= charLen;
            }
        }
        return (src & ((1 << codeLength) - 1)) == numIndex;
    }

    public static void main(String[] args) {
        SteerableSerial serial = new SteerableSerial();
        IntStream.range(0, 10).forEach(i->{
            String code = serial.getCode();
            boolean verify = serial.verify(code);
            System.out.println(code + " " + verify);
        });
        /*  输出结果：
            2X4Y67ezRP true
            WU4t678vvz true
            nv4K67DJ2F true
            EG4gE7ib23 true
            2tP56Cz9yG true
            2X4dP7hib3 true
            u3y5ik89kR true
            23M56viHfS true
            k3gpUZ89U3 true
            23DM6zgW2H true
         */
    }
}
