package cn.lhllhl.pixelisle.service;


import cn.lhllhl.pixelisle.model.dto.space.SpaceAddRequest;
import cn.lhllhl.pixelisle.model.dto.space.SpaceQueryRequest;
import cn.lhllhl.pixelisle.model.entity.Space;
import cn.lhllhl.pixelisle.model.entity.User;
import cn.lhllhl.pixelisle.model.vo.SpaceVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import javax.servlet.http.HttpServletRequest;

/**
* @description 针对表【space】的数据库操作Service
* @createDate 2026-03-14 21:14:45
*/
public interface SpaceService extends IService<Space> {

    /**
     * 创建空间
     *
     * @param spaceAddRequest
     * @param loginUser
     * @return
     */
    Long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    /**
     * 校验
     *
     * @param space
     * @param isAdd
     */
    void validSpace(Space space, boolean isAdd);


    /**
     * 普普通通的由DTO获取对应的查询的wrapper
     *
     * @param spaceQueryRequest
     * @return
     */
    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);


    /**
     * 获取patrue对象（含有user）(单条)
     *
     * @param space
     * @param request
     * @return
     */
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);


    /**
     * 获取分页查询Space(多条)
     *
     * @param spacePage
     * @param request
     * @return
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);


    /**
     * 根据空间级别填充空间对象
     *
     * @param space
     */
    void fillSpaceBySpaceLevel(Space space);


    /**
     * 校验默认是否含有某个空间
     *
     * @param loginUser
     * @param space
     */
    void checkSpaceAuth(User loginUser, Space space);


}
