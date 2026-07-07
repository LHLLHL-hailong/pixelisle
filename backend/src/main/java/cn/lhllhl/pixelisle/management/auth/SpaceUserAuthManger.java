package cn.lhllhl.pixelisle.management.auth;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import cn.lhllhl.pixelisle.management.auth.model.SpaceUserAuthConfig;
import cn.lhllhl.pixelisle.management.auth.model.SpaceUserPermissionConstant;
import cn.lhllhl.pixelisle.management.auth.model.SpaceUserRole;
import cn.lhllhl.pixelisle.model.entity.Space;
import cn.lhllhl.pixelisle.model.entity.SpaceUser;
import cn.lhllhl.pixelisle.model.entity.User;
import cn.lhllhl.pixelisle.model.enums.SpaceRoleEnum;
import cn.lhllhl.pixelisle.model.enums.SpaceTypeEnum;
import cn.lhllhl.pixelisle.service.SpaceUserService;
import cn.lhllhl.pixelisle.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SpaceUserAuthManger {

    public static final SpaceUserAuthConfig SPACE_USER_AUTH_CONFIG ;


    @Autowired
    private UserService userService;

    @Autowired
    private SpaceUserService spaceUserService;

    //项目启动的时候自动加载
    static {
        String json = ResourceUtil.readUtf8Str("biz/spaceUserAuthConfig.json");
        SPACE_USER_AUTH_CONFIG= JSONUtil.toBean(json, SpaceUserAuthConfig.class);
    }

    public List<String> getPermissionsByRole(String spaceUserRole){

        if (StrUtil.isEmpty(spaceUserRole)) {
            return new ArrayList<>();
        }

        SpaceUserRole spaceUserRole1 = SPACE_USER_AUTH_CONFIG.getRoles().stream().
                filter(role -> role.getKey().equals(spaceUserRole)).
                findFirst().orElse(null);
        if (spaceUserRole1 == null) {
            return new ArrayList<>();
        }
        return spaceUserRole1.getPermissions();
    }

    public List<String> getPermissionList(Space space, User loginUser) {
        if (loginUser == null) {
            return new ArrayList<>();
        }
        // 管理员权限
        List<String> ADMIN_PERMISSIONS = getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue());
        // 公共图库
        if (space == null) {
            if (userService.isAdmin(loginUser)) {
                return ADMIN_PERMISSIONS;
            }
            //fixme:这里可能画蛇添足
            List<String> cans=new ArrayList<>();
            cans.add(SpaceUserPermissionConstant.PICTURE_VIEW);
            cans.add(SpaceUserPermissionConstant.PICTURE_UPLOAD);

            return cans;
        }



        SpaceTypeEnum spaceTypeEnum = SpaceTypeEnum.getEnumByValue(space.getSpaceType());
        if (spaceTypeEnum == null) {
            return new ArrayList<>();
        }
        // 根据空间获取对应的权限
        switch (spaceTypeEnum) {
            case PRIVATE:
                // 私有空间，仅本人或管理员有所有权限（排除成员管理，因为私人空间没有成员）
                if (space.getUserId().equals(loginUser.getId()) || userService.isAdmin(loginUser)) {
                    List<String> privatePermissions = new ArrayList<>(ADMIN_PERMISSIONS);
                    privatePermissions.remove(SpaceUserPermissionConstant.SPACE_USER_MANAGE);
                    return privatePermissions;
                } else {
                    return new ArrayList<>();
                }
            case TEAM:
                // 团队空间，查询 SpaceUser 并获取角色和权限
                SpaceUser spaceUser = spaceUserService.lambdaQuery()
                        .eq(SpaceUser::getSpaceId, space.getId())
                        .eq(SpaceUser::getUserId, loginUser.getId())
                        .one();
                if (spaceUser == null) {
                    return new ArrayList<>();
                } else {
                    return getPermissionsByRole(spaceUser.getSpaceRole());
                }
        }
        return new ArrayList<>();
    }





}
