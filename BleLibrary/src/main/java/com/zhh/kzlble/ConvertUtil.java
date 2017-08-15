package com.zhh.kzlble;

/**
 * 该工具类主要是字符串，字节与十六进制的相互转换
 *
 * @author sam
 * @author 1.0
 */
public class ConvertUtil {

    /**
     * 十进制转十六进制字符串
     *
     * @param oct
     * @return
     */
    public static String oct2Hex(int oct) {
        String hex = Integer.toHexString(oct);
        return hex.length() == 1 ? "0" + hex : hex;
    }

    /**
     * 连个字节合并一个字节
     *
     * @param src0
     * @param src1
     * @return
     */
    public static byte uniteBytes(byte src0, byte src1) {
        byte _b0 = Byte.decode("0x" + new String(new byte[]{src0}))
                .byteValue();
        _b0 = (byte) (_b0 << 4);
        byte _b1 = Byte.decode("0x" + new String(new byte[]{src1}))
                .byteValue();
        byte ret = (byte) (_b0 ^ _b1);
        return ret;
    }

    /**
     * 十六进制字符串转换字节数组
     *
     * @param src
     * @return
     */
    public static byte[] HexString2Bytes(String src) {
        if (null == src || 0 == src.length())
            return null;
        src = src.replace(" ", "").toUpperCase();
        byte[] ret = new byte[src.length() / 2];
        byte[] tmp = src.getBytes();
        for (int i = 0; i < (tmp.length / 2); i++)
            ret[i] = uniteBytes(tmp[i * 2], tmp[i * 2 + 1]);
        return ret;
    }

    /**
     * 字节数组转换十六进制字符串
     *
     * @param b
     * @return
     */
    public static String Bytes2HexString(byte[] b) {
        String ret = "";
        for (int i = 0; i < b.length; i++) {
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            ret += hex.toUpperCase();
        }
        return ret;
    }

}