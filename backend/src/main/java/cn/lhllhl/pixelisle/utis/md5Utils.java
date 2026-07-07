package cn.lhllhl.pixelisle.utis;


import org.springframework.util.DigestUtils;

public class md5Utils {

    public static final String PREFIX="I see you ";

   public static String get(String password) {

        return DigestUtils.md5DigestAsHex(new StringBuilder().append(PREFIX).append(password).toString().getBytes());


    }





}
