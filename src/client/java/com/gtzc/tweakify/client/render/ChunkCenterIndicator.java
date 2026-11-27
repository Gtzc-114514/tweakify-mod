package com.gtzc.tweakify.client.render;

import net.minecraft.core.SectionPos;
import net.minecraft.world.phys.Vec3;

/**
 * 区块中心指示：原版区块边界（F3+G）渲染时，在当前区块中心补一条竖直指示线。
 *
 * <p>只负责几何计算，实际提交由 {@code ChunkBorderRendererChunkCenterMixin} 调用
 * {@code Gizmos.line} 完成；区块边界关闭时原版渲染器不会运行，因此本功能也自然不显示。
 */
public final class ChunkCenterIndicator {
	// 与区块边界的主线颜色区分：白色、略透明，竖线更容易和青色/黄色网格分开。
	public static final int LINE_COLOR = 0xFFFFFFFF;
	public static final float LINE_WIDTH = 2.0F;
	// 区块中心相对区块最小坐标的偏移：16 / 2。
	public static final double CENTER_OFFSET = 8.0;

	private ChunkCenterIndicator() {
	}

	// 当前区块中心的竖线：从世界最低点到最高点，x/z 固定在区块中心。
	public static Vec3[] centerLine(SectionPos section, int minY, int maxY) {
		double x = section.minBlockX() + CENTER_OFFSET;
		double z = section.minBlockZ() + CENTER_OFFSET;
		return new Vec3[]{
			new Vec3(x, minY, z),
			new Vec3(x, maxY + 1.0, z)
		};
	}
}
