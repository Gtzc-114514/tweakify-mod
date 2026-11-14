package com.gtzc.tweakify.client;

import com.gtzc.tweakify.TweakifyMod;
import com.gtzc.tweakify.config.TweakifyModConfig;
import com.gtzc.tweakify.util.HookProbe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.contextualbar.ContextualBar;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;

/**
 * 调试信息叠加层：在 HUD 左下角按行显示运行时状态。
 *
 * <p>基础行是坐标、朝向、维度与脚下方块；性能行、准星目标行与钩子探针行由
 * 同名的子开关控制。文字沿用原版 HUD 的描边样式，保证在水下/亮色背景上也清晰。
 */
public final class DebugInfoOverlay {
	private static final int TEXT_COLOR = 0xFFE0E0E0;
	private static final int LINE_HEIGHT = 10;
	private static final int MARGIN = 2;
	// 钩子列表最多显示这么多项，超出时以 "...(+N)" 结尾，避免叠加层被撑满屏幕。
	private static final int MAX_HOOK_LINES = 6;

	private DebugInfoOverlay() {
	}

	// 由 Hud.extractRenderState 末尾调用：此时原版 HUD 已经提交。
	public static void render(GuiGraphicsExtractor graphics, Minecraft client) {
		TweakifyModConfig config = TweakifyMod.CONFIG;
		if (config == null || !config.testing.debugInfoOverlay) {
			return;
		}
		if (client.player == null || client.level == null) {
			return;
		}
		List<String> lines = buildLines(client, config);
		// 底边锚在原版条带（经验条/定位栏）上方，并让出一行名字：头像上沿 2 像素 + 名字行 + 1 像素余量。
		int bottom = ContextualBar.EMPTY.top(client.getWindow())
			- (2 + client.font.lineHeight + 1);
		int y = Math.max(0, bottom - LINE_HEIGHT * lines.size());
		for (String line : lines) {
			graphics.text(client.font, line, MARGIN, y, TEXT_COLOR, true);
			y += LINE_HEIGHT;
		}
	}

	// 包私有：游戏内测试可直接校验文本内容，不必依赖渲染。
	static List<String> buildLines(Minecraft client, TweakifyModConfig config) {
		List<String> lines = new ArrayList<>();
		BlockPos pos = client.player.blockPosition();
		lines.add(String.format("XYZ: %.1f / %.1f / %.1f", client.player.getX(),
			client.player.getY(), client.player.getZ()));
		lines.add("Facing: " + client.player.getDirection().getName()
			+ String.format(" (%.1f)", client.player.getYRot()));
		lines.add("Dim: " + client.level.dimension().identifier());
		BlockState below = client.level.getBlockState(pos.below());
		Identifier belowId = BuiltInRegistries.BLOCK.getKey(below.getBlock());
		lines.add("Below: " + (belowId == null ? "unknown" : belowId.toString()));
		if (config.testing.debugInfoShowPerformance) {
			Runtime runtime = Runtime.getRuntime();
			long usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L);
			long maxMb = runtime.maxMemory() / (1024L * 1024L);
			lines.add("FPS: " + client.getFps() + "  Mem: " + usedMb + "/" + maxMb + " MB");
		}
		if (config.testing.debugInfoShowTarget) {
			lines.add("Target: " + resolveTarget(client));
		}
		if (config.testing.debugInfoShowHooks) {
			appendHookLines(lines);
		}
		return lines;
	}

	private static String resolveTarget(Minecraft client) {
		Entity entity = client.crosshairPickEntity;
		if (entity != null) {
			Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
			return id == null ? "entity" : id.toString();
		}
		HitResult hit = client.hitResult;
		if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
			Identifier id = BuiltInRegistries.BLOCK.getKey(
				client.level.getBlockState(blockHit.getBlockPos()).getBlock());
			return id == null ? "block" : id.toString();
		}
		return "-";
	}

	private static void appendHookLines(List<String> lines) {
		List<String> hooks = HookProbe.reportedHooks();
		if (hooks.isEmpty()) {
			lines.add("Hooks: (none yet)");
			return;
		}
		lines.add("Hooks: " + hooks.size());
		int shown = Math.min(hooks.size(), MAX_HOOK_LINES);
		for (int i = 0; i < shown; i++) {
			lines.add(" - " + hooks.get(i));
		}
		if (hooks.size() > shown) {
			lines.add(" - ...(+" + (hooks.size() - shown) + ")");
		}
	}
}
