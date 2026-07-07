package cn.lhllhl.pixelisle.controller;

import cn.lhllhl.pixelisle.annotation.AuthCheck;
import cn.lhllhl.pixelisle.common.BaseResponse;
import cn.lhllhl.pixelisle.common.DeleteRequest;
import cn.lhllhl.pixelisle.common.ResultUtils;
import cn.lhllhl.pixelisle.exception.BusinessException;
import cn.lhllhl.pixelisle.exception.ErrorCode;
import cn.lhllhl.pixelisle.exception.ThrowUtils;
import cn.lhllhl.pixelisle.model.dto.user.*;
import cn.lhllhl.pixelisle.model.entity.User;
import cn.lhllhl.pixelisle.model.enums.UserRoleEnum;
import cn.lhllhl.pixelisle.model.vo.LoginUserVo;
import cn.lhllhl.pixelisle.model.vo.UserVo;
import cn.lhllhl.pixelisle.service.UserService;
import cn.lhllhl.pixelisle.utis.transferUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@Slf4j
@RequestMapping("/user")
@Api(tags="用户接口")
public class UserController {


    @Autowired
    private UserService userService;



    @PostMapping("/register")
    @ApiOperation("用户注册接口")
    BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest==null, ErrorCode.PARAMS_ERROR);

        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String userAccount = userRegisterRequest.getUserAccount();

        log.info("用户注册触发 obj  {}", userAccount);

        long l = userService.userRegister(userAccount, userPassword, checkPassword);

        log.info("user register success id {}",l);


        return ResultUtils.success(l);


    }


    @ApiOperation("用户登录")
    @PostMapping("/login")
    public BaseResponse<LoginUserVo> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {

        ThrowUtils.throwIf(userLoginRequest==null,ErrorCode.PARAMS_ERROR);

        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();


        LoginUserVo loginUserVo = userService.userLogin(userAccount, userPassword, request);


        log.error("user login success id {}", loginUserVo);


        return ResultUtils.success(loginUserVo);



    }

    @ApiOperation("获取当前登录用户信息")
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVo> getLoginUser(HttpServletRequest request) {

        User loginUser = userService.getLoginUser(request);


        LoginUserVo loginUserVo = new LoginUserVo();

        BeanUtils.copyProperties(loginUser, loginUserVo);


        return ResultUtils.success(loginUserVo);


    }


    @ApiOperation("用户登出")
@PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {


        userService.userLogout(request);

        return ResultUtils.success(true);
    }

    @PostMapping("/add")
    @ApiOperation("管理员测试添加用户")
    @AuthCheck(UserRoleEnum.ADMIN)
    BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        String pass="123456";
        if(userAddRequest==null){
            throw  new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        User now = new User();

        BeanUtils.copyProperties(userAddRequest, now);


        boolean save = userService.save(now);



        return ResultUtils.success(now.getId());


    }

    @GetMapping("/get")
    @AuthCheck(UserRoleEnum.ADMIN)
    public BaseResponse<User> getUserById(Long id) {
        ThrowUtils.throwIf(id==null,ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(id<0,ErrorCode.PARAMS_ERROR);
        User byId = userService.getById(id);

        return ResultUtils.success(byId);

    }


    @GetMapping("/get/vo")
    public BaseResponse<UserVo > getById(Long id){
        ThrowUtils.throwIf(id==null,ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(id<0,ErrorCode.PARAMS_ERROR);
        User byId = userService.getById(id);

        return ResultUtils.success(transferUtils.transferUserToUserVo(byId));

    }


    @PostMapping("/delete")
    @AuthCheck(UserRoleEnum.ADMIN)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest){


        if(deleteRequest==null || deleteRequest.getId()==null || deleteRequest.getId()<0){
            throw  new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        boolean b = userService.removeById(deleteRequest.getId());

        return ResultUtils.success(b);

    }

    /**
     * 更新用户
     */
    @PostMapping("/update")
    @AuthCheck(UserRoleEnum.ADMIN)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    @ApiOperation("兑换会员码")
    @PostMapping("/exchange/vip")
    public BaseResponse<Boolean> exchangeVip(@RequestBody UserVipExchangeRequest request, HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(request == null || request.getVipCode() == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpRequest);
        boolean result = userService.exchangeVip(loginUser, request.getVipCode());
        return ResultUtils.success(result);
    }

    @PostMapping("/list/page/vo")
    public BaseResponse<Page<UserVo>> listUserByRole(@RequestBody UserQueryRequest userQueryRequest){

        log.info("{}",userQueryRequest);

        if(userQueryRequest == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Page<User> userPage = new Page<>(userQueryRequest.getCurrent(), userQueryRequest.getPageSize());

        Page<User> page = userService.page(userPage, transferUtils.getQueryWrapper(userQueryRequest));


        Page<UserVo> result = new Page<>(userQueryRequest.getCurrent(), userQueryRequest.getPageSize());
        result.setTotal(page.getTotal());

        List<UserVo> collect = page.getRecords().stream().map(obj -> {


            UserVo userVo = new UserVo();
            BeanUtils.copyProperties(obj, userVo);
            return userVo;
        }).collect(Collectors.toList());

        result.setRecords(collect);

        return ResultUtils.success(result);


    }







}
