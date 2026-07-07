package cn.lhllhl.pixelisle.management.update;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.lhllhl.pixelisle.config.CosClientConfig;
import cn.lhllhl.pixelisle.exception.BusinessException;
import cn.lhllhl.pixelisle.exception.ErrorCode;
import cn.lhllhl.pixelisle.management.CosManger;
import cn.lhllhl.pixelisle.model.dto.picture.UploadPictureResult;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;


@Slf4j
public abstract class PictureUploadTemplate {

    @Resource
    CosClientConfig cosClientConfig;

    @Resource
    CosManger file;



    /// 进行图片上传

    public UploadPictureResult uploadPictureByUrl(Object inputSource, String uploadPathPrefix) {

        ///参数校验

        String post =validPicture(inputSource);

        /// 图片名字的预处理
        String uuid = RandomUtil.randomString(16);
        String originalFilename =getOriginFileName(inputSource);

//        if(originalFilename.indexOf(".")==originalFilename.length()-1 && StringUtils.hasText(post)){
//            originalFilename +=  post;
//        }



        DateFormat dataFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date data = new Date();

        String format1 = dataFormat.format(data);


        String updateFilename=null;

        if(StringUtils.hasText(post) && post.contains("/")){
            String[] split = post.split("/");

            String suffix = split[split.length - 1];

            updateFilename = String.format("%s_%s.%s", format1, uuid, suffix);
        }else{

            updateFilename = String.format("%s_%s.%s", format1, uuid, FileUtil.getSuffix(originalFilename));

        }




        String uploadPath = String.format("/%s/%s", uploadPathPrefix, updateFilename);//拼接对象键
        File file = null;
        try {
            /// 上传文件
            file = File.createTempFile(uploadPath, null);
            processFile(inputSource, file);
            //multipartFile.transferTo(file);

            //在此处进行获取信息
            PutObjectResult putObjectResult = this.file.putPictureObject(uploadPath, file);


            List<CIObject> objectList = putObjectResult.getCiUploadResult().getProcessResults().getObjectList();

            if(!CollUtil.isEmpty(objectList)){
                /// 0
                CIObject ciObject = objectList.get(0);
                CIObject ciObject1 =ciObject;


                //没有缩略图的时候用压缩图
                 if(objectList.size()>1){
                    /// 1
                    ciObject1 = objectList.get(1);

                 }


                return buildResultObj(originalFilename, ciObject,ciObject1,putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo());

            }

            log.error("图片处理集为空，疑似处理失败");
            return buildResultObj(putObjectResult, uploadPath, originalFilename, file);


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



    /**
     * 对转换后的图片的处理过程
     * @param originalFilename
     * @param ciObject
     * @return
     */
    private UploadPictureResult buildResultObj(String originalFilename, CIObject ciObject,CIObject ciObject1,ImageInfo imageInfo) {
        ///解析封装文件信息



        String format = ciObject.getFormat();
        int with = ciObject.getWidth();
        int height = ciObject.getHeight();

        double v = NumberUtil.round(with * 1.0 / height, 2).doubleValue();


        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        /// 包装压缩后的图片
        uploadPictureResult.setUrl(cosClientConfig.getHost() +"/" + ciObject.getKey());
        uploadPictureResult.setPicName(FileUtil.mainName(originalFilename));
        uploadPictureResult.setPicSize(ciObject.getSize().longValue());
        uploadPictureResult.setPicWidth(with);
        uploadPictureResult.setPicHeight(height);
        uploadPictureResult.setPicScale(v);
        uploadPictureResult.setPicFormat(format);
        uploadPictureResult.setPicColor(imageInfo.getAve());;

        /// 包装缩略图
        uploadPictureResult.setThumbnailUrl(cosClientConfig.getHost() +"/" + ciObject1.getKey());


        return uploadPictureResult;
    }








    /**
     * 封装返回结果
     * @param putObjectResult
     * @param uploadPath
     * @param originalFilename
     * @param file
     * @return
     */

    private UploadPictureResult buildResultObj(PutObjectResult putObjectResult, String uploadPath, String originalFilename, File file) {
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
        uploadPictureResult.setPicColor(imageInfo.getAve());


        return uploadPictureResult;
    }

    /**
     * 上传文件
     * @param inputSource
     * @param file
     */
    abstract void processFile(Object inputSource, File file);
    /**
     *
     * 获取原始名称
     * @param inputSource
     * @return
     */
    abstract  String getOriginFileName(Object inputSource) ;
    /**
     * 校验图片
     * @param inputSource
     */
    abstract String validPicture(Object inputSource);

}
