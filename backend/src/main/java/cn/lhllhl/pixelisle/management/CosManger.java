package cn.lhllhl.pixelisle.management;

import cn.hutool.core.io.FileUtil;
import cn.lhllhl.pixelisle.config.CosClientConfig;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.*;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Component
public class CosManger {


    @Autowired
    CosClientConfig cosClientConfig;

    @Autowired
    COSClient cosClient;

    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        return cosClient.putObject(putObjectRequest);
    }
    /**
     * 下载对象
     *
     * @param key 唯一键
     * @return COSObject（注意使用后需关闭流）
     */
    public COSObject getObject(String key) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
        return cosClient.getObject(getObjectRequest);
    }

    /**
     * 根据对象键删除单独的图片对象
     * @param key
     */
    public void deleteObject(String key){

        DeleteObjectRequest deleteObjectRequest = new DeleteObjectRequest(cosClientConfig.getBucket(), key);
        cosClient.deleteObject(deleteObjectRequest);
    }


    /**
     * 批量删除
     * @param keys
     * @return
     */
    public DeleteObjectsResult deleteObjectsBatch(List<String> keys){
        DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(cosClientConfig.getBucket());

        List<DeleteObjectsRequest.KeyVersion> keyList = new ArrayList<>();

        for (String key : keys) {
            DeleteObjectsRequest.KeyVersion keyVersion = new DeleteObjectsRequest.KeyVersion(key);
            keyList.add(keyVersion);
        }

        DeleteObjectsResult deleteObjectsResult = cosClient.deleteObjects(deleteObjectsRequest);

        return deleteObjectsResult;
    }


    /**
     * 上传图片并对图片进行处理
     *
     * @param key
     * @param file
     * @return
     */
    public PutObjectResult putPictureObject(String key, File file) {

        //fixme
        List<String> processedFileKeys = new ArrayList<>();


        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);

        PicOperations picOperations = new PicOperations();

        /// 返回图片所有信息
        picOperations.setIsPicInfo(1);

        /// 构建新的文件名称
        String main=FileUtil.mainName(key);
        String fileName = main+".webp";

        int index = key.indexOf(main);

        String prefix = key.substring(0, index);


        /// 添加格式转换规则


        /// 压缩
        List<PicOperations.Rule> list = new ArrayList<PicOperations.Rule>();
        PicOperations.Rule compressRule = new PicOperations.Rule();
        compressRule.setFileId("/transfer/compress"+prefix+fileName);  //设置对象键
        compressRule.setRule("imageMogr2/format/webp"); //设置规则
        compressRule.setBucket(cosClientConfig.getBucket());   //设置存储到的桶（通常为原来的桶）
        list.add(compressRule);//0--->压缩规则
        processedFileKeys.add("/transfer/compress"+prefix+fileName); // 记录压缩图Key


        if(file.length()> 20 * 1024){

            /// 缩略
            PicOperations.Rule thumbnailRule = new PicOperations.Rule();
            thumbnailRule.setFileId("/transfer/thumbnail"+prefix+FileUtil.mainName(key)+"_thumbnail.webp");  //设置对象键
            thumbnailRule.setRule(String.format("imageMogr2/thumbnail/%sx%s>/format/webp/quality/90",512,512)); //设置规则
            thumbnailRule.setBucket(cosClientConfig.getBucket());   //设置存储到的桶（通常为原来的桶）
            list.add(thumbnailRule);//1--->缩略图规则
            processedFileKeys.add("/transfer/thumbnail"+prefix+FileUtil.mainName(key)+"_thumbnail.webp"); // 记录缩略图Key




        }




        picOperations.setRules(list);
        putObjectRequest.setPicOperations(picOperations);




        //先进行处理
        PutObjectResult result = cosClient.putObject(putObjectRequest);

        /// 在这里进行转化，删除原图，只留下压缩后的图片

        //再删除原图
        this.deleteObject(key);


        //fixme
        // 步骤4：为处理后的压缩图/缩略图配置浏览器缓存（核心步骤）
        for (String processedKey : processedFileKeys) {
            setCosObjectCacheControl(cosClientConfig.getBucket(), processedKey, 2592000); // 缓存1月
        }


        return result;
    }


    //fixme
    /**
     * 为COS对象设置浏览器缓存头（Cache-Control）
     * @param bucketName 存储桶名称
     * @param objectKey 文件的ObjectKey
     * @param maxAge 缓存时长（秒）：86400=1天，31536000=1年
     */
    private void setCosObjectCacheControl(String bucketName, String objectKey, int maxAge) {
        // 1. 构建拷贝请求（COS修改元数据的标准方式：拷贝自身+替换元数据）
        CopyObjectRequest copyRequest = new CopyObjectRequest(bucketName, objectKey, bucketName, objectKey);

        // 2. 配置缓存头（核心：Cache-Control）
        ObjectMetadata newMetadata = new ObjectMetadata();
        // 关键：设置浏览器缓存规则（public允许CDN/浏览器缓存，max-age设置时长）
        newMetadata.setCacheControl("public, max-age=" + maxAge);
        // 必须设置：替换原有元数据（而非合并）
        newMetadata.setHeader("x-cos-metadata-directive", "REPLACE");

        copyRequest.setNewObjectMetadata(newMetadata);

        // 3. 执行拷贝（修改元数据）
        try {
            cosClient.copyObject(copyRequest);
            System.out.println("缓存头配置成功，文件Key：" + objectKey);
        } catch (Exception e) {
            System.err.println("缓存头配置失败，文件Key：" + objectKey + "，错误：" + e.getMessage());
            // 可添加重试逻辑（比如重试3次）
        }
    }


}
