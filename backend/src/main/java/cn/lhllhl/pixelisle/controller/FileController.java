package cn.lhllhl.pixelisle.controller;

import cn.lhllhl.pixelisle.annotation.AuthCheck;
import cn.lhllhl.pixelisle.common.BaseResponse;
import cn.lhllhl.pixelisle.common.ResultUtils;
import cn.lhllhl.pixelisle.exception.BusinessException;
import cn.lhllhl.pixelisle.exception.ErrorCode;
import cn.lhllhl.pixelisle.management.CosManger;
import cn.lhllhl.pixelisle.model.enums.UserRoleEnum;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.COSObjectInputStream;
import com.qcloud.cos.utils.IOUtils;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

@RestController
@Slf4j
public class FileController {


    @Autowired
    CosManger cosManger;

    /**
     * 测试文件上传
     * @param multipartFile
     * @return
     */
    @ApiOperation("测试文件的上传（废弃）")
    @AuthCheck(UserRoleEnum.ADMIN)
    @PostMapping("/test/upload")
    public BaseResponse<String> testUploadFile(@RequestPart("file") MultipartFile multipartFile) {
        // 文件目录
        String filename = multipartFile.getOriginalFilename();
        String filepath = String.format("/test/%s", filename);
        File file = null;
        try {
            // 上传文件
            file = File.createTempFile(filepath,  null);
            multipartFile.transferTo(file);
            cosManger.putObject(filepath, file);
            // 返回可访问的地址
            return ResultUtils.success(filepath);
        } catch (Exception e) {
            log.error("file upload error, filepath = " + filepath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            if (file != null) {
                // 删除临时文件
                boolean delete = file.delete();
                if (!delete) {
                    log.error("file delete error, filepath = " + filepath);
                }
            }
        }
    }


    /**
     * 测试文件下载
     * @param filepath
     * @param response
     */
    @ApiOperation("测试文件下载（废弃 ）")
    @AuthCheck(UserRoleEnum.ADMIN)
    @GetMapping("/test/download/")
    public void testDownloadFile(String filepath, HttpServletResponse response) {
        COSObject cosObject = cosManger.getObject(filepath);
        try (COSObjectInputStream cosObjectInput = cosObject.getObjectContent()) {
            byte[] bytes = IOUtils.toByteArray(cosObjectInput);
            // 设置响应头
            response.setContentType("application/octet-stream;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment; filename=" + filepath);
            // 写入响应
            response.getOutputStream().write(bytes);
            response.getOutputStream().flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }




//    @AuthCheck(UserRoleEnum.ADMIN)
//    @GetMapping("/test/download/")
//    ResponseEntity<InputStreamResource> testDownloadFile(String filepath) throws IOException {
//        COSObject cosObject = cosManger.getObject(filepath);
//        COSObjectInputStream cosObjectInput = cosObject.getObjectContent();
//
//        InputStreamResource inputStreamReader=null;
//        try {
//
//            inputStreamReader = new InputStreamResource(cosObjectInput);
//
//            String encodedFileName=cosObject.getObjectMetadata().getContentDisposition();
//            ResponseEntity<InputStreamResource> body = ResponseEntity.ok().
//                    header("Content-Disposition", "attachment; filename=\"" + encodedFileName + "\"").
//                    contentType(MediaType.APPLICATION_OCTET_STREAM).
//                    body(inputStreamReader);
//
//            return body;
//
//
////            byte[] bytes = IOUtils.toByteArray(cosObjectInput);
////            // 设置响应头
////            response.setContentType("application/octet-stream;charset=UTF-8");
////            response.setHeader("Content-Disposition", "attachment; filename=" + filepath);
////            // 写入响应
////            response.getOutputStream().write(bytes);
////            response.getOutputStream().flush();
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }finally {
//            //cosObjectInput.close();
//        }
//    }



}
