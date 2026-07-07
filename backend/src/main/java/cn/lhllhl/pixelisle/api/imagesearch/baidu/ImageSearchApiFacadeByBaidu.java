package cn.lhllhl.pixelisle.api.imagesearch.baidu;

import cn.lhllhl.pixelisle.api.imagesearch.baidu.model.ImageSearchResult;
import cn.lhllhl.pixelisle.api.imagesearch.baidu.sub.GetImageFirstUrlApi;
import cn.lhllhl.pixelisle.api.imagesearch.baidu.sub.GetImageListApi;
import cn.lhllhl.pixelisle.api.imagesearch.baidu.sub.GetImagePageUrlApi;
import cn.lhllhl.pixelisle.api.imagesearch.ImageSearchApiFacade;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ImageSearchApiFacadeByBaidu implements ImageSearchApiFacade {

    /**
     * 搜索图片
     *
     * @param imageUrl
     * @return
     */
    public  List<ImageSearchResult> searchImage(String imageUrl) {
        String imagePageUrl = GetImagePageUrlApi.getImagePageUrl(imageUrl);
        String imageFirstUrl = GetImageFirstUrlApi.getImageFirstUrl(imagePageUrl);
        List<ImageSearchResult> imageList = GetImageListApi.getImageList(imageFirstUrl);
        return imageList;
    }

//    public static void main(String[] args) {
//        // 测试以图搜图功能
//        String imageUrl = "https://seopic.699pic.com/photo/50085/2259.jpg_wh1200.jpg";
//        List<ImageSearchResult> resultList = searchImage(imageUrl);
//        System.out.println("结果列表" + resultList);
//    }
}
