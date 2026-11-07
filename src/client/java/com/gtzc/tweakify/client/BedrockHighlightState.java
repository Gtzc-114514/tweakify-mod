package com.gtzc.tweakify.client;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 单层基岩高亮状态：记录哪些面朝向空气，供渲染层填充标记。
 * 邻接方块挡住的面不标记，避免无谓的绘制。
 * 扫描由 {@link BlockMarkerScanner} 与屏障可见共用一次方块遍历。
 */
public final class BedrockHighlightState {
	// 一个需要标记的单层基岩：顶/底面是否暴露（邻接空气）。
	public record Marker(BlockPos pos, boolean topExposed, boolean bottomExposed) {
	}

	private static List<Marker> markers = List.of();

	private BedrockHighlightState() {
	}

	// 当前需要标记的方块（只读）。
	public static List<Marker> markers() {
		return markers;
	}

	// 纯判定：自身是基岩且上下都不是基岩 -> 该列只有一层。
	public static boolean isSingleLayer(boolean bedrock, boolean aboveBedrock, boolean belowBedrock) {
		return bedrock && !aboveBedrock && !belowBedrock;
	}

	// 纯判定：相机在顶面之上才能看到顶面，在底面之下才能看到底面。
	public static boolean isFaceVisibleFrom(boolean topFace, double cameraY, int blockY) {
		return topFace ? cameraY > blockY + 1.0 : cameraY < blockY;
	}

	// 计算某个位置的标记；不是单层基岩或两面都被方块挡住时返回 null。
	// 自身状态由调用方传入（同一位置只查一次），只有候选方块才会再去查上下邻居。
	static Marker markerAt(BlockGetter level, BlockPos pos, BlockState state) {
		if (!state.is(Blocks.BEDROCK)) {
			return null;
		}
		BlockState above = level.getBlockState(pos.above());
		if (above.is(Blocks.BEDROCK)) {
			return null;
		}
		BlockState below = level.getBlockState(pos.below());
		if (below.is(Blocks.BEDROCK)) {
			return null;
		}
		boolean topExposed = above.isAir();
		boolean bottomExposed = below.isAir();
		return topExposed || bottomExposed ? new Marker(pos.immutable(), topExposed, bottomExposed) : null;
	}

	// 接收扫描结果：超出上限时保留离玩家最近的标记，避免扫描窗口角落抢占名额。
	static void setMarkers(List<Marker> found, BlockPos center, int maxMarkers) {
		List<Marker> accepted = found;
		if (accepted.size() > maxMarkers) {
			List<Marker> sorted = new ArrayList<>(accepted);
			sorted.sort(Comparator.comparingDouble(marker -> marker.pos().distSqr(center)));
			accepted = sorted.subList(0, maxMarkers);
		}
		markers = List.copyOf(accepted);
	}

	// 功能关闭时清空标记（避免残留上一帧的几何）。
	static void clear() {
		if (!markers.isEmpty()) {
			markers = List.of();
		}
	}
}
