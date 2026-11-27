package com.gtzc.tweakify.client.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 诅咒移除范围：全部 / 仅绑定 / 仅消失；未知附魔路径只在“全部”范围移除。
 */
class CurseRemovalScopeTest {

	@Test
	void allRemovesEveryCurse() {
		assertTrue(CurseRemovalScope.shouldRemove("all", "binding_curse"));
		assertTrue(CurseRemovalScope.shouldRemove("all", "vanishing_curse"));
		assertTrue(CurseRemovalScope.shouldRemove("all", "modded_curse"));
	}

	@Test
	void bindingOnlyKeepsVanishing() {
		assertTrue(CurseRemovalScope.shouldRemove("binding_only", "binding_curse"));
		assertFalse(CurseRemovalScope.shouldRemove("binding_only", "vanishing_curse"));
		assertFalse(CurseRemovalScope.shouldRemove("binding_only", "modded_curse"));
	}

	@Test
	void vanishingOnlyKeepsBinding() {
		assertTrue(CurseRemovalScope.shouldRemove("vanishing_only", "vanishing_curse"));
		assertFalse(CurseRemovalScope.shouldRemove("vanishing_only", "binding_curse"));
	}

	@Test
	void unknownCodeFallsBackToAll() {
		assertTrue(CurseRemovalScope.shouldRemove("not-a-scope", "modded_curse"));
		assertTrue(CurseRemovalScope.shouldRemove(null, "binding_curse"));
	}
}
