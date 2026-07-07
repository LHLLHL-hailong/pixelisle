package cn.lhllhl.pixelisle.controller;

import cn.lhllhl.pixelisle.annotation.AuthCheck;
import cn.lhllhl.pixelisle.annotation.NoUsed;
import cn.lhllhl.pixelisle.common.BaseResponse;
import cn.lhllhl.pixelisle.common.DeleteRequest;
import cn.lhllhl.pixelisle.common.ResultUtils;
import cn.lhllhl.pixelisle.exception.BusinessException;
import cn.lhllhl.pixelisle.exception.ErrorCode;
import cn.lhllhl.pixelisle.exception.ThrowUtils;
import cn.lhllhl.pixelisle.management.auth.SpaceUserAuthManger;
import cn.lhllhl.pixelisle.management.sharding.DynamicShardingManager;
import cn.lhllhl.pixelisle.model.dto.space.*;
import cn.lhllhl.pixelisle.model.entity.Space;
import cn.lhllhl.pixelisle.model.entity.SpaceUser;
import cn.lhllhl.pixelisle.model.entity.User;
import cn.lhllhl.pixelisle.model.enums.SpaceLevelEnum;
import cn.lhllhl.pixelisle.model.enums.SpaceTypeEnum;
import cn.lhllhl.pixelisle.model.enums.UserRoleEnum;
import cn.lhllhl.pixelisle.service.SpaceUserService;
import cn.lhllhl.pixelisle.model.vo.SpaceVO;
import cn.lhllhl.pixelisle.service.PictureService;
import cn.lhllhl.pixelisle.service.SpaceService;
import cn.lhllhl.pixelisle.service.UserService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@Slf4j
@RequestMapping("/space")
@Api("正式的文件上传和处理的接口")
public class SpaceController {


    @Autowired
    private UserService userService;

    @Autowired
    private SpaceService spaceService;

    @Autowired
    private PictureService pictureService;
    @Autowired
    private SpaceUserService spaceUserService;

    @Autowired
    private SpaceUserAuthManger spaceUserAuthManger;

    @Autowired
    private DynamicShardingManager dynamicShardingManager;


    /**
     * 添加空间
     * @param spaceAddRequest
     * @param request
     * @return
     */
    @PostMapping("/add")
    public BaseResponse<Long> addSpace(@RequestBody SpaceAddRequest spaceAddRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(spaceAddRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        long id = spaceService.addSpace(spaceAddRequest, loginUser);
        return ResultUtils.success(id);
    }


    /**
     * 删除指定的空间
     *
     * @param deleteRequest
     * @param request
     * @return
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteSpace(@RequestBody DeleteRequest deleteRequest,
                                             HttpServletRequest request) {

        if (deleteRequest == null || deleteRequest.getId() == null) {

            throw new BusinessException(ErrorCode.PARAMS_ERROR, "删除的id值不能为空");
        }

        User loginUser = userService.getLoginUser(request);

        ThrowUtils.throwIf(loginUser == null || loginUser.getId() == null, ErrorCode.NOT_LOGIN_ERROR);

        Long userId = loginUser.getId();

        Space space = spaceService.getById(deleteRequest.getId());


        spaceService.checkSpaceAuth(loginUser, space);
//        /// update delete 先判断数据是否存在
//        if (space == null) {
//
//            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
//        }
//        /// 再判断只有本人或者管理员才能删除
//        if (userId != space.getUserId() && !loginUser.getUserRole().equals(UserRoleEnum.ADMIN.getValue())) {
//
//            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
//
//        }

        Long spaceId = space.getId();
        pictureService.deleteSpaceAndPicture(spaceId);

        // 级联逻辑删除空间成员关系
        spaceUserService.lambdaUpdate()
                .eq(SpaceUser::getSpaceId, spaceId)
                .remove();

        boolean b = spaceService.removeById(deleteRequest.getId());

        if (!b) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }

        return ResultUtils.success(b);


    }


//    /**
//     * 删除空间
//     */
//    @PostMapping("/delete")
//    public BaseResponse<Boolean> deleteSpace(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
//        if (deleteRequest == null || deleteRequest.getId() <= 0) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        }
//        User loginUser = userService.getLoginUser(request);
//        long id = deleteRequest.getId();
//        // 判断是否存在
//        Space oldSpace = spaceService.getById(id);
//        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
//        // 仅本人或管理员可删除
//        if (!oldSpace.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
//            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
//        }
//        // 操作数据库
//        boolean result = spaceService.removeById(id);
//        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
//        return ResultUtils.success(true);
//    }

    /**
     * 更新空间（仅管理员可用）
     * （增量更新（指定了级别没指定最大count或size除外））
     */

    @PostMapping("/update")
    @AuthCheck(UserRoleEnum.ADMIN)
    public BaseResponse<Boolean> updateSpace(@RequestBody SpaceUpdateRequest spaceUpdateRequest, HttpServletRequest request) {
        if (spaceUpdateRequest == null || spaceUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 将实体类和 DTO 进行转换
        Space space = new Space();
        BeanUtils.copyProperties(spaceUpdateRequest, space);

        // 数据校验
        spaceService.validSpace(space, false);
        // 判断是否存在
        long id = spaceUpdateRequest.getId();
        Space oldSpace = spaceService.getById(id);
        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);

        spaceService.fillSpaceBySpaceLevel(space);///自动填充最大值（当没有手动指定时）

        // 操作数据库
        boolean result = spaceService.updateById(space);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        // 团队空间从非旗舰升级到旗舰 → 自动创建分表（避免重复建表）
        if (Objects.equals(oldSpace.getSpaceType(), SpaceTypeEnum.TEAM.getValue())
                && !Objects.equals(oldSpace.getSpaceLevel(), SpaceLevelEnum.FLAGSHIP.getValue())
                && Objects.equals(space.getSpaceLevel(), SpaceLevelEnum.FLAGSHIP.getValue())) {
            space.setSpaceType(oldSpace.getSpaceType());
            space.setId(id);
            dynamicShardingManager.createSpacePictureTable(space);
        }

        // 修改配额后清分析缓存
        if (id != 0L) {
            pictureService.clearListCache(id);
        }
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取空间（仅管理员可用）
     */
    @NoUsed
    @GetMapping("/get")
    @AuthCheck(UserRoleEnum.ADMIN)
    public BaseResponse<Space> getSpaceById(long id, HttpServletRequest request) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Space space = spaceService.getById(id);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);


        // 获取封装类
        return ResultUtils.success(space);
    }

    /**
     * 根据 id 获取空间(用于显示具体的空间信息)
     */
    @GetMapping("/get/vo")
    public BaseResponse<SpaceVO> getSpaceVOById(long id, HttpServletRequest request) {

        //todo:用户只能查看自己的空间
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);


        // 查询数据库
        Space space = spaceService.getById(id);

        User loginUser = userService.getLoginUser(request);

        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);

        List<String> permissionList = spaceUserAuthManger.getPermissionList(space, loginUser);
        // 获取封装类
        SpaceVO spaceVO = spaceService.getSpaceVO(space, request);

        //设置权限
        spaceVO.setPermissionList(permissionList);
        return ResultUtils.success(spaceVO);
    }

    /**
     * 分页获取空间列表（仅管理员可用）
     */
    @PostMapping("/list/page")
    @AuthCheck(UserRoleEnum.ADMIN)
    public BaseResponse<Page<Space>> listSpaceByPage(@RequestBody SpaceQueryRequest spaceQueryRequest) {
        long current = spaceQueryRequest.getCurrent();
        long size = spaceQueryRequest.getPageSize();

        // 查询数据库
        Page<Space> spacePage = spaceService.page(new Page<>(current, size),
                spaceService.getQueryWrapper(spaceQueryRequest));
        return ResultUtils.success(spacePage);
    }

    /**
     * 用户用于查询自己的空间（查询自己的空间是否存在）
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<SpaceVO>> listSpaceVOByPage(@RequestBody SpaceQueryRequest spaceQueryRequest,
                                                         HttpServletRequest request) {

        long current = spaceQueryRequest.getCurrent();
        long size = spaceQueryRequest.getPageSize();
        /// 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        // 非管理员只能查看自己的空间，强制注入当前用户 ID
        User loginUser = userService.getLoginUser(request);
        if (!userService.isAdmin(loginUser)) {
            spaceQueryRequest.setUserId(loginUser.getId());
        }

        // 查询数据库
        Page<Space> spacePage = spaceService.page(new Page<>(current, size),
                spaceService.getQueryWrapper(spaceQueryRequest));
        // 获取封装类
        return ResultUtils.success(spaceService.getSpaceVOPage(spacePage, request));
    }


//    /**
//     * 分页获取空间列表(有缓存)（封装类）
//     */
//    @NoUsed("上线开启替换上层")
//    @PostMapping("/list/page/vo/cache")
//    public BaseResponse<Page<SpaceVO>> listSpaceVOByPageWithCache(@RequestBody SpaceQueryRequest spaceQueryRequest,
//                                                             HttpServletRequest request) {
//        long current = spaceQueryRequest.getCurrent();
//        long size = spaceQueryRequest.getPageSize();
//        /// 限制爬虫
//        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
//
//
//
//        Page<SpaceVO> spaceVoPage = spaceService.listSpaceVOByCache(spaceQueryRequest, request, current, size);
//        return ResultUtils.success(spaceVoPage);
//
//
//    }


    /**
     * 编辑空间（给用户使用）(逻辑比较简单并没有抽象到service进行)
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editSpace(@RequestBody SpaceEditRequest spaceEditRequest, HttpServletRequest request) {
        if (spaceEditRequest == null || spaceEditRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 在此处将实体类和 DTO 进行转换
        Space space = new Space();
        BeanUtils.copyProperties(spaceEditRequest, space);

        // 设置编辑时间
        space.setEditTime(new Date());//自动填充数据
        // 数据校验
        spaceService.validSpace(space, false);
        User loginUser = userService.getLoginUser(request);
        /// 判断是否存在
        long id = spaceEditRequest.getId();


        Space oldSpace = spaceService.getById(id);

        spaceService.checkSpaceAuth(loginUser, oldSpace);
//        ThrowUtils.throwIf(oldSpace == null, ErrorCode.NOT_FOUND_ERROR);
//        /// 仅本人或管理员可编辑
//        if (!oldSpace.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
//            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
//        }

        // 操作数据库
        boolean result = spaceService.updateById(space);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        if (id != 0L) {
            pictureService.clearListCache(id);
        }
        return ResultUtils.success(true);
    }

    /**
     * 查询所有级别
     * @return
     */
    @GetMapping("/list/level")
    public BaseResponse<List<SpaceLevel>> listSpaceLevel() {
        List<SpaceLevel> spaceLevelList = Arrays.stream(SpaceLevelEnum.values())
                .map(spaceLevelEnum -> new SpaceLevel(
                        spaceLevelEnum.getValue(),
                        spaceLevelEnum.getText(),
                        spaceLevelEnum.getMaxCount(),
                        spaceLevelEnum.getMaxSize()
                ))
                .collect(Collectors.toList());
        return ResultUtils.success(spaceLevelList);
    }




}
