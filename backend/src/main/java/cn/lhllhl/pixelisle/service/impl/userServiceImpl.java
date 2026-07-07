package cn.lhllhl.pixelisle.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.lhllhl.pixelisle.constant.UserConstant;
import cn.lhllhl.pixelisle.exception.BusinessException;
import cn.lhllhl.pixelisle.exception.ErrorCode;
import cn.lhllhl.pixelisle.management.auth.StpKit;
import cn.lhllhl.pixelisle.model.enums.UserRoleEnum;
import cn.lhllhl.pixelisle.model.vo.LoginUserVo;
import cn.lhllhl.pixelisle.model.vo.UserVo;
import cn.lhllhl.pixelisle.utis.transferUtils;
import cn.lhllhl.pixelisle.utis.md5Utils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cn.lhllhl.pixelisle.model.entity.User;
import cn.lhllhl.pixelisle.service.UserService;
import cn.lhllhl.pixelisle.mapper.userMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;


/**
* @description 针对表【user】的数据库操作Service实现
* @createDate 2026-02-25 22:05:38
*/
@Service
public class userServiceImpl extends ServiceImpl<userMapper, User>
    implements UserService {




    public static final String LogIN="log_in_user";






    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {

        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        if(!checkPassword.equals(userPassword)){
            throw new BusinessException(40001,"两次密码不一致");
        }

        //1. 进行参数校验


        QueryWrapper<User> userAccount1 = new QueryWrapper<User>().eq("userAccount", userAccount);

        long count = this.count(userAccount1);

        if (count > 0) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR);
        }
        //2. 查询是否存在
        //3. 进行密码的加密
        String newPass = md5Utils.get(userPassword);

        User build = User.builder().userPassword(newPass).userName(userAccount).userRole(UserRoleEnum.USER.getValue()).userAccount(userAccount).build();

        //.createtime(new Date()).updatetime(new Date()).edittime(new Date())



            this.save(build);


        //4. 插入数据库
        return build.getId();
    }

    @Override
    public LoginUserVo userLogin(String userAccount, String userPassword, HttpServletRequest request) {

        if (StrUtil.hasBlank(userAccount,userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        QueryWrapper<User> userAccount1 = new QueryWrapper<User>().eq("userAccount", userAccount);

        User one = this.getOne(userAccount1);

        if(one==null){
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"用户不存在或密码错误");
        }


        String password = md5Utils.get(userPassword);

        if(!password.equals(one.getUserPassword())){

            throw new BusinessException(ErrorCode.OPERATION_ERROR,"用户不存在或密码错误");

        }

        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, one);
        StpKit.SPACE.login(one.getId());
        StpKit.SPACE.getSession().set(UserConstant.USER_LOGIN_STATE, one);

        return transferUtils.transferUserToLoginUserVo(one);

    }

    @Override
    public User getLoginUser(HttpServletRequest request) {

        User obj = (User) request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if(obj==null || obj.getId()==null){

            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }


        Long id = obj.getId();
        User user = this.baseMapper.selectById(id);

        return user;

    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        Object attribute = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);


        if(attribute==null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }

        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);

        return true;

    }

    @Override
    public boolean isAdmin(User user) {

        if (user == null) {
            return false;
        }


        String value = UserRoleEnum.ADMIN.getValue();


        return value.equals(user.getUserRole());
    }

    @Override
    public UserVo getUserVO(User user) {



        UserVo userVo = new UserVo();


        BeanUtils.copyProperties(user, userVo);


        return userVo;


    }

    /**
     * 兑换会员码
     * @param user
     * @param vipCode
     * @return
     */
    /** JSON 会员码配置文件路径 */
    private static final String VIP_CODE_PATH = "biz/vipCode.json";
    /** 单码锁池：key=会员码, value=该码的锁对象。用完后 remove 防止泄漏 */
    private static final java.util.concurrent.ConcurrentHashMap<String, Object> CODE_LOCKS = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    public boolean exchangeVip(User user, String vipCode) {
        if (user == null || StrUtil.isBlank(vipCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数错误");
        }
        // ── 1. 读-验-写 JSON 文件（锁在单个码上，不同码可并发）──
        Object lock = CODE_LOCKS.computeIfAbsent(vipCode, k -> new Object());
        synchronized (lock) {
            try {
                // 1. 读取 JSON 文件（用 getResourceAsStream 兼容 JAR 包内读取）
                cn.hutool.json.JSONArray arr;
                try (java.io.InputStream is = getClass().getClassLoader().getResourceAsStream(VIP_CODE_PATH)) {
                    if (is == null) {
                        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "会员码配置文件不存在");
                    }
                    String content = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    arr = cn.hutool.json.JSONUtil.parseArray(content);
                } catch (Exception e) {
                    log.error("读取会员码文件失败", e);
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统错误");
                }

                // 2. 查找 + 校验
                cn.hutool.json.JSONObject target = null;
                int targetIndex = -1;
                for (int i = 0; i < arr.size(); i++) {
                    cn.hutool.json.JSONObject obj = arr.getJSONObject(i);
                    if (vipCode.equals(obj.getStr("code"))) {
                        target = obj;
                        targetIndex = i;
                        break;
                    }
                }
                if (target == null) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "无效的会员码");
                }
                if (Boolean.TRUE.equals(target.getBool("hasUsed"))) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "该会员码已被使用");
                }

                // 3. 标记 + 写回
                target.set("hasUsed", true);
                arr.set(targetIndex, target);
                try {
                    String updated = cn.hutool.json.JSONUtil.toJsonPrettyStr(arr);
                    java.net.URL url = getClass().getClassLoader().getResource(VIP_CODE_PATH);
                    if (url != null && "file".equals(url.getProtocol())) {
                        java.nio.file.Files.write(java.nio.file.Paths.get(url.toURI()),
                                updated.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    } else {
                        log.error("无法写入会员码文件：资源不在文件系统中");
                        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统错误");
                    }
                } catch (Exception e) {
                    log.error("写入会员码文件失败", e);
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "系统错误");
                }
            } finally {
                CODE_LOCKS.remove(vipCode); // 用完清理，防止锁池膨胀
            }
        } // synchronized end

        // ── 4. 更新用户：角色 → VIP，有效期 → 1年后，记录兑换码 ──
        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setUserRole(UserRoleEnum.VIP.getValue());
        // 计算 1 年后（如果已有有效期则叠加，否则从当前时间算）
        long now = System.currentTimeMillis();
        long expireMs;
        if (user.getVipExpireTime() != null && user.getVipExpireTime().getTime() > now) {
            // 已有有效期：叠加 1 年
            expireMs = user.getVipExpireTime().getTime() + 365L * 24 * 3600 * 1000;
        } else {
            expireMs = now + 365L * 24 * 3600 * 1000;
        }
        updateUser.setVipExpireTime(new java.util.Date(expireMs));
        updateUser.setVipCode(vipCode);
        // 生成会员编号
        updateUser.setVipNumber(System.currentTimeMillis() % 100000000 + 10000000);
        boolean updated = this.updateById(updateUser);
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "更新用户失败");
        }

        // 同步更新内存中的 user 对象
        user.setUserRole(updateUser.getUserRole());
        user.setVipExpireTime(updateUser.getVipExpireTime());
        user.setVipCode(updateUser.getVipCode());
        user.setVipNumber(updateUser.getVipNumber());

        return true;
    }


}




