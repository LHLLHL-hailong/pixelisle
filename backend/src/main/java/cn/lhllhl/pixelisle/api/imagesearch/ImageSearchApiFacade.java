package cn.lhllhl.pixelisle.api.imagesearch;

import cn.lhllhl.pixelisle.api.imagesearch.baidu.model.ImageSearchResult;

import java.util.List;

public interface ImageSearchApiFacade {

    public List<ImageSearchResult> searchImage(String imageUrl) ;
}
