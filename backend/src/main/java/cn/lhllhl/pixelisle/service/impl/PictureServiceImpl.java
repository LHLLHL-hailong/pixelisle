package cn.lhllhl.pixelisle.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.lhllhl.pixelisle.api.aliyunai.AliYunAiApi;
import cn.lhllhl.pixelisle.api.aliyunai.model.CreateOutPaintingTaskRequest;
import cn.lhllhl.pixelisle.api.aliyunai.model.CreateOutPaintingTaskResponse;
import cn.lhllhl.pixelisle.common.DeleteRequest;
import cn.lhllhl.pixelisle.exception.BusinessException;
import cn.lhllhl.pixelisle.exception.ErrorCode;
import cn.lhllhl.pixelisle.exception.ThrowUtils;
import cn.lhllhl.pixelisle.management.CosManger;
import cn.lhllhl.pixelisle.management.update.FilePictureUpload;
import cn.lhllhl.pixelisle.management.update.UrlPictureUpload;
import cn.lhllhl.pixelisle.model.dto.picture.*;
import cn.lhllhl.pixelisle.model.entity.Space;
import cn.lhllhl.pixelisle.model.entity.User;
import cn.lhllhl.pixelisle.model.enums.PictureReviewStatusEnum;
import cn.lhllhl.pixelisle.model.enums.SpaceTypeEnum;
import cn.lhllhl.pixelisle.model.vo.PictureVo;
import cn.lhllhl.pixelisle.model.vo.UserVo;
import cn.lhllhl.pixelisle.service.SpaceService;
import cn.lhllhl.pixelisle.service.UserService;
import cn.lhllhl.pixelisle.utis.ColorSimilarUtils;
import cn.lhllhl.pixelisle.utis.ColorTransformUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.lhllhl.pixelisle.model.entity.Picture;
import cn.lhllhl.pixelisle.service.PictureService;
import cn.lhllhl.pixelisle.mapper.PictureMapper;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
* @description 针对表【picture】的数据库操作Service实现
* @createDate 2026-02-28 20:42:54
*/
@Slf4j
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService{

//
//    @Autowired
//    FileManger fileManger;

    @Autowired
    UserService userService;

    @Autowired
    FilePictureUpload filePictureUpload;

    @Autowired
    UrlPictureUpload urlPictureUpload;



    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    Cache<String,String> LOCAL_CACHE;
    @Autowired
    private CosManger cosManger;

    @Autowired
    private SpaceService spaceService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Lazy
    @Autowired
    private PictureService selfProxy;

    @Autowired
    private AliYunAiApi aliYunAiApi;


    /**
     * 插入图片对象的校验（还校验id  ?）
     * @param picture
     */
    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改数据时，id 不能为空，有参数则校验
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id 不能为空");
        // 如果传递了 url，才校验
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }


    /**
     * 传入了id就是更新，没有传入id就是新增（覆盖式更新）（注意这个更新是把原本的图片文件删除了）
     * @param inputSource
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    @Override
    public PictureVo uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {


        //TODO:如果说用户想要删除图片的话添加到一半就退出需不需要删除对象存储
        if(loginUser == null){
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        Long spaceId = pictureUploadRequest.getSpaceId();
        // 规范化：0 表示公共图片，业务层统一用 null 表示公共
        if (spaceId != null && spaceId == 0L) {
            spaceId = null;
        }

        /// 存在上传空间时进行检验存在性与权限
        if(spaceId != null){

            Space space = spaceService.getById(spaceId);

            ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR,"空间不存在");

            if(space.getTotalCount()>=space.getMaxCount()){
                throw new BusinessException(ErrorCode.OPERATION_ERROR,"剩余空间条数不足");
            }

            if(space.getTotalSize()>=space.getMaxSize()){
                throw new BusinessException(ErrorCode.OPERATION_ERROR,"剩余最大小不足");

            }

            //
        }


        Long pictureId=null;

        if(pictureUploadRequest != null){
            pictureId=pictureUploadRequest.getId();
        }



        if(pictureId != null){


            //添加了用于更新的权限校验
          //  QueryWrapper<Picture> pictureId1 = new QueryWrapper<Picture>().eq("id", pictureId);

            Picture oldPicture = this.getById(pictureId);//FIXME

            if (oldPicture == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR,"图片不存在");
            }

            // 权限校验已由 Controller 层 @SaSpaceCheckPermission 处理

            /// 确保了修改时候的空间无法修改
            /// 如果更新的时候发现老的图片有spaceId新的没有就把请求的spaceId指向老的（因为当spaceId为null时候，后面的更新不会只更新spaceId为空的（增量更新，而其为空的时候又没有权限校验））
            // 规范化：null 和 0 都表示公共图片，兼容迁移前后数据
            boolean isNewPublic = (spaceId == null);
            boolean isOldPublic = (oldPicture.getSpaceId() == null || oldPicture.getSpaceId() == 0L);

            if (isNewPublic) {
                // 请求未指定空间 → 沿用旧图片的空间
                if (!isOldPublic) {
                    spaceId = oldPicture.getSpaceId();
                }
                // 两者都是公共图片 → spaceId 保持 null（后续写入会转为 0L）
            } else {
                // 请求指定了非公共空间 → 禁止新旧空间不一致
                if (isOldPublic || !oldPicture.getSpaceId().equals(spaceId)) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "修改的图片不在这个空间上(图片空间无法更改)");
                }
            }


//            boolean id = this.exists(pictureId1);
//            if(!id){
//                throw new BusinessException(ErrorCode.PARAMS_ERROR,"图片不存在");
//            }
        }

        //id-->存在更新
        //id-->null-->新建




        String updatePathPrefix=null;
        //不同空间cos存储位置不同
        if(spaceId == null){

            updatePathPrefix=String.format("public/%s",loginUser.getId());

        }else{
            updatePathPrefix=String.format("space/%s",spaceId);
        }





        UploadPictureResult uploadPictureResult=null;


        /// 根据文件的类型选择正确的上传器
        if(inputSource instanceof MultipartFile){

            uploadPictureResult=filePictureUpload.uploadPictureByUrl(inputSource, updatePathPrefix);


        }else if(inputSource instanceof String){
            uploadPictureResult=urlPictureUpload.uploadPictureByUrl(inputSource, updatePathPrefix);

        }else {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"文件上传的类型不支持");
        }


        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setName( uploadPictureResult.getPicName());
        // 公共图片使用 0 作为分片键，确保 ShardingSphere 能精确路由到主表
        picture.setSpaceId(spaceId != null ? spaceId : 0L);

        if(pictureUploadRequest!=null && StringUtils.hasText( pictureUploadRequest.getNamePrefix())){

            picture.setName( pictureUploadRequest.getNamePrefix());///设置图片名称(仅是传来图片名称的前提下)
        }
        picture.setPicSize( uploadPictureResult.getPicSize());//
        picture.setPicWidth( uploadPictureResult.getPicWidth());//
        picture.setPicHeight( uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        picture.setEditTime(new Date());
        picture.setThumbnailUrl(uploadPictureResult.getThumbnailUrl());//设置缩率图
        picture.setPicColor(ColorTransformUtils.getStandardColor(uploadPictureResult.getPicColor()) );


        if(pictureId != null){
            picture.setId(pictureId);
            picture.setCreateTime(new Date());
        }

        /// 刷新审核状态

        fillReviewParam(picture, loginUser);

        // 查旧记录（只读），事务内做额度扣减，事务后删 COS
        Picture oldPicture = null;
        if (pictureId != null) {
            oldPicture = this.getById(pictureId);
        }

        Long finalSpaceId = spaceId;
        final Long pid = pictureId;
        final Picture finalOldPicture = oldPicture;

        /// 插入数据库和更新额度要么全部成功要么全部回滚
        transactionTemplate.execute(status -> {
            boolean b;
            if (pid != null) {
                // 更新：WHERE 必须带 spaceId，ShardingSphere 才能路由到正确的分表
                UpdateWrapper<Picture> updateWrapper = new UpdateWrapper<>();
                updateWrapper.eq("id", picture.getId()).eq("spaceId", picture.getSpaceId());
                b = this.update(picture, updateWrapper);

                // 扣减旧图片占用的空间额度（事务内，回滚时一起撤销）
                if (finalOldPicture != null && finalSpaceId != null) {
                    UpdateWrapper<Space> oldSpaceWrapper = new UpdateWrapper<>();
                    oldSpaceWrapper.eq("Id", finalSpaceId)
                            .setSql("totalSize=totalSize-" + finalOldPicture.getPicSize())
                            .setSql("totalCount=totalCount-1");
                    boolean oldUpdate = spaceService.update(oldSpaceWrapper);
                    ThrowUtils.throwIf(!oldUpdate, ErrorCode.OPERATION_ERROR, "额度更新失败");
                }
            } else {
                b = this.save(picture);
            }
            ThrowUtils.throwIf(!b, ErrorCode.OPERATION_ERROR, "数据库插入失败");
            if (finalSpaceId != null) {//不是公共图库的时候才去减少额度
                UpdateWrapper<Space> updateWrapper = new UpdateWrapper<>();

                updateWrapper.eq("Id", finalSpaceId).setSql("totalSize=totalSize+"+picture.getPicSize()).setSql("totalCount=totalCount+1");
                boolean update = spaceService.update(updateWrapper);

                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }

            return null;

        });

        // 事务成功提交后，异步删除旧 COS 文件（失败也不影响数据一致性）
        if (finalOldPicture != null) {
            selfProxy.clearPictureFile(finalOldPicture);
        }

        return PictureVo.pictureToPictureVo(picture);
    }


    @Override
    public void editPictureService(PictureEditRequest pictureEditRequest, HttpServletRequest request) {
        // 在此处将实体类和 DTO 进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);
        // 注意将 list 转为 string
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        // 设置编辑时间
        picture.setEditTime(new Date());
        // 数据校验
        this.validPicture(picture);
        User loginUser = userService.getLoginUser(request);
        /// 判断是否存在
        long id = pictureEditRequest.getId();
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
//        /// 仅本人或管理员可编辑
//        if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
//            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
//        }
        // 分情况校验权限
        // this.checkPictureAuth(loginUser, oldPicture);
        // 刷新审核状态
        /// 刷新审核状态
        this.fillReviewParam(picture, loginUser);
        // 操作数据库：WHERE 必须带 spaceId，ShardingSphere 才能路由到正确的分表
        UpdateWrapper<Picture> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", picture.getId()).eq("spaceId", oldPicture.getSpaceId());
        boolean result = this.update(picture, updateWrapper);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
    }





    @Override
    public boolean deletePictureService(DeleteRequest deleteRequest, User loginUser) {
        Picture picture = this.getById(deleteRequest.getId());


        /// update delete 先判断数据是否存在
        if(picture==null){

            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }

        // 分情况校验权限
        // this.checkPictureAuth(loginUser,picture);

//        if(userId != picture.getUserId() && !loginUser.getUserRole().equals(UserRoleEnum.ADMIN.getValue())){
//
//            throw  new BusinessException(ErrorCode.NO_AUTH_ERROR);
//
//        }

        Long finalSpaceId = picture.getSpaceId();

        /// 删除数据库和更新额度要么全部成功要么全部回滚
        Long delSpaceId = picture.getSpaceId();
        transactionTemplate.execute(status -> {
            // DELETE：WHERE 必须带 spaceId，ShardingSphere 才能路由到正确的分表
            QueryWrapper<Picture> deleteWrapper = new QueryWrapper<>();
            deleteWrapper.eq("id", deleteRequest.getId()).eq("spaceId", delSpaceId);
            boolean b = this.remove(deleteWrapper);
            ThrowUtils.throwIf(!b, ErrorCode.OPERATION_ERROR,"数据库删除失败");
            if (finalSpaceId != null && finalSpaceId != 0L) {//不是公共图库的时候才去增加额度
                UpdateWrapper<Space> updateWrapper = new UpdateWrapper<>();

                updateWrapper.eq("Id", finalSpaceId).setSql("totalSize=totalSize-"+picture.getPicSize()).setSql("totalCount=totalCount-1");
                boolean update = spaceService.update(updateWrapper);

                ThrowUtils.throwIf(!update,ErrorCode.OPERATION_ERROR,"额度更新失败");
            }

            return null;

        });


//        if(!b){
//            throw  new BusinessException(ErrorCode.SYSTEM_ERROR);
//        }

        //删除指定的图片（走代理确保 @Async 生效）
        selfProxy.clearPictureFile(picture);
        return true;
    }



    /**
     * picture的分页查询参数转化为对应的wrapper
     *
     * @param pictureQueryRequest
     * @return
     */
    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        //FIXME:新增字段的映射关系
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }

        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long reviewerId = pictureQueryRequest.getReviewerId();


        // 从对象中取值
        boolean nullSpaceId = pictureQueryRequest.isNullSpaceId();
        Long spaceId = pictureQueryRequest.getSpaceId();


//        if(nullSpaceId){
//            spaceId=null;
//        }


        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();


        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Date endEditTime = pictureQueryRequest.getEndEditTime();

        // 从多字段中搜索
        if (StrUtil.isNotBlank(searchText)) {
            // 需要拼接查询条件
            queryWrapper.and(qw -> qw.like("name", searchText)
                    .or()
                    .like("introduction", searchText)
            );
        }
        queryWrapper.eq(spaceId!=null,"spaceId",spaceId);
        // 公共图库查询：兼容迁移前(spaceId IS NULL)和迁移后(spaceId = 0)
        if (nullSpaceId) {
            queryWrapper.and(w -> w.isNull("spaceId").or().eq("spaceId", 0L));
        } else {
            // 私有/团队空间：排除已拒绝的图片
            queryWrapper.ne("reviewStatus", PictureReviewStatusEnum.REJECT.getValue());
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
        queryWrapper.like(ObjUtil.isNotEmpty(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);

        //编辑时间的设置
        queryWrapper.ge(ObjUtil.isNotEmpty(startEditTime), "editTime", startEditTime);
        queryWrapper.lt(ObjUtil.isNotEmpty(endEditTime), "editTime", endEditTime);
        // JSON 数组查询
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }


    /**
     * 要把返回的图片信息上拼接上创作者（脱敏后）的信息
     *
     * @param picture
     * @param request
     * @return
     */
    @Override
    public PictureVo getPictureVO(Picture picture, HttpServletRequest request) {
        // 对象转封装类
        PictureVo pictureVO = PictureVo.pictureToPictureVo(picture);

        // 关联查询用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVo userVO = userService.getUserVO(user);
            pictureVO.setUserVo(userVO);
        }

        return pictureVO;
    }



    /**
     * 分页获取图片封装
     */
    @Override
    public Page<PictureVo> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVo> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        // 对象列表 => 封装对象列表
        List<PictureVo> pictureVOList = pictureList.stream().map(PictureVo::pictureToPictureVo).collect(Collectors.toList());
        // 1. 关联查询用户信息
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVO.setUserVo(userService.getUserVO(user));
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }


    /**
     * 图片审核的执行方法
     *
     * @param pictureReviewRequest
     * @param loginUser
     */
    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {

        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);

        //校验参数

        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        String reviewMessage = pictureReviewRequest.getReviewMessage();

        PictureReviewStatusEnum enumByValue = PictureReviewStatusEnum.getEnumByValue(reviewStatus);

        if (id == null || enumByValue == null || id < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }


        //图片是否存在

        Picture picture = this.getById(id);

        if (picture == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片不存在");
        }

        if (enumByValue == PictureReviewStatusEnum.REJECT && reviewMessage == null) {

            throw new BusinessException(ErrorCode.PARAMS_ERROR, "拒绝必须填写原因");
        }

        if (picture.getReviewStatus().equals(pictureReviewRequest.getReviewStatus())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "禁止重复审核");
        }

        //自动填充

        Picture pictureObj = new Picture();

        BeanUtils.copyProperties(picture, pictureObj);

        pictureObj.setReviewTime(new Date());
        pictureObj.setReviewerId(loginUser.getId());
        pictureObj.setReviewStatus(reviewStatus);
        pictureObj.setReviewMessage(reviewMessage);

        //执行修改：WHERE 必须带 spaceId，ShardingSphere 才能路由到正确的分表
        UpdateWrapper<Picture> reviewWrapper = new UpdateWrapper<>();
        reviewWrapper.eq("id", pictureObj.getId()).eq("spaceId", pictureObj.getSpaceId());
        this.update(pictureObj, reviewWrapper);

    }

    /**
     * 公共的刷新审核状态代码
     *
     * @param picture
     * @param loginUser
     */
    @Override
    public void fillReviewParam(Picture picture, User loginUser) {

        if (userService.isAdmin(loginUser)) {
            picture.setReviewerId(loginUser.getId());
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewTime(new Date());
            picture.setReviewMessage("管理员自动过审");
        } else {

            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }


    }

    @Override
    public Integer uploadPictureByBath(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {

        //校验参数

        String searchText = pictureUploadByBatchRequest.getSearchText();
        Integer count = pictureUploadByBatchRequest.getCount();
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();


        //默认值名称设置
        if(!StringUtils.hasText(namePrefix)){

            if(StringUtils.hasText(searchText)){
                namePrefix=searchText;
            }else{
                namePrefix="default";
            }
        }



        ThrowUtils.throwIf(count>30 ,ErrorCode.PARAMS_ERROR,"最多30条");
        ThrowUtils.throwIf(loginUser==null, ErrorCode.NOT_LOGIN_ERROR);

        //抓取内容

        String fetchUrl="https://cn.bing.com/images/async?q={}&mmasync=1";

        String url = StrUtil.format(fetchUrl, searchText);

        Document document;
        try {
            document = Jsoup.connect(url).get();
        } catch (IOException e) {
            log.error("获取页面失败{}",e);
            e.printStackTrace();
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"获取页面失败");
        }

        //解析内容

        Elements dbController = document.getElementsByClass("dgControl");//FIXME:感觉不是这个
       // Elements dbController = document.getElementsByClass("dg_b isvctrl");//FIXME:感觉不是这个

        if(dbController==null || dbController.size()==0){

            throw new BusinessException(ErrorCode.PARAMS_ERROR,"获取主元素失败");
        }

        Element first = dbController.first();

        Elements select = first.select("a.iusc");


        int countSum=0;
        int total=0;

        long time = System.currentTimeMillis();
        for (Element element : select) {

            if(countSum>=count){
                break;
            }

            String mAttr = element.attr("m");
            if (!StringUtils.hasText(mAttr)) {
                total++;
                continue;
            }

            JSONObject m = JSONUtil.parseObj(mAttr);
            String PictureUrl = m.getStr("murl");
            // 降级：murl 为空时用 turl（Bing CDN 缓存大图）
            if (!StringUtils.hasText(PictureUrl)) {
                PictureUrl = m.getStr("turl");
            }

            if(!StringUtils.hasText(PictureUrl) || ! Validator.isUrl(PictureUrl)){
                log.warn("图像地址解析失败" );
                total++;
                continue;
            }

            int index = PictureUrl.indexOf("?");
            if(index!=-1){
                PictureUrl=PictureUrl.substring(0,index);
            }


            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();

            try {
                pictureUploadRequest.setFileUrl(PictureUrl);
                pictureUploadRequest.setNamePrefix(namePrefix+"-"+time+"-"+(countSum+1));///拼接图片名称
                pictureUploadRequest.setSpaceId(pictureUploadByBatchRequest.getSpaceId());
                PictureVo pictureVo = this.uploadPicture(PictureUrl, pictureUploadRequest, loginUser);

                log.info("图片上传成功，{}",pictureVo);
            } catch (Exception e) {
                total++;
                log.error("图片上传失败 {}" ,e.getMessage());
                continue;
            }

            countSum++;


        }

        if(countSum!=count){
            log.warn("预计上传 {} ,实际上传 {}  ,上传失败 {}" ,count,countSum,total);
        }else{
            log.info("预计上传 {} ,实际上传 {}  ,上传失败 {}" ,count,countSum,total);
        }

        // 清缓存，让新上传的图片立即可见
        Long spaceId = pictureUploadByBatchRequest.getSpaceId();
        if (spaceId != null && spaceId != 0L) {
            clearListCache(spaceId);
        }

        return countSum;




        //上传图片
    }

    @Override
    public Page<PictureVo> listPictureVoByCache(PictureQueryRequest pictureQueryRequest, HttpServletRequest request, long current, long size) {
        /// 有搜索词就直接走数据库
        if(StringUtils.hasText(pictureQueryRequest.getSearchText()) ){
            log.info("执行了搜索词，缓存避免");
            Page<Picture> picturePage = this.page(new Page<>(current, size),
                    this.getQueryWrapper(pictureQueryRequest));
            return this.getPictureVOPage(picturePage, request);
        }

        Long spaceId = pictureQueryRequest.getSpaceId();
        if (spaceId == null) spaceId = 0L;

        // 查空间类型
        Space space = spaceService.getById(spaceId);
        int spaceType = (space != null) ? space.getSpaceType() : 0;

        // ── 构建缓存 key ──
        String str = new JSONObject(pictureQueryRequest).toString();
        String keyPost = DigestUtils.md5DigestAsHex(str.getBytes());

        String key;
        if (spaceType == SpaceTypeEnum.TEAM.getValue()) {
            // 团队空间：key 加 userId 隔离不同角色的权限
            User loginUser = userService.getLoginUser(request);
            key = String.format("pic:list:%d:u%d:%s", spaceId, loginUser.getId(), keyPost);
        } else {
            // 公共图库 / 私有空间：key 不含 userId（私有空间只有一个人，无需隔离）
            key = String.format("pic:list:%d:%s", spaceId, keyPost);
        }

        // ── ① Caffeine 本地缓存 ──
        String ifPresent = LOCAL_CACHE.getIfPresent(key);
        if(ifPresent != null){
            log.info("本地缓存直接命中 {}", key);
            return JSONUtil.toBean(ifPresent, new cn.hutool.core.lang.TypeReference<Page<PictureVo>>() {}, false);
        }

        // ── ② Redis ──
        ValueOperations valueOperations = redisTemplate.opsForValue();
        Page<PictureVo> pictureVoPage = (Page<PictureVo>) valueOperations.get(key);
        if(pictureVoPage != null){
            log.info("Redis 命中 {}，回写本地", key);
            LOCAL_CACHE.put(key, JSONUtil.toJsonStr(pictureVoPage));
            return pictureVoPage;
        }

        // ── ③ MySQL ──
        Page<Picture> picturePage = this.page(new Page<>(current, size),
                this.getQueryWrapper(pictureQueryRequest));
        Page<PictureVo> pictureVOPage = this.getPictureVOPage(picturePage, request);

        // 回写缓存（TTL 5~10min 随机防雪崩）
        long expireTime = 5L + RandomUtil.randomLong(0L, 5L);
        valueOperations.set(key, pictureVOPage, expireTime, TimeUnit.MINUTES);
        LOCAL_CACHE.put(key, JSONUtil.toJsonStr(pictureVOPage));
        log.info("回写缓存 {}，TTL={}min", key, expireTime);

        return pictureVOPage;
    }

    /**
     * 清除某个空间的图片列表缓存（私有/团队空间写操作后调用）
     * 公共图库不手动清，等 TTL 自然过期
     */
    @Override
    public void clearListCache(Long spaceId) {
        final long sid = (spaceId != null) ? spaceId : 0L;
        String prefix = "pic:list:" + sid + ":";
        String anaPattern = "ana:*:s" + sid;
        String anaPatternSub = "ana:*:s" + sid + ":*";
        String sidMarker = ":s" + sid;
        try {
            // 1. 删除 Redis（先清远端，保证缓存一致性）
            for (String pattern : new String[]{prefix + "*", anaPattern, anaPatternSub}) {
                scanAndDelete(pattern);
            }
            log.info("clearListCache redis done for spaceId={}", sid);
        } finally {
            // 2. 删除 Caffeine（finally 保证一定执行）
            LOCAL_CACHE.asMap().keySet().removeIf(key ->
                    key.startsWith(prefix)
                    || (key.startsWith("ana:") && (key.endsWith(sidMarker) || key.contains(sidMarker + ":"))));
        }
    }

    /**
     * 使用 SCAN 分批扫描匹配 key 并逐批删除，避免 KEYS 阻塞 Redis
     */
    private void scanAndDelete(String pattern) {
        redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Void>) connection -> {
            try (org.springframework.data.redis.core.Cursor<byte[]> cursor = connection.scan(
                    org.springframework.data.redis.core.ScanOptions.scanOptions()
                            .match(pattern).count(200).build())) {
                java.util.List<byte[]> batch = new java.util.ArrayList<>();
                while (cursor.hasNext()) {
                    batch.add(cursor.next());
                    if (batch.size() >= 200) {
                        connection.del(batch.toArray(new byte[0][]));
                        batch.clear();
                    }
                }
                if (!batch.isEmpty()) {
                    connection.del(batch.toArray(new byte[0][]));
                }
            }
            return null;
        });
    }

    @Async("clearPictureExecutor")
    @Override
    public void deleteSpaceAndPicture(Long spaceId) {

        QueryWrapper<Picture> pictureQueryWrapper = new QueryWrapper<>();

        pictureQueryWrapper.eq("spaceId", spaceId);

        java.util.List<Picture> picList = this.getBaseMapper().selectList(pictureQueryWrapper);
        for (Picture picture : picList) {
            selfProxy.clearPictureFile(picture);
        }

        this.getBaseMapper().delete(pictureQueryWrapper);

        // 异步删除完成后清缓存（此时 DB 已无数据，清缓存安全）
        clearListCache(spaceId);
    }


    @Async("clearPictureExecutor")
    @Override
    public void clearPictureFile(Picture oldPicture) {

        ///在秒传场景下判断

//
//        String url = oldPicture.getUrl();
//
//        LambdaQueryWrapper<Picture> queryWrapper = new LambdaQueryWrapper<Picture>().eq(Picture::getUrl, url);
//        long count = this.count(queryWrapper);
//
//        if(count >1){
//            log.warn("进行了秒传的删除，执行删除退避 url : {}",oldPicture.getUrl());
//            return;
//        }


        try {


            cosManger.deleteObject(transferOnjKey(oldPicture.getUrl()));

            String thumbnailUrl = oldPicture.getThumbnailUrl();


            cosManger.deleteObject(transferOnjKey(thumbnailUrl));
        } catch (Exception e) {
            log.error("异步清理异常 url:{} and message {}",oldPicture.getUrl(),e.getMessage());
                throw new RuntimeException(e);
        }


    }


//    /**
//     * 公用图库中的图片只有系统管理员和本人可以操作
//     * 私有空间的图片只有本人能操纵
//     * @param loginUser
//     * @param picture
//     */
//    @Override
//    public void checkPictureAuth(User loginUser, Picture picture) {
//        Long spaceId = picture.getSpaceId();
//
//        if(spaceId==null){
//
//            if(!picture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser) ){
//
//                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
//            }
//
//        }else{
//
//            if(!picture.getUserId().equals(loginUser.getId())  ){
//
//                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
//            }
//
//
//
//        }
//    }

    /**
     * 批量编辑图片的 分类 标签 模板重命名
     * @param pictureEditByBatchRequest
     * @param loginUser
     */
    @Override
    public void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);

        List<Long> pictureIdList = pictureEditByBatchRequest.getPictureIdList();
        Long spaceId = pictureEditByBatchRequest.getSpaceId();
        String category = pictureEditByBatchRequest.getCategory();
        List<String> tags = pictureEditByBatchRequest.getTags();
        ThrowUtils.throwIf(pictureIdList==null || pictureIdList.size()==0, ErrorCode.PARAMS_ERROR,"必须有图片列表");

        ThrowUtils.throwIf(spaceId == null || spaceId == 0L, ErrorCode.PARAMS_ERROR);

        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        Space space = spaceService.getById(spaceId);

        if (!userService.isAdmin(loginUser)) {
            ThrowUtils.throwIf(space==null, ErrorCode.PARAMS_ERROR,"空间不存在");
            // 权限校验已由 Controller 层 @SaSpaceCheckPermission 处理
        }else{
            log.warn("admin对公共图库进行批量管理");//fixme:不保证一定全对
        }

        QueryWrapper<Picture> pictureQueryWrapper = new QueryWrapper<>();
        pictureQueryWrapper.eq("spaceId", spaceId);
        pictureQueryWrapper.in("id", pictureIdList);//fixme :查询字段不一定是对的
        pictureQueryWrapper.select("spaceId","id");

        List<Picture> pictures = this.getBaseMapper().selectList(pictureQueryWrapper);

        pictures.stream().forEach(item->{


            if(category!=null){
                item.setCategory(category);
            }
            if(tags!=null){
                item.setTags(JSONUtil.toJsonStr(tags));
            }
        });


        String nameRule = pictureEditByBatchRequest.getNameRule();

        fillPictureNameWithPictureRules(pictures,nameRule);

        boolean status = true;
        // 批量更新：每条 UPDATE 必须带 spaceId，ShardingSphere 才能路由到正确的分表
        for (Picture item : pictures) {
            UpdateWrapper<Picture> batchUpdateWrapper = new UpdateWrapper<>();
            batchUpdateWrapper.eq("id", item.getId()).eq("spaceId", item.getSpaceId());
            status = this.update(item, batchUpdateWrapper) && status;
        }

        ThrowUtils.throwIf(!status,ErrorCode.OPERATION_ERROR);




    }

    @Override
    public CreateOutPaintingTaskResponse createPictureOutPaintTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser) {
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        ThrowUtils.throwIf(createPictureOutPaintingTaskRequest == null, ErrorCode.PARAMS_ERROR);

        Long pictureId = createPictureOutPaintingTaskRequest.getPictureId();
        ThrowUtils.throwIf(pictureId == null, ErrorCode.PARAMS_ERROR, "必须指定图片id");
        CreateOutPaintingTaskRequest.Parameters parameters = createPictureOutPaintingTaskRequest.getParameters();

        Picture oldPicture = this.getById(pictureId);

        ThrowUtils.throwIf(oldPicture == null, ErrorCode.PARAMS_ERROR, "请求的图片不存在");

        //权限校验
        // checkPictureAuth(loginUser, oldPicture);

        //创建request
        CreateOutPaintingTaskRequest request = new CreateOutPaintingTaskRequest();
        CreateOutPaintingTaskRequest.Input input = new CreateOutPaintingTaskRequest.Input();
        input.setImageUrl(oldPicture.getUrl());
        request.setInput(input);
        request.setParameters(parameters);

        CreateOutPaintingTaskResponse response = aliYunAiApi.createOutPaintingTask(request);

        return response ;


    }


    @Override
    public List<PictureVo> searchPictureByColor(Long spaceId, String picColor, User loginUser) {
        // 1. 校验参数
        ThrowUtils.throwIf(spaceId == null || StrUtil.isBlank(picColor), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 2. 校验空间存在（权限由 Controller @SaSpaceCheckPermission 保证）
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        // 3. 校验目标颜色格式
        Color targetColor;
        try {
            targetColor = Color.decode(picColor);
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "颜色格式不正确");
        }
        // 4. 查询该空间下有主色调的图片（排除审核驳回，最多 500 条）
        List<Picture> pictureList = this.lambdaQuery()
                .eq(Picture::getSpaceId, spaceId)
                .ne(Picture::getReviewStatus, PictureReviewStatusEnum.REJECT.getValue())
                .isNotNull(Picture::getPicColor)
                .last("LIMIT 500")
                .list();
        // 如果没有图片，直接返回空列表
        if (CollUtil.isEmpty(pictureList)) {
            return Collections.emptyList();
        }
        // 5. 计算相似度并排序
        List<Picture> sortedPictures = pictureList.stream()
                .sorted(Comparator.comparingDouble(picture -> {
                    String hexColor = picture.getPicColor();
                    if (StrUtil.isBlank(hexColor)) {
                        return Double.MAX_VALUE;
                    }
                    try {
                        Color pictureColor = Color.decode(hexColor);
                        return -ColorSimilarUtils.calculateSimilarity(targetColor, pictureColor);
                    } catch (NumberFormatException e) {
                        return Double.MAX_VALUE;
                    }
                }))
                .limit(12)
                .collect(Collectors.toList());
        // 转换为 PictureVO
        return sortedPictures.stream()
                .map(PictureVo::pictureToPictureVo)
                .collect(Collectors.toList());
    }


    //批量回填名称（根据模板）
    private void fillPictureNameWithPictureRules(List<Picture> pictures, String nameRule) {

        if(StrUtil.isEmpty(nameRule) || CollUtil.isEmpty(pictures)){
            return;
        }
        int count=1;

        try {
            for (Picture picture : pictures) {
                picture.setName(nameRule.replaceAll("\\{序号}", String.valueOf(count++)));
            }
        } catch (Exception e) {

            log.error("批量更改命名时,图片解析异常{}",e.getMessage());
            e.printStackTrace();
            throw new BusinessException(ErrorCode.PARAMS_ERROR);

        }


    }


    String transferOnjKey(String url){

        if(url==null){
            return null;
        }

        String target = url.substring(url.indexOf("myqcloud.com")+"myqcloud.com".length());


        return target;



    }


}




