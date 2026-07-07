package cn.lhllhl.pixelisle.model.dto.space;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 空间级别
 */
@Data
@AllArgsConstructor
public class SpaceLevel {

    /**
     * 值
     */
    private int value;

    private String text;

    private long maxCount;

    private long maxSize;
}