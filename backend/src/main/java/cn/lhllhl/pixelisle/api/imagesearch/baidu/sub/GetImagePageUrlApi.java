package cn.lhllhl.pixelisle.api.imagesearch.baidu.sub;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * step 01
 * 获取以图片搜图的地址
 */
public class GetImagePageUrlApi {


    public static void main(String[] args) {
        String target="https://seopic.699pic.com/photo/50085/2259.jpg_wh1200.jpg";

        String imagePageUrl = getImagePageUrl(target);

        System.out.println("imagePageUrl = " + imagePageUrl);
    }

    public static String getImagePageUrl(String imageUrl) {
        // tn: pc
        // from: pc
        // image_source: PC_UPLOAD_URL
        // sdkParams:

        // 1. 准备请求参数
        Map<String, Object> formData = new HashMap<>();
        formData.put("image", imageUrl);
        formData.put("tn", "pc");
        formData.put("from", "pc");
        formData.put("image_source", "PC_UPLOAD_URL");
        // 获取当前时间戳
        long uptime = System.currentTimeMillis();
        // 请求地址
        String url = "https://graph.baidu.com/upload?uptime=" + uptime;

        //String acsToken = "jmM4zyI8OUixvSuWh0sCy4xWbsttVMZb9qcRTmn6SuNWg0vCO7N0s6Lffec+IY5yuqHujHmCctF9BVCGYGH0H5SH/H3VPFUl4O4CP1jp8GoAzuslb8kkQQ4a21Tebge8yhviopaiK66K6hNKGPlWt78xyyJxTteFdXYLvoO6raqhz2yNv50vk4/41peIwba4lc0hzoxdHxo3OBerHP2rfHwLWdpjcI9xeu2nJlGPgKB42rYYVW50+AJ3tQEBEROlg/UNLNxY+6200B/s6Ryz+n7xUptHFHi4d8Vp8q7mJ26yms+44i8tyiFluaZAr66/+wW/KMzOhqhXCNgckoGPX1SSYwueWZtllIchRdsvCZQ8tFJymKDjCf3yI/Lw1oig9OKZCAEtiLTeKE9/CY+Crp8DHa8Tpvlk2/i825E3LuTF8EQfzjcGpVnR00Lb4/8A";

        // 2. 发送请求
        HttpResponse httpResponse = HttpRequest.post(url)
                .form(formData)
                .timeout(5000)
                .header("acs-token", RandomUtil.randomString(1))
                .execute();


        if (httpResponse.getStatus()!= HttpStatus.HTTP_OK) {
            throw  new RuntimeException("step 01失败");
        }

        JSONObject entries = new JSONObject(httpResponse.body());

        Integer status = entries.getInt("status");

        Object data = entries.get("data");

        if(!status.equals(0) || data==null){

            System.out.println("entries.toString() = " + entries.toString());

            throw new RuntimeException("step 01 返回值解析异常！！");

        }

        JSONObject target = new JSONObject(data);

        String rawUIrl = target.getStr("url");//这里得到的url要解码

        String searchUrl = URLUtil.decode(rawUIrl, StandardCharsets.UTF_8);

        if(StrUtil.isBlank(searchUrl)){
            throw new RuntimeException("url 解析失败");
        }


        return searchUrl;


    }


    }
