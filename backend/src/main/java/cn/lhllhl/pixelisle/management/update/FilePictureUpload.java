package cn.lhllhl.pixelisle.management.update;

import cn.lhllhl.pixelisle.exception.BusinessException;
import cn.lhllhl.pixelisle.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

/**
 * 文件图片上传
 */
@Service
public class FilePictureUpload extends PictureUploadTemplate {


    @Override
    void processFile(Object inputSource, File file) {

        valid(inputSource);




        MultipartFile multipartFile = (MultipartFile) inputSource;

        try {
            multipartFile.transferTo(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    String getOriginFileName(Object inputSource) {

        valid(inputSource);


        MultipartFile multipartFile = (MultipartFile) inputSource;

        String originalFilename = multipartFile.getOriginalFilename();


        return originalFilename;
    }

    @Override
    String validPicture(Object inputSource) {

        valid(inputSource);

        MultipartFile file = (MultipartFile) inputSource;

        if (file == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件不能为空");
        }

        long size = file.getSize();

        final long MAX_SIZE = 15L * 1024 * 1024;

        if (size >= MAX_SIZE) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小不能超过15M");

        }

        ArrayList<String> list = new ArrayList<>();

        Collections.addAll(list, "jpeg", "jpg", "png", "webp", "bmp");

        String[] split = file.getOriginalFilename().split("\\.");
        if(split.length<=1){

            throw  new BusinessException(ErrorCode.PARAMS_ERROR,"不支持的文件类型");

        }

        String end =  split[1];
        if (!list.contains(end)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片形式不允许");

        }


        return null;





    }

    void valid(Object inputSource) {

        if (!(inputSource instanceof MultipartFile)) {

            throw new BusinessException(ErrorCode.PARAMS_ERROR,"类型不是multipartFile");
        }

    }
}
