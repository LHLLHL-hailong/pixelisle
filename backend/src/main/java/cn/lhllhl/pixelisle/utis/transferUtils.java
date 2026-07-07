package cn.lhllhl.pixelisle.utis;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.lhllhl.pixelisle.exception.BusinessException;
import cn.lhllhl.pixelisle.exception.ErrorCode;
import cn.lhllhl.pixelisle.model.dto.user.UserQueryRequest;
import cn.lhllhl.pixelisle.model.entity.User;
import cn.lhllhl.pixelisle.model.vo.LoginUserVo;
import cn.lhllhl.pixelisle.model.vo.UserVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.BeanUtils;

public class transferUtils {

   public static LoginUserVo  transferUserToLoginUserVo(User user){

        LoginUserVo loginUserVo = new LoginUserVo();
        BeanUtils.copyProperties(user,loginUserVo);
        loginUserVo.setUserRole(user.getUserRole());
        return loginUserVo;

    }
   public static UserVo transferUserToUserVo(User user){

        UserVo userVo = new UserVo();
        BeanUtils.copyProperties(user,userVo);
        return userVo;

    }




    public static  QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userName = userQueryRequest.getUserName();
        String userAccount = userQueryRequest.getUserAccount();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }



//    public QueryWrapper<user> getQueryWrapper(UserQueryRequest userQueryRequest) {
//        // 入参非空校验
//        if (userQueryRequest == null) {
//            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
//        }
//        // 提取查询参数
//        Long id = userQueryRequest.getId();
//        String userName = userQueryRequest.getUsername();
//        String userAccount = userQueryRequest.getUseraccount();
//        String userRole = userQueryRequest.getUserrole();
//        String sortField = userQueryRequest.getSortField();
//        String sortOrder = userQueryRequest.getSortOrder();
//
//        // 构建查询条件封装器
//        QueryWrapper<user> queryWrapper = new QueryWrapper<>();
//        // 精准匹配条件（非空时拼接）
//        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
//        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
//        // 模糊匹配条件（非空时拼接）
//        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
//        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
//
//        // 动态排序条件
//        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
//
//        return queryWrapper;
//    }

}
