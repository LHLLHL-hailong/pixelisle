package cn.lhllhl.pixelisle.controller;

import cn.hutool.json.JSONUtil;
import cn.lhllhl.pixelisle.annotation.AuthCheck;
import cn.lhllhl.pixelisle.annotation.NoUsed;
import cn.lhllhl.pixelisle.api.aliyunai.AliYunAiApi;
import cn.lhllhl.pixelisle.api.aliyunai.model.CreateOutPaintingTaskResponse;
import cn.lhllhl.pixelisle.api.aliyunai.model.GetOutPaintingTaskResponse;
import cn.lhllhl.pixelisle.api.imagesearch.ImageSearchApiFacade;
import cn.lhllhl.pixelisle.api.imagesearch.baidu.ImageSearchApiFacadeByBaidu;
import cn.lhllhl.pixelisle.api.imagesearch.baidu.model.ImageSearchResult;
import cn.lhllhl.pixelisle.common.BaseResponse;
import cn.lhllhl.pixelisle.common.DeleteRequest;
import cn.lhllhl.pixelisle.common.ResultUtils;
import cn.lhllhl.pixelisle.exception.BusinessException;
import cn.lhllhl.pixelisle.exception.ErrorCode;
import cn.lhllhl.pixelisle.exception.ThrowUtils;
import cn.lhllhl.pixelisle.management.auth.SpaceUserAuthManger;
import cn.lhllhl.pixelisle.management.auth.StpKit;
import cn.lhllhl.pixelisle.management.auth.annotation.SaSpaceCheckPermission;
import cn.lhllhl.pixelisle.management.auth.model.SpaceUserPermissionConstant;
import cn.lhllhl.pixelisle.model.dto.picture.*;
import cn.lhllhl.pixelisle.model.entity.Picture;
import cn.lhllhl.pixelisle.model.entity.Space;
import cn.lhllhl.pixelisle.model.entity.User;
import cn.lhllhl.pixelisle.model.enums.PictureReviewStatusEnum;
import cn.lhllhl.pixelisle.model.enums.UserRoleEnum;
import cn.lhllhl.pixelisle.model.vo.PictureTagCategory;
import cn.lhllhl.pixelisle.model.vo.PictureVo;
import cn.lhllhl.pixelisle.service.PictureService;
import cn.lhllhl.pixelisle.service.SpaceService;
import cn.lhllhl.pixelisle.service.UserService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("/picture")
@Api("正式的文件上传和处理的接口")
public class PictureController {


    @Autowired
    private UserService userService;

    @Autowired
    private PictureService pictureService;

    @Autowired
    private SpaceService spaceService;

    @Autowired
    private AliYunAiApi aliYunAiApi;
    @Autowired
    private SpaceUserAuthManger spaceUserAuthManger;


//    private final Cache<String,String> LOCAL_CACHE= Caffeine.newBuilder().
//            initialCapacity(1024).maximumSize(10000L).
//            expireAfterWrite(Duration.ofMinutes(5)).build();


    /**
     * 文件上传传图
     * @param multipartFile
     * @param pictureUploadRequest
     * @param request
     * @return
     */
    @SaSpaceCheckPermission(value= SpaceUserPermissionConstant.PICTURE_UPLOAD)
    @PostMapping("/upload")
    @ApiOperation("正式的文件上传(文件上传)")
    public BaseResponse<PictureVo>  uploadPicture(@RequestPart("file") MultipartFile multipartFile,
                                                  PictureUploadRequest pictureUploadRequest,
                                                  HttpServletRequest request){


        User loginUser = userService.getLoginUser(request);
        PictureVo pictureVo = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        Long uploadSpaceId = pictureUploadRequest.getSpaceId();
        if (uploadSpaceId != null && uploadSpaceId != 0L) {
            pictureService.clearListCache(uploadSpaceId);
        }

        return ResultUtils.success(pictureVo);


    }

    /**
     * url传图
     * @param pictureUploadRequest
     * @param request
     * @return
     */

    @PostMapping("/upload/url")
    @ApiOperation("正式的文件上传(url)")
    @SaSpaceCheckPermission(value= SpaceUserPermissionConstant.PICTURE_UPLOAD)
    public BaseResponse<PictureVo>  uploadPictureByUrl(
                                                  @RequestBody PictureUploadRequest pictureUploadRequest,
                                                  HttpServletRequest request){


        User loginUser = userService.getLoginUser(request);
        String fileUrl = pictureUploadRequest.getFileUrl();
        PictureVo pictureVo = pictureService.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
        Long urlUploadSpaceId = pictureUploadRequest.getSpaceId();
        if (urlUploadSpaceId != null && urlUploadSpaceId != 0L) {
            pictureService.clearListCache(urlUploadSpaceId);
        }

        return ResultUtils.success(pictureVo);


    }




    @PostMapping("/delete")
    @SaSpaceCheckPermission(value= SpaceUserPermissionConstant.PICTURE_DELETE)
    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest,
                                               HttpServletRequest request){

        if(deleteRequest==null || deleteRequest.getId()==null){

            throw new BusinessException(ErrorCode.PARAMS_ERROR,"删除的id值不能为空");
        }

        User loginUser = userService.getLoginUser(request);

        ThrowUtils.throwIf(loginUser==null || loginUser.getId()==null,ErrorCode.NOT_LOGIN_ERROR);

        Long userId= loginUser.getId();

        // 查 spaceId 用于清缓存
        Picture picture = pictureService.getById(deleteRequest.getId());
        Long delSpaceId = (picture != null) ? picture.getSpaceId() : null;

        boolean b = pictureService.deletePictureService(deleteRequest, loginUser);

        if (b && delSpaceId != null && delSpaceId != 0L) {
            pictureService.clearListCache(delSpaceId);
        }

        return ResultUtils.success(b);


    }



//    /**
//     * 删除图片
//     */
//    @PostMapping("/delete")
//    public BaseResponse<Boolean> deletePicture(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
//        if (deleteRequest == null || deleteRequest.getId() <= 0) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        User loginUser = userService.getLoginUser(request);
//        long id = deleteRequest.getId();
//        // 判断是否存在
//        Picture oldPicture = pictureService.getById(id);
//        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
//        // 仅本人或管理员可删除
//        if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
//            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
//        }
//        // 操作数据库
//        boolean result = pictureService.removeById(id);
//        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
//        return ResultUtils.success(true);
//    }

    /**
     * 更新图片（仅管理员可用）
     */
    @NoUsed
    @PostMapping("/update")
    @AuthCheck(UserRoleEnum.ADMIN)
    public BaseResponse<Boolean> updatePicture(@RequestBody PictureUpdateRequest pictureUpdateRequest, HttpServletRequest request) {
        if (pictureUpdateRequest == null || pictureUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 将实体类和 DTO 进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureUpdateRequest, picture);
        // 注意将 list 转为 string
        picture.setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));
        // 数据校验
        pictureService.validPicture(picture);
        // 判断是否存在
        long id = pictureUpdateRequest.getId();
        Picture oldPicture = pictureService.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);


        /// 刷新审核状态、
        pictureService.fillReviewParam(oldPicture, userService.getLoginUser(request));

        // 操作数据库
        boolean result = pictureService.updateById(picture);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取图片（仅管理员可用）
     */
    @NoUsed
    @GetMapping("/get")
    @AuthCheck(UserRoleEnum.ADMIN)
    public BaseResponse<Picture> getPictureById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(picture);
    }

    /**
     * 根据 id 获取图片（封装类）
     */
    /// 用户也可以指定id查询(但是依旧需要是脱敏后的数据)
    @GetMapping("/get/vo")
//    @SaSpaceCheckPermission(value= SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<PictureVo> getPictureVOById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);

        // 查询数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);

        User loginUser = userService.getLoginUser(request);
        Long spaceId = picture.getSpaceId();
        boolean isSpacePicture = (spaceId != null && spaceId != 0L);

        if (isSpacePicture) {
            // 空间图片：必须登录，走 RBAC 权限校验（不限制审核状态）
            ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
        } else {
            // 公共图片：已审核的任何人可见，未审核的仅本人/管理员可见
            if (loginUser == null
                    && !picture.getReviewStatus().equals(PictureReviewStatusEnum.PASS.getValue())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "该图片未审核通过");
            }
            if (loginUser != null
                    && !picture.getUserId().equals(loginUser.getId())
                    && !picture.getReviewStatus().equals(PictureReviewStatusEnum.PASS.getValue())
                    && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "该图片未审核通过");
            }
        }

        Space space = isSpacePicture ? spaceService.getById(spaceId) : null;
        List<String> permissionList = spaceUserAuthManger.getPermissionList(space, loginUser);
        // 获取封装类
        PictureVo pictureVO = pictureService.getPictureVO(picture, request);
        pictureVO.setPermissionList(permissionList);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 分页获取图片列表（仅管理员可用）
     */
    @PostMapping("/list/page")
    @AuthCheck(UserRoleEnum.ADMIN)
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();

        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
        return ResultUtils.success(picturePage);
    }

    /**
     * 分页获取图片列表（封装类）
     */
    @PostMapping("/list/page/vo")
//    @SaSpaceCheckPermission(value= SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<Page<PictureVo>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                             HttpServletRequest request) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        /// 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        Long spaceId = pictureQueryRequest.getSpaceId();



        if(spaceId == null || spaceId == 0L){
            /// 限制只能查询是审核通过的
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());

            pictureQueryRequest.setNullSpaceId(true);//不用检查的强制要求只能看公共图片


        }else{
            //必须存在且是用户本人的
            Space space = spaceService.getById(spaceId);

            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);

            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);

            ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);

        }


        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size),
                pictureService.getQueryWrapper(pictureQueryRequest));
        // 获取封装类
        return ResultUtils.success(pictureService.getPictureVOPage(picturePage, request));
    }




    /**
     * 分页获取图片列表(有缓存)（封装类）
     */
    @PostMapping("/list/page/vo/cache")
    public BaseResponse<Page<PictureVo>> listPictureVOByPageWithCache(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                             HttpServletRequest request) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        Long spaceId = pictureQueryRequest.getSpaceId();

        if (spaceId == null || spaceId == 0L) {
            // 公共图库：限制只能查询审核通过的
            pictureQueryRequest.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            pictureQueryRequest.setNullSpaceId(true);
        } else {
            // 空间图片：校验权限
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
            boolean hasPermission = StpKit.SPACE.hasPermission(SpaceUserPermissionConstant.PICTURE_VIEW);
            ThrowUtils.throwIf(!hasPermission, ErrorCode.NO_AUTH_ERROR);
        }

        Page<PictureVo> pictureVoPage = pictureService.listPictureVoByCache(pictureQueryRequest, request, current, size);
        return ResultUtils.success(pictureVoPage);
    }


    /**
     * 编辑图片（给用户使用）
     */
    @PostMapping("/edit")
    @SaSpaceCheckPermission(value= SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest, HttpServletRequest request) {
        if (pictureEditRequest == null || pictureEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        pictureService.editPictureService(pictureEditRequest, request);
        Picture editPic = pictureService.getById(pictureEditRequest.getId());
        if (editPic != null && editPic.getSpaceId() != null && editPic.getSpaceId() != 0L) {
            pictureService.clearListCache(editPic.getSpaceId());
        }
        return ResultUtils.success(true);
    }


    @GetMapping("/tag_category")
        public BaseResponse<PictureTagCategory> listPictureTagCategory() {
            PictureTagCategory pictureTagCategory = new PictureTagCategory();
            List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
            List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
            pictureTagCategory.setTagList(tagList);
            pictureTagCategory.setCategoryList(categoryList);
            return ResultUtils.success(pictureTagCategory);
        }


    @PostMapping("/review")
    @AuthCheck(UserRoleEnum.ADMIN)
    public BaseResponse<Boolean> addPictureReview(@RequestBody PictureReviewRequest pictureReviewRequest, HttpServletRequest request) {

        // ThrowUtils.throwIf(pictureReviewRequest==null, ErrorCode.PARAMS_ERROR);

        User loginUser = userService.getLoginUser(request);

        pictureService.doPictureReview(pictureReviewRequest, loginUser);
        // 审核后清缓存：空间图片立即可见/不可见，公共图库等 TTL
        Picture reviewedPic = pictureService.getById(pictureReviewRequest.getId());
        if (reviewedPic != null && reviewedPic.getSpaceId() != null && reviewedPic.getSpaceId() != 0L) {
            pictureService.clearListCache(reviewedPic.getSpaceId());
        }
        return ResultUtils.success(true);
    }

    @PostMapping("/upload/batch")
    @AuthCheck(UserRoleEnum.ADMIN)
    public BaseResponse<Integer> uploadPictureByBatch(@RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest,HttpServletRequest  request){
        ThrowUtils.throwIf(pictureUploadByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User user = userService.getLoginUser(request);
        Integer result = pictureService.uploadPictureByBath(pictureUploadByBatchRequest, user);

        return ResultUtils.success(result);
    }


    /**
     * 以图搜图
     * @param searchPictureByPictureRequest
     * @return
     */
    @PostMapping("/search/picture")
    @SaSpaceCheckPermission(value= SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<List<ImageSearchResult>> searchPictureByPicture(@RequestBody SearchPictureByPictureRequest searchPictureByPictureRequest){



        ThrowUtils.throwIf(searchPictureByPictureRequest == null, ErrorCode.PARAMS_ERROR);

        Long pictureId = searchPictureByPictureRequest.getPictureId();

        ThrowUtils.throwIf(pictureId == null || pictureId<0, ErrorCode.PARAMS_ERROR);

        Picture picture = pictureService.getById(pictureId);

        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);

        ImageSearchApiFacade imageSearchApiFacade=new ImageSearchApiFacadeByBaidu(); //fixme:这里可以替换实现

        List<ImageSearchResult> imageSearchResults = imageSearchApiFacade.searchImage(picture.getUrl());

        return ResultUtils.success(imageSearchResults);


    }

    /**
     * 批量编辑图片
     */
    @PostMapping("/edit/batch")
    @SaSpaceCheckPermission(value= SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<Boolean> editPictureByBatch(@RequestBody PictureEditByBatchRequest pictureEditByBatchRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureEditByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.editPictureByBatch(pictureEditByBatchRequest, loginUser);
        if (pictureEditByBatchRequest.getSpaceId() != null && pictureEditByBatchRequest.getSpaceId() != 0L) {
            pictureService.clearListCache(pictureEditByBatchRequest.getSpaceId());
        }
        return ResultUtils.success(true);
    }

    /**
     * 创建扩图任务
     * @param createPictureOutPaintingTaskRequest
     * @param request
     * @return
     */
    @PostMapping("/out_painting/create_task")
    @SaSpaceCheckPermission(value= SpaceUserPermissionConstant.PICTURE_EDIT)
    public BaseResponse<CreateOutPaintingTaskResponse> createAiExpendTask(@RequestBody CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, HttpServletRequest request) {

        ThrowUtils.throwIf(createPictureOutPaintingTaskRequest == null || createPictureOutPaintingTaskRequest.getPictureId()==null, ErrorCode.PARAMS_ERROR);

        User loginUser = userService.getLoginUser(request);

        CreateOutPaintingTaskResponse pictureOutPaintTask = pictureService.createPictureOutPaintTask(createPictureOutPaintingTaskRequest, loginUser);

        return ResultUtils.success(pictureOutPaintTask);

    }

    /**
     * 查询扩图任务
     * @param taskId
     * @return
     */
    @GetMapping("/out_painting/get_task")
    public BaseResponse<GetOutPaintingTaskResponse> getPictureOutPaintingTask(String taskId) {

        ThrowUtils.throwIf(taskId == null || taskId.isEmpty(), ErrorCode.PARAMS_ERROR);

        GetOutPaintingTaskResponse outPaintingTask = aliYunAiApi.getOutPaintingTask(taskId);

        return ResultUtils.success(outPaintingTask);


    }


    /**
     * 按照颜色搜索
     */
    @PostMapping("/search/color")
    @SaSpaceCheckPermission(value = SpaceUserPermissionConstant.PICTURE_VIEW)
    public BaseResponse<List<PictureVo>> searchPictureByColor(@RequestBody SearchPictureByColorRequest searchPictureByColorRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(searchPictureByColorRequest == null, ErrorCode.PARAMS_ERROR);
        String picColor = searchPictureByColorRequest.getPicColor();
        Long spaceId = searchPictureByColorRequest.getSpaceId();
        User loginUser = userService.getLoginUser(request);
        List<PictureVo> pictureVOList = pictureService.searchPictureByColor(spaceId, picColor, loginUser);
        return ResultUtils.success(pictureVOList);
    }


}
