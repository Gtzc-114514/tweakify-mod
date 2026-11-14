package com.gtzc.tweakify.client;

import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.phys.AABB;

/**
 * 铁傀儡进矿车：空矿车与铁傀儡接触时，由服务端让铁傀儡坐进矿车。
 * 纯判定只负责开关、端侧、空车、矿车种类与搜索框计算，实际拾取逻辑在 MinecartPickUpIronGolemMixin。
 */
public final class IronGolemMinecartState {
	// 三个方向都膨胀：铁傀儡站在矿车顶部时也要能被检测到。
	public static final double SEARCH_MARGIN = 0.2;

	private IronGolemMinecartState() {
	}

	public static boolean shouldPickUp(boolean enabled, boolean clientSide, boolean hasPassenger,
									   boolean plainMinecart) {
		return enabled && !clientSide && !hasPassenger && plainMinecart;
	}

	// 只认普通矿车：漏斗/运输/动力/TNT/刷怪笼/命令方块矿车都是 AbstractMinecart 的其它子类，
	// 它们各自有界面或特殊功能，让铁傀儡坐进去会直接堵死。
	public static boolean isPlainMinecart(AbstractMinecart minecart) {
		return minecart != null && minecart.getClass() == Minecart.class;
	}

	// 矿车的实体搜索范围：水平与垂直方向都向外扩张。
	public static AABB searchBox(AABB cartBox) {
		return cartBox.inflate(SEARCH_MARGIN, SEARCH_MARGIN, SEARCH_MARGIN);
	}
}
