package com.gtzc.tweakify.client;

import com.gtzc.tweakify.config.TweakifyModConfig;
import fi.dy.masa.malilib.config.IConfigOptionListEntry;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * 防猪灵装备切换：需要替换的护甲部位。
 *
 * <p>AUTO 在“身上没有任何金制护甲”时只从背包里选一件换上（有哪个用哪个），
 * 而不是把所有金装备都穿上——原版猪灵只要看到任意一件金制护甲就会中立。
 * 其余模式只处理指定部位；模式不再覆盖的部位会由状态机换回原装备。
 */
public enum PiglinArmorSwapMode implements CodeOptionEntry {
	AUTO(TweakifyModConfig.PIGLIN_ARMOR_SWAP_AUTO,
		"tweakify-mod.config.general.option.piglinGoldArmorSwapMode.auto"),
	HELMET(TweakifyModConfig.PIGLIN_ARMOR_SWAP_HELMET,
		"tweakify-mod.config.general.option.piglinGoldArmorSwapMode.helmet"),
	CHESTPLATE(TweakifyModConfig.PIGLIN_ARMOR_SWAP_CHESTPLATE,
		"tweakify-mod.config.general.option.piglinGoldArmorSwapMode.chestplate"),
	LEGGINGS(TweakifyModConfig.PIGLIN_ARMOR_SWAP_LEGGINGS,
		"tweakify-mod.config.general.option.piglinGoldArmorSwapMode.leggings"),
	BOOTS(TweakifyModConfig.PIGLIN_ARMOR_SWAP_BOOTS,
		"tweakify-mod.config.general.option.piglinGoldArmorSwapMode.boots");

	private final String code;
	private final String translationKey;

	PiglinArmorSwapMode(String code, String translationKey) {
		this.code = code;
		this.translationKey = translationKey;
	}

	@Override
	public String code() {
		return this.code;
	}

	@Override
	public String translationKey() {
		return this.translationKey;
	}

	public static PiglinArmorSwapMode fromCode(String code) {
		for (PiglinArmorSwapMode mode : values()) {
			if (mode.code.equals(code)) {
				return mode;
			}
		}
		return AUTO;
	}

	// 该模式下是否允许处理指定护甲部位。
	// AUTO 返回全部四个部位（供恢复/模式切换判断），是否实际替换由状态机的
	// “身上已有金制护甲”守卫控制，保证只穿一件。
	public boolean allows(EquipmentSlot slot) {
		return switch (this) {
			case AUTO -> slot == EquipmentSlot.HEAD || slot == EquipmentSlot.CHEST
				|| slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET;
			case HELMET -> slot == EquipmentSlot.HEAD;
			case CHESTPLATE -> slot == EquipmentSlot.CHEST;
			case LEGGINGS -> slot == EquipmentSlot.LEGS;
			case BOOTS -> slot == EquipmentSlot.FEET;
		};
	}

	@Override
	public IConfigOptionListEntry fromString(String value) {
		return fromCode(value);
	}
}
