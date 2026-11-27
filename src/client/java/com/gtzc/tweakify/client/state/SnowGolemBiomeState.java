package com.gtzc.tweakify.client.state;

/**
 * 雪傀儡群系免疫：可限定只在主世界生效。
 */
public final class SnowGolemBiomeState {
	private SnowGolemBiomeState() {
	}

	public static boolean shouldSuppressMelting(boolean enabled, boolean overworldOnly, boolean inOverworld) {
		return enabled && (!overworldOnly || inOverworld);
	}
}
