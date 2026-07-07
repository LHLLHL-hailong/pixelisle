package cn.lhllhl.pixelisle.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 会员码兑换请求
 */
@Data
public class UserVipExchangeRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String vipCode;
}
