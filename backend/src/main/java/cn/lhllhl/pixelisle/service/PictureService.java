package cn.lhllhl.pixelisle.service;

import cn.lhllhl.pixelisle.api.aliyunai.model.CreateOutPaintingTaskResponse;
import cn.lhllhl.pixelisle.common.DeleteRequest;
import cn.lhllhl.pixelisle.model.dto.picture.*;
import cn.lhllhl.pixelisle.model.entity.Picture;
import cn.lhllhl.pixelisle.model.entity.User;
import cn.lhllhl.pixelisle.model.vo.PictureVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.scheduling.annotation.Async;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @description 针对表【picture】的数据库操作Service
* @createDate 2026-02-28 20:42:54
*/
public interface PictureService extends IService<Picture> {

    void validPicture(Picture picture);

    /**
     * 上传图片
     *
     * @param inputSource
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    PictureVo uploadPicture(Object inputSource,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);

    /**
     * 编辑图片的抽取
     * @param pictureEditRequest
     * @param request
     */
    void editPictureService(PictureEditRequest pictureEditRequest, HttpServletRequest request);

    /**
     * 删除方法的抽取
     * @param deleteRequest
     * @param loginUser
     * @return
     */
    boolean deletePictureService(DeleteRequest deleteRequest, User loginUser);

    /**
     * 普普通通的由DTO获取对应的查询的wrapper
     * @param pictureQueryRequest
     * @return
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);


    /**
     * 获取patrue对象（含有user）(单条)
     * @param picture
     * @param request
     * @return
     */
    PictureVo getPictureVO(Picture picture, HttpServletRequest request);


    /**
     * 获取分页查询Picture(多条)
     * @param picturePage
     * @param request
     * @return
     */
    Page<PictureVo> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);


    /**
     * 执行图片的审核
     *
     * @param pictureReviewRequest
     * @param loginUser
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    void fillReviewParam(Picture picture, User loginUser);


    /**
     * 批量抓取图片和创建图片
     *
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return
     */
    Integer uploadPictureByBath(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser);


    /**
     * 用户查询图片（有缓存5-10min过期）
     * @param pictureQueryRequest
     * @param request
     * @param current
     * @param size
     * @return
     */
    Page<PictureVo> listPictureVoByCache(PictureQueryRequest pictureQueryRequest, HttpServletRequest request, long current, long size);

    /**
     * 清除某个空间的图片列表缓存
     */
    void clearListCache(Long spaceId);


    @Async("clearPictureExecutor")
    void deleteSpaceAndPicture(Long spaceId);

    /**
     * 删除指定的图片
     * @param picture
     */
    void clearPictureFile(Picture picture);


    /**
     * 校验图片的权限
     * @param loginUser
     * @param picture
     */
    // void checkPictureAuth(User loginUser , Picture picture);


    /**
     * 批量更新图片
     *
     * @param pictureEditByBatchRequest
     * @param loginUser
     */
    void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser);

    /**
     * 创建扩图任务
     *
     * @param createPictureOutPaintingTaskRequest
     * @param loginUser
     * @return
     */
    CreateOutPaintingTaskResponse createPictureOutPaintTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser);



    /**
     * 根据颜色搜索图片
     *
     * @param spaceId
     * @param picColor
     * @param loginUser
     * @return
     */
    List<PictureVo> searchPictureByColor(Long spaceId, String picColor, User loginUser);



}
