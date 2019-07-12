package com.dodoca.utils;

import com.sun.org.apache.xerces.internal.impl.dv.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * @description: 解析cookie
 * @author: TianGuangHui
 * @create: 2019-07-11 10:59
 **/
public class AESUtil {

    /**
     * 解密session信息
     * @param src
     * @param iv
     * @return
     * @throws Exception
     */
    public static String decrypt(String src, String iv, String encryptionKey) throws Exception {
        //"算法/模式/补码方式"
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        SecretKeySpec secretKeySpec = new SecretKeySpec(encryptionKey.getBytes("UTF-8"), "AES");
        IvParameterSpec ivParameterSpec = new IvParameterSpec(Base64.decode(iv));
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
        return new String(cipher.doFinal(Base64.decode(src)));
    }


}
