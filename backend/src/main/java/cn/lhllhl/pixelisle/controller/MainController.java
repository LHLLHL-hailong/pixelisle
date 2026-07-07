package cn.lhllhl.pixelisle.controller;

import cn.lhllhl.pixelisle.annotation.AuthCheck;
import cn.lhllhl.pixelisle.common.BaseResponse;
import cn.lhllhl.pixelisle.common.ResultUtils;
import cn.lhllhl.pixelisle.model.enums.UserRoleEnum;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api(tags = "测试接口")
public class MainController {

    /**
     * 健康检查接口
     * @return
     */
    @GetMapping("/health")
    @ApiOperation("健康接口")
    @AuthCheck(UserRoleEnum.ADMIN)
    public BaseResponse<String> health(){
        return ResultUtils.success("ok");
    }
}
