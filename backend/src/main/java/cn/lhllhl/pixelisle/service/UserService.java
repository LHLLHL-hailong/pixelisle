package cn.lhllhl.pixelisle.service;

import cn.lhllhl.pixelisle.model.entity.User;
import cn.lhllhl.pixelisle.model.vo.LoginUserVo;
import cn.lhllhl.pixelisle.model.vo.UserVo;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpServletRequest;

/**
* @description 针对表【user】的数据库操作Service
* @createDate 2026-02-25 22:05:38
*/
public interface UserService extends IService<User> {


    /**
     * 用户注册
     * @param userAccount
     * @param userPassword
     * @param checkPassword
     * @return
     */
    public long userRegister(String userAccount, String userPassword, String checkPassword);


    /**
     * 用户登录接口
     * @param userAccount
     * @param userPassword
     * @param request
     * @return
     */
    LoginUserVo userLogin(String userAccount, String userPassword, HttpServletRequest request);


    /**
     * 获取当前登录用户的信息
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);


    /**
     * 用户注销
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);


    /**
     * 判断用户是不是管理员
     *
     * @param user
     * @return
     */
    boolean isAdmin(User user);

    /**
     * 对用户进行脱敏
     *
     * @param user
     * @return
     */
    UserVo getUserVO(User user);


    /**
     * 用户兑换会员（会员码兑换）
     */
    boolean exchangeVip(User user, String vipCode);
}
