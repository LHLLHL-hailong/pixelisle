package cn.lhllhl.pixelisle.management;


import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.*;
import cn.lhllhl.pixelisle.config.CosClientConfig;
import cn.lhllhl.pixelisle.exception.BusinessException;
import cn.lhllhl.pixelisle.exception.ErrorCode;
import cn.lhllhl.pixelisle.exception.ThrowUtils;
import cn.lhllhl.pixelisle.model.dto.picture.UploadPictureResult;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;


@Slf4j
@Service
@Deprecated
public class FileManger {

    @Autowired
    CosClientConfig cosClientConfig;

    @Autowired
    CosManger cosManger;


    public UploadPictureResult uploadPicture(MultipartFile multipartFile, String uploadPathPrefix) {

        ///参数校验
        isValid(multipartFile);



        /// 图片名字的预处理
        String uuid = RandomUtil.randomString(16);
        String originalFilename = multipartFile.getOriginalFilename();


        DateFormat dataFormat = new SimpleDateFormat("yyyy-MM-dd");


        Date data = new Date();

        String format1 = dataFormat.format(data);


        String updateFilename = String.format("%s_%s.%s", format1, uuid, FileUtil.getSuffix(originalFilename));

        String uploadPath = String.format("/%s/%s", uploadPathPrefix, updateFilename);


        File file = null;
        try {
            /// 上传文件
            file = File.createTempFile(uploadPath, null);
            multipartFile.transferTo(file);
            PutObjectResult putObjectResult = cosManger.putPictureObject(uploadPath, file);


            ///解析封装文件信息
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();

            String format = imageInfo.getFormat();
            int with = imageInfo.getWidth();
            int height = imageInfo.getHeight();

            double v = NumberUtil.round(with * 1.0 / height, 2).doubleValue();


            UploadPictureResult uploadPictureResult = new UploadPictureResult();

            uploadPictureResult.setUrl(cosClientConfig.getHost()  + uploadPath);
            uploadPictureResult.setPicName(FileUtil.mainName(multipartFile.getOriginalFilename()));
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setPicWidth(with);
            uploadPictureResult.setPicHeight(height);
            uploadPictureResult.setPicScale(v);
            uploadPictureResult.setPicFormat(format);


            return uploadPictureResult;


            /// 返回可访问的地址
            //return ResultUtils.success(uploadPath);
        } catch (Exception e) {
            log.error("file upload error, filepath = " + uploadPath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            if (file != null) {
                // 删除临时文件
                boolean delete = file.delete();
                if (!delete) {
                    log.error("file delete error, filepath = " + uploadPath);
                }
            }
        }


    }


    void isValid(MultipartFile file) {

        if (file == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件不能为空");
        }

        long size = file.getSize();

        int M = 1024 * 1024;

        if (size >= 15 * M) {
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


    }



    ///根据文件url进行校验
    void isValid(String fileUrl){

        if(StrUtil.isEmpty(fileUrl) ||  StrUtil.isBlank(fileUrl)){
            throw  new BusinessException(ErrorCode.PARAMS_ERROR);
        }


        /// 检验url
        ThrowUtils.throwIf(Validator.isUrl(fileUrl) || !(fileUrl.startsWith("http://") || fileUrl.startsWith("https://")),ErrorCode.PARAMS_ERROR,"传入的url格式不正确");


        HttpRequest request = HttpUtil.createRequest(Method.HEAD, fileUrl);
        HttpResponse result =null;

        try {
            result = request.execute();

            if (!(result.getStatus()== HttpStatus.HTTP_OK)) {
                return;
            }

            /// 检验媒体类型
            String header = result.header("Content-Type");

            if (StrUtil.isBlank(header)) {
                return;
            }

            if(!header.contains("image")){
                throw new BusinessException(ErrorCode.PARAMS_ERROR);
            }

            /// 检验大小
            String len = result.header("Content_Length");

            if(StrUtil.isEmpty(len) || StrUtil.isBlank(len)){
                return ;
            }

            Long size=Long.parseLong(len);

            int M = 1024 * 1024;

            if (size >= 15 * M) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小不能超过15M");

            }




        }catch (Exception e){
            result.close();

            throw e;

        }






    }



    /// 进行基于url的图片上传

    public UploadPictureResult uploadPictureByUrl(String fileUrl, String uploadPathPrefix) {

        ///参数校验

        isValid(fileUrl);



        /// 图片名字的预处理
        String uuid = RandomUtil.randomString(16);
        String originalFilename =FileUtil.mainName(fileUrl);


        DateFormat dataFormat = new SimpleDateFormat("yyyy-MM-dd");


        Date data = new Date();

        String format1 = dataFormat.format(data);


        String updateFilename = String.format("%s_%s.%s", format1, uuid, FileUtil.getSuffix(originalFilename));

        String uploadPath = String.format("/%s/%s", uploadPathPrefix, updateFilename);


        File file = null;
        try {
            /// 上传文件
            file = File.createTempFile(uploadPath, null);
            HttpUtil.downloadFile(fileUrl, file);
            //multipartFile.transferTo(file);
            PutObjectResult putObjectResult = cosManger.putPictureObject(uploadPath, file);


            ///解析封装文件信息
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();

            String format = imageInfo.getFormat();
            int with = imageInfo.getWidth();
            int height = imageInfo.getHeight();

            double v = NumberUtil.round(with * 1.0 / height, 2).doubleValue();


            UploadPictureResult uploadPictureResult = new UploadPictureResult();

            uploadPictureResult.setUrl(cosClientConfig.getHost()  + uploadPath);
            uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
            uploadPictureResult.setPicSize(FileUtil.size(file));
            uploadPictureResult.setPicWidth(with);
            uploadPictureResult.setPicHeight(height);
            uploadPictureResult.setPicScale(v);
            uploadPictureResult.setPicFormat(format);


            return uploadPictureResult;


            /// 返回可访问的地址
            //return ResultUtils.success(uploadPath);
        } catch (Exception e) {
            log.error("file upload error, filepath = " + uploadPath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            if (file != null) {
                // 删除临时文件
                boolean delete = file.delete();
                if (!delete) {
                    log.error("file delete error, filepath = " + uploadPath);
                }
            }
        }


    }

}
