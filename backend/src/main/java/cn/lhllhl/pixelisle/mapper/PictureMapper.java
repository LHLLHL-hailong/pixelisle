package cn.lhllhl.pixelisle.mapper;

import cn.lhllhl.pixelisle.model.dto.picture.PictureScheduleDeleteBean;
import cn.lhllhl.pixelisle.model.entity.Picture;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

import java.util.List;

/**
* @description 针对表【picture】的数据库操作Mapper
* @Entity cn.lhllhl.pixelisle.model.entity.Picture
*/
public interface PictureMapper extends BaseMapper<Picture> {


    /**
     * 定期删除过期
     * @param currentMaxId 》=
     * @param batchSize    nums
     * @return
     */
    List<PictureScheduleDeleteBean> selectPictureScheduleList(Long currentMaxId,Integer batchSize);


    /**
     *
     * @param minId  》=
     * @param maxId  《=
     */
    void deleteByRangeidAndisDeleted(Long minId,Long maxId);

}




