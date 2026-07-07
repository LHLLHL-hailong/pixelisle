package cn.lhllhl.pixelisle.utis;

import lombok.extern.slf4j.Slf4j;

/**
 * 颜色转换工具类
 */
@Slf4j
public class ColorTransformUtils {

    private ColorTransformUtils() {
        // 工具类不需要实例化
    }

    /**
     * 将腾讯云数据万象返回的非标准 RGB 十六进制值
     * 补全为标准的 6 位 {@code 0xRRGGBB} 格式。
     *
     * <h3>问题背景</h3>
     * imageAve 等接口在颜色分量含前导零时，各分量独立转十六进制后
     * 直接拼接，导致返回 3~5 位十六进制：
     * <pre>
     *   R=0xA1, G=0x00, B=0xB2
     *   → hex(R)="a1" + hex(G)="0" + hex(B)="b2" = "a10b2"  (5 chars)
     * </pre>
     *
     * <h3>解析策略</h3>
     * 从左到右逐分量恢复。由于值 0 的 hex 为 "0"，而值 16~255 为 2 chars：
     * <ul>
     *   <li>剩余字符数 == 剩余分量数 → 每分量 1 char</li>
     *   <li>当前字符为 '0' → 该分量为 0x00，消费 1 char</li>
     *   <li>否则 → 消费 2 chars</li>
     * </ul>
     *
     * @param color 原始颜色值，如 "0xA10B2"、"0x0AB0"、"0x000"、"0x736246"
     * @return 标准 6 位格式，如 "0xa100b2"，入参为 null 时返回 "0x000000"
     */
    public static String getStandardColor(String color) {
        if (color == null || color.isEmpty()) {
            return "0x000000";
        }

        // 去掉 0x / 0X 前缀
        String hex = color;
        if (hex.startsWith("0x") || hex.startsWith("0X")) {
            hex = hex.substring(2);
        }

        int len = hex.length();

        // 标准 6 位：直接返回
        if (len == 6) {
            return "0x" + hex.toLowerCase();
        }

        // 非标准位数：打 warn 日志，记录原始值便于排查
        log.warn("imageAve returned non-standard hex: raw={}, hexLen={}, hexBody={}",
                color, len, hex);

        // 逐分量解析 R、G、B
        int[] rgb = parseCompactHex(hex);

        return String.format("0x%02x%02x%02x", rgb[0], rgb[1], rgb[2]);
    }

    /**
     * 解析紧凑格式（3~5 位）的十六进制字符串为 R、G、B 三个分量。
     *
     * <p>编码规则：每个分量独立调用 Integer.toHexString()
     * 后拼接。值 0~15 产生 1 char，值 16~255 产生 2 chars。
     *
     * @param hex 不含 "0x" 前缀的纯十六进制字符串
     * @return int[3] 数组，依次为 R、G、B（0~255）
     */
    private static int[] parseCompactHex(String hex) {
        int[] result = new int[3];
        int pos = 0;
        int len = hex.length();

        for (int comp = 0; comp < 3; comp++) {
            int remaining = len - pos;
            int remainingComps = 3 - comp;

            boolean singleChar;

            if (remaining <= 0) {
                // 理论上不会走到这里，兜底
                result[comp] = 0;
                continue;
            }

            if (remaining == remainingComps) {
                // 剩余字符刚好够每分量 1 char → 全部按 1 char 处理
                singleChar = true;
            } else if (hex.charAt(pos) == '0') {
                // 当前字符是 '0' → 是独立分量，值为 0x00
                singleChar = true;
            } else {
                // 默认消费 2 chars
                singleChar = false;
            }

            if (singleChar) {
                result[comp] = Integer.parseInt(hex.substring(pos, pos + 1), 16);
                pos += 1;
            } else {
                result[comp] = Integer.parseInt(hex.substring(pos, pos + 2), 16);
                pos += 2;
            }
        }

        return result;
    }
}