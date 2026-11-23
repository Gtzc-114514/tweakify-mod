package com.gtzc.tweakify.config;

import java.util.List;

import static com.gtzc.tweakify.config.ConfigCatalog.*;

/**
 * 「显示」标签的配置目录条目：世界内可视化与 HUD 叠加层。
 *
 * <p>条目顺序即界面顺序；本文件集中登记攻击范围、爆炸范围、横扫范围、屏障/基岩
 * 与轮廓/高亮等可视化开关，功能标签只保留实际行为。
 */
final class DisplayCatalogEntries {
	private DisplayCatalogEntries() {
	}

	static void addPart1(List<Entry> entries) {
		// 方块轮廓
		entries.add(display("customBlockOutline", true));
		entries.add(child("customBlockOutline", TweakifyModConfig.BLOCK_OUTLINE_WIDTH_FIELD));
		entries.add(child("customBlockOutline", "blockOutlineAnimationSpeed"));
		entries.add(child("customBlockOutline", "blockOutlinePalette"));
		entries.add(child("customBlockOutline", "blockOutlineColorMode"));
		// 实体高亮（prettyName 使用 highlight 前缀）
		entries.add(display("entityHighlight", true));
		entries.add(child("entityHighlight", "entityHighlightStyle"));
		entries.add(child("entityHighlight", "entityHighlightBlinkPeriod"));
		entries.add(child("entityHighlight", "entityHighlightColor"));
		entries.add(child("entityHighlight", "highlightPreset1", true));
		entries.add(child("highlightPreset1", "highlightPreset1Entities"));
		entries.add(child("entityHighlight", "highlightPreset2", true));
		entries.add(child("highlightPreset2", "highlightPreset2Entities"));
		entries.add(child("entityHighlight", "highlightPreset3", true));
		entries.add(child("highlightPreset3", "highlightPreset3Entities"));
		// 单层基岩与屏障
		entries.add(display("highlightSingleLayerBedrock", true));
		entries.add(child("highlightSingleLayerBedrock", "highlightSingleLayerBedrockMaxMarkers"));
		entries.add(child("highlightSingleLayerBedrock", "highlightSingleLayerBedrockScanRadius"));
		entries.add(child("highlightSingleLayerBedrock", "highlightSingleLayerBedrockColor"));
		entries.add(display("barrierVisibility", true));
		entries.add(child("barrierVisibility", "barrierVisibilityMaxMarkers"));
		entries.add(child("barrierVisibility", "barrierVisibilityScanRadius"));
		entries.add(child("barrierVisibility", "barrierVisibilityParticleInterval"));
		// 生物攻击范围
		entries.add(display("attackRangeDisplay", true));
		entries.add(child("attackRangeDisplay", "attackRangeDisplayMode"));
		entries.add(child("attackRangeDisplay", "attackRangeRenderMode"));
		entries.add(child("attackRangeDisplay", "attackRangePalette"));
		entries.add(child("attackRangeDisplay", "attackRangeIncludeSelf", true));
		entries.add(child("attackRangeDisplay", "attackRangeHostileOnly"));
		entries.add(child("attackRangeDisplay", "attackRangeIncludeInvisible"));
		entries.add(child("attackRangeDisplay", "attackRangeMaxDistance"));
		entries.add(child("attackRangeDisplay", "attackRangeMaxTargets"));
		entries.add(child("attackRangeDisplay", "attackRangeEntities"));
		// 爆炸范围：复用攻击范围的显示/渲染模式，颜色固定红色。
		entries.add(display("explosionRangeDisplay", true));
		entries.add(child("explosionRangeDisplay", "explosionRangeShowTnt"));
		entries.add(child("explosionRangeDisplay", "explosionRangeShowTntMinecarts"));
		entries.add(child("explosionRangeDisplay", "explosionRangeShowCreepers"));
		entries.add(child("explosionRangeDisplay", "explosionRangeShowEndCrystals"));
		// 横扫范围：参考 sweeping-view，目标扩展框 + 三格球体攻击范围。
		entries.add(display("sweepingView", true));
		entries.add(child("sweepingView", "sweepingViewWireframe"));
		entries.add(child("sweepingView", "sweepingViewWireframeColor"));
		entries.add(child("sweepingView", "sweepingViewFace"));
		entries.add(child("sweepingView", "sweepingViewFaceSolid"));
		entries.add(child("sweepingView", "sweepingViewFaceColor"));
		entries.add(child("sweepingView", "sweepingViewSphere"));
		entries.add(child("sweepingView", "sweepingViewSphereColor"));
		entries.add(child("sweepingView", "sweepingViewVehicles"));
		// 更好的 F2 截图：截图卡片与 F9 4K、输出格式集中在同一父项下。
		entries.add(display("betterScreenshot", false));
		entries.add(child("betterScreenshot", "betterScreenshotF9"));
		entries.add(child("betterScreenshot", "betterScreenshotFormat"));
		entries.add(child("betterScreenshot", "betterScreenshotChatCard"));
		// 区块中心指示：原版区块边界（F3+G）开启时，在所在区块中心补一条竖线。
		entries.add(display("chunkCenterIndicator", true));
	}
}
