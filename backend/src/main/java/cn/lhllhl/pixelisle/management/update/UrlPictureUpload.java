package cn.lhllhl.pixelisle.management.update;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.*;
import cn.lhllhl.pixelisle.exception.BusinessException;
import cn.lhllhl.pixelisle.exception.ErrorCode;
import cn.lhllhl.pixelisle.exception.ThrowUtils;
import org.springframework.stereotype.Service;

import java.io.File;


/**
 * url文件上传
 */
@Service
public class UrlPictureUpload extends PictureUploadTemplate {
    @Override
    void processFile(Object inputSource, File file) {

        valid(inputSource);

        String fileUrl = (String) inputSource;

        HttpUtil.downloadFile(fileUrl,file);

    }

    @Override
    String getOriginFileName(Object inputSource) {

        valid(inputSource);

        String fileUrl = (String) inputSource;
        String originalFilename = FileUtil.mainName(fileUrl);


        return originalFilename;
    }

    @Override
    String validPicture(Object inputSource) {

        valid(inputSource);

        String fileUrl = (String) inputSource;


        if(StrUtil.isEmpty(fileUrl) ||  StrUtil.isBlank(fileUrl)){
            throw  new BusinessException(ErrorCode.PARAMS_ERROR);
        }


        /// 检验url
        ThrowUtils.throwIf(!Validator.isUrl(fileUrl) || !(fileUrl.startsWith("http://") || fileUrl.startsWith("https://")),ErrorCode.PARAMS_ERROR,"传入的url格式不正确");


        HttpRequest request = HttpUtil.createRequest(Method.HEAD, fileUrl);
        HttpResponse result =null;

        try {
            result = request.execute();

            if (!(result.getStatus()== HttpStatus.HTTP_OK)) {
                return null;
            }

            /// 检验媒体类型
            String header = result.header("Content-Type");

            //System.err.println("header = " + header);

            if (StrUtil.isBlank(header)) {
                return null;
            }

            if(!header.contains("image")){
                throw new BusinessException(ErrorCode.PARAMS_ERROR);
            }

            /// 检验大小
            String len = result.header("Content-Length");

            if(StrUtil.isEmpty(len) || StrUtil.isBlank(len)){
                return header ;
            }

            Long size=Long.parseLong(len);

            int M = 1024 * 1024;

            if (size >= 15 * M) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小不能超过15M");

            }


            return header;


        }catch (Exception e){
            result.close();

            throw e;

        }


    }

    void valid(Object inputSource) {
        if(!(inputSource instanceof String)){
            throw  new BusinessException(ErrorCode.PARAMS_ERROR,"不是String类型");
        }
    }
}
