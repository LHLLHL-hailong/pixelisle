package cn.lhllhl.pixelisle.service;

import cn.lhllhl.pixelisle.model.dto.spaceuser.SpaceUserAddRequest;
import cn.lhllhl.pixelisle.model.dto.spaceuser.SpaceUserQueryRequest;
import cn.lhllhl.pixelisle.model.entity.SpaceUser;
import cn.lhllhl.pixelisle.model.vo.SpaceUserVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @description 针对表【space_user】的数据库操作Service
* @createDate 2026-04-25 18:48:44
*/
public interface SpaceUserService extends IService<SpaceUser> {


    /**
     * 创建空间成员
     *
     * @param spaceAddRequest
     * @return
     */
    Long addSpaceUser(SpaceUserAddRequest spaceAddRequest);

    /**
     * 校验空间成员
     *
     * @param spaceUser
     * @param isAdd
     */
    void validSpaceUser(SpaceUser spaceUser, boolean isAdd);


    /**
     * 普普通通的由DTO获取对应的查询的wrapper
     *
     * @param spaceQueryRequest
     * @return
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceQueryRequest);


    /**
     * 获取空间成员包装类（关联 Space 和 User信息）
     *
     * @param spaceUser
     * @param request
     * @return
     */
    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);


    /**
     * 获取分页查询SpaceUser(多条列表)
     *
     * @param spaceUserList
     * @return
     */
    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList) ;




}
