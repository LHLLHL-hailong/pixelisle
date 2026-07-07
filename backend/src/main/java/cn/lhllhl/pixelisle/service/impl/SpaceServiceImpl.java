package cn.lhllhl.pixelisle.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.lhllhl.pixelisle.exception.BusinessException;
import cn.lhllhl.pixelisle.exception.ErrorCode;
import cn.lhllhl.pixelisle.exception.ThrowUtils;
import cn.lhllhl.pixelisle.management.sharding.DynamicShardingManager;
import cn.lhllhl.pixelisle.mapper.SpaceMapper;
import cn.lhllhl.pixelisle.model.dto.space.SpaceAddRequest;
import cn.lhllhl.pixelisle.model.dto.space.SpaceQueryRequest;
import cn.lhllhl.pixelisle.model.entity.Space;
import cn.lhllhl.pixelisle.model.entity.SpaceUser;
import cn.lhllhl.pixelisle.model.entity.User;
import cn.lhllhl.pixelisle.model.enums.SpaceLevelEnum;
import cn.lhllhl.pixelisle.model.enums.SpaceRoleEnum;
import cn.lhllhl.pixelisle.model.enums.SpaceTypeEnum;
import cn.lhllhl.pixelisle.model.enums.UserRoleEnum;
import cn.lhllhl.pixelisle.model.vo.SpaceVO;
import cn.lhllhl.pixelisle.model.vo.UserVo;
import cn.lhllhl.pixelisle.service.SpaceService;
import cn.lhllhl.pixelisle.service.SpaceUserService;
import cn.lhllhl.pixelisle.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
* @description 针对表【space】的数据库操作Service实现
* @createDate 2026-03-14 21:14:45
*/
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
    implements SpaceService{

    @Autowired
    private UserService userService;


    @Autowired
    private SpaceUserService spaceUserService;


    @Autowired
    TransactionTemplate transactionTemplate;

    ConcurrentHashMap<Long, Object> spaceMap = new ConcurrentHashMap<>();
    @Autowired
    private DynamicShardingManager dynamicShardingManager;


    @Override
    public Long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {

        ThrowUtils.throwIf(spaceAddRequest == null, ErrorCode.PARAMS_ERROR, "创建参数不能为空");
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);

        Space space = new Space();



        /// 拷贝参数
        BeanUtils.copyProperties(spaceAddRequest, space);

        if(StrUtil.isEmpty( space.getSpaceName())){
            space.setSpaceName("default space");
        }

        if(space.getSpaceLevel()==null){
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }

        /// 自动参数校验
        space.setUserId(loginUser.getId());
        space.setCreateTime(new Date());
        space.setEditTime(new Date());

        /// 如果空间类型为空自动添加私人类型
        if(space.getSpaceType()==null){
            space.setSpaceType(SpaceTypeEnum.PRIVATE.getValue());
        }



        /// 基础参数校验
        this.validSpace(space, false);///因为为空的时候会添加默认值，所以这里不校验二者是否为空

        SpaceLevelEnum level = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());

        /// 权限校验：普通版所有人可建，专业版VIP/管理员可建，旗舰版仅管理员
        if (level != SpaceLevelEnum.COMMON) {
            String userRole = loginUser.getUserRole();
            if (SpaceLevelEnum.FLAGSHIP.equals(level)) {
                // 旗舰版：仅管理员
                if (!userService.isAdmin(loginUser)) {
                    throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "旗舰版仅管理员可创建");
                }
            } else {
                // 专业版：VIP 或管理员
                if (!userService.isAdmin(loginUser)
                        && !UserRoleEnum.VIP.getValue().equals(userRole)) {
                    throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "专业版仅VIP会员可创建，请先兑换会员");
                }
            }
        }


        /// 自动填充额度
        this.fillSpaceBySpaceLevel(space);

       // String lock = String.valueOf(loginUser.getId()).intern();

        Object lockObj = spaceMap.computeIfAbsent(loginUser.getId(), k -> new Object());

        /// 互斥性
        synchronized (lockObj) {


            Long result=transactionTemplate.execute(status -> {


                //fixme:这里字段名称可能出问题
                boolean count = this.exists(new QueryWrapper<Space>().eq("spaceType",spaceAddRequest.getSpaceType()).eq("userId", loginUser.getId()));

                if (count) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR,"每人只能创建一个空间");
                }
                this.save(space);

                spaceMap.remove(loginUser.getId());

                /// 创建的空间如果是团队空间就自动设置创建者为管理员
                if (space.getSpaceType().equals(SpaceTypeEnum.TEAM.getValue())) {
                    SpaceUser spaceUser = new SpaceUser();
                    spaceUser.setSpaceId(space.getId());
                    spaceUser.setUserId(loginUser.getId());
                    spaceUser.setSpaceRole(SpaceRoleEnum.ADMIN.getValue());
                    spaceUser.setCreateTime(new Date());

                    boolean isIn = spaceUserService.save(spaceUser);
                    if(!isIn){
                        throw new BusinessException(ErrorCode.OPERATION_ERROR,"自动创建团队空间管理员失败");
                    }

                    dynamicShardingManager.createSpacePictureTable(space);

                }


                return space.getId();

            });

            return result;

        }

    }

    /**
     * 注意：创建空间 和 修改空间 的校验规则不一致
     * 创建空间的时候空间名称不能为空
     * （注意:不检查权限和内存使用）
     *
     * @param space
     * @param isAdd
     */
    @Override
    public void validSpace(Space space, boolean isAdd) {

        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR, "创建的空间不能为空");


        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();

        //校验的时候先校验对应的空间的类型
        //Integer spaceType = space.getSpaceType();
        SpaceLevelEnum spaceLevelType = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());

//        Long maxSize = space.getMaxSize();
//        Long maxCount = space.getMaxCount();
//        Long totalSize = space.getTotalSize();
//        Long totalCount = space.getTotalCount();
        if (isAdd) {
            ThrowUtils.throwIf(spaceName == null, ErrorCode.PARAMS_ERROR, "创建的空间名称不能为空");
            ThrowUtils.throwIf(spaceLevel == null, ErrorCode.PARAMS_ERROR, "创建的空间级别不能为空");
            ThrowUtils.throwIf(spaceLevelType==null,ErrorCode.PARAMS_ERROR,"创建的空间级别不存在");
            ThrowUtils.throwIf(SpaceTypeEnum.getEnumByValue(space.getSpaceType())==null,ErrorCode.PARAMS_ERROR,"创建的空间类型不存在");

//            SpaceLevelEnum level = SpaceLevelEnum.getEnumByValue(spaceLevel);
//
//            if (level == null) {
//            }
        }
        if (!StrUtil.isEmpty(spaceName) && spaceName.length() > 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称过长");
        }


    }

    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        if (spaceQueryRequest == null) {
            return queryWrapper;
        }


        Long id = spaceQueryRequest.getId();
        Long userId = spaceQueryRequest.getUserId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();
        Integer spaceType = spaceQueryRequest.getSpaceType();

        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "spaceName", spaceName);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceLevel), "spaceLevel", spaceLevel);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceType), "spaceType", spaceType);
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    /**
     * 返回值封装User
     *
     * @param space
     * @param request
     * @return
     */
    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        // 对象转封装类
        SpaceVO spaceVO = SpaceVO.objToVo(space);

        // 关联查询用户信息
        Long userId = space.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVo userVO = userService.getUserVO(user);
            spaceVO.setUser(userVO);
        }

        return spaceVO;
    }


    /**
     * 分页数据封装用户信息
     *
     * @param spacePage
     * @param request
     * @return
     */
    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        List<Space> spaceList = spacePage.getRecords();
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        if (CollUtil.isEmpty(spaceList)) {
            return spaceVOPage;
        }
        // 对象列表 => 封装对象列表
        List<SpaceVO> spaceVOList = spaceList.stream().map(SpaceVO::objToVo).collect(Collectors.toList());
        // 1. 关联查询用户信息
        Set<Long> userIdSet = spaceList.stream().map(Space::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        spaceVOList.forEach(spaceVO -> {
            Long userId = spaceVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceVO.setUser(userService.getUserVO(user));
        });
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }

    /**
     * 注意：如果管理员已经设置了最大count和最大size则不会自动填充
     *
     * @param space
     */
    @Override
    public void fillSpaceBySpaceLevel(Space space) {

        Integer level = space.getSpaceLevel();

        SpaceLevelEnum levelEnum = SpaceLevelEnum.getEnumByValue(level);

        //存在空间级别才进行填充
        if (levelEnum != null) {

            Long maxCount = space.getMaxCount();
            Long maxSize = space.getMaxSize();

            if (maxCount == null) {/// 管理员设置了就不填充
                space.setMaxCount(levelEnum.getMaxCount());
            }

            if (maxSize == null) {/// 管理员设置了就不填充
                space.setMaxSize(levelEnum.getMaxSize());
            }

        }


    }

    @Override
    public void checkSpaceAuth(User loginUser, Space space) {

        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "请求空间不存在");

        if (!Objects.equals(space.getUserId(), loginUser.getId()) && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);

        }

    }
}




