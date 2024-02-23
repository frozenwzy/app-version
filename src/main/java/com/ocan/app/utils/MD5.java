
package com.ocan.app.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MD5加密
 */
public class MD5 {

    private static final Logger LOG = LoggerFactory.getLogger(MD5.class);

    private static final String[] HEXDIGITS = { "0", "1", "2", "3", "4", "5",
            "6", "7", "8", "9", "a", "b", "c", "d", "e", "f" };

    /*
        作用：将字节数组转换为十六进制字符串。
        参数：b - 要转换的字节数组。
        返回值：转换后的十六进制字符串。
     */
    public static String byteArrayToHexString(byte[] b) {
        StringBuffer resultSb = new StringBuffer();
        for (int i = 0; i < b.length; i++){
            resultSb.append(byteToHexString(b[i]));
        }
        return resultSb.toString();
    }

    /*
        作用：将字节转换为十六进制字符串。
        参数：b - 要转换的字节。
        返回值：转换后的十六进制字符串。
     */
    private static String byteToHexString(byte b) {
        int n = b;
        if (n < 0) {
            n += 256;
        }
        int d1 = n / 16;
        int d2 = n % 16;
        return HEXDIGITS[d1] + HEXDIGITS[d2];
    }

    /*
        作用：对指定字符串进行MD5加密。
        参数：origin - 要加密的字符串；charsetname - 字符集的名称。
        返回值：经过MD5加密处理后的字符串。
     */
    public static String md5Encode(String origin, String charsetname) {
        String resultString = null;
        try {
            resultString = new String(origin);
            MessageDigest md = MessageDigest.getInstance("MD5");
            if (charsetname == null || "".equals(charsetname)) {
                resultString = byteArrayToHexString(md.digest(resultString.getBytes()));
            } else {
                resultString = byteArrayToHexString(md.digest(resultString.getBytes(charsetname)));
            }
        } catch (Exception exception) {
        }
        return resultString;
    }


   /*
        作用：对指定字符串进行MD5加密。
        参数：str - 要加密的字符串。
        返回值：经过MD5加密处理后的字符串。
    */
    public static String stringMD5(String str) {
        if (str == null || str.length() == 0) {
            throw new IllegalArgumentException("String to encript cannot be null or zero length");
        }
        StringBuilder hexString = new StringBuilder();
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(str.getBytes());
            byte[] hash = md.digest();
            for (byte aHash : hash) {
                if ((0xff & aHash) < 0x10) {
                    hexString.append("0");
                    hexString.append(Integer.toHexString((0xFF & aHash)));
                } else {
                    hexString.append(Integer.toHexString(0xFF & aHash));
                }
            }
        } catch (NoSuchAlgorithmException e) {
            LOG.error("EXP", e);
        }
        return hexString.toString();
    }

    /*
        作用：对指定字节数组进行MD5加密。
        参数：data - 要加密的字节数组。
        返回值：经过MD5加密处理后的字符串。
     */
    public static String bufferMD5(byte[] data) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("String to encript cannot be null or zero length");
        }
        StringBuilder hexString = new StringBuilder();
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(data);
            byte[] hash = md.digest();
            for (byte aHash : hash) {
                if ((0xff & aHash) < 0x10) {
                    hexString.append("0");
                    hexString.append(Integer.toHexString((0xFF & aHash)));
                } else {
                    hexString.append(Integer.toHexString(0xFF & aHash));
                }
            }
        } catch (NoSuchAlgorithmException e) {
            LOG.error("EXP", e);
        }
        return hexString.toString();
    }
}
