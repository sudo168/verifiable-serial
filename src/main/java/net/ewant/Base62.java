package net.ewant;

public class Base62 {
    //A-Za-z0-9
    private static final byte[] STANDARD_ENCODE_TABLE = new byte[]{
            65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90,
            97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122,
            48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 43, 47};


    public static long decodeToLong(String src) {
        long ret = 0;
        long count = 1;
        long sign = 1;
        if (src.charAt(0) == '-') {
            sign = -1;
            src = src.substring(1);
        }
        for (int i = src.length() - 1; i > -1; i--) {
            char ch = src.charAt(i);
            ret += indexOf(ch) * count;
            count *= 62;
        }
        return sign * ret;
    }

    public static String encode(long src) {
        StringBuffer buff = new StringBuffer();
        char ch = 0;
        if (src < 0) {
            src = -1 * src;
            ch = '-';
        }

        while (src > 0) {
            buff.append((char)STANDARD_ENCODE_TABLE[(int) (src % 62)]);
            src /= 62;
        }
        if (ch > 0) {
            buff.append('-');
        }
        return buff.reverse().toString();
    }

    private static int indexOf(char ch){
        for(int i=0; i<STANDARD_ENCODE_TABLE.length;i++){
            if(ch == STANDARD_ENCODE_TABLE[i]){
                return i;
            }
        }
        return -1;
    }
}
