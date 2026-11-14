package com.gtzc.tweakify.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 顶点缓存的构建与扩容：属性写入、容量增长后旧顶点保留、重复 begin 重置计数。
 */
class CachedGeometryTest {

	@Test
	void beginResetsCountAndAddStoresAttributes() {
		CachedGeometry geometry = new CachedGeometry();
		geometry.begin(true, true, 2);
		geometry.add(1.0f, 2.0f, 3.0f, 0xFF112233, 0.0f, 1.0f, 0.0f, 0.5f);
		geometry.add(4.0f, 5.0f, 6.0f, 0xFF445566, 1.0f, 0.0f, 0.0f, 1.0f);

		assertEquals(2, geometry.vertexCount());
		assertEquals(1.0f, geometry.x(0), 1.0e-6);
		assertEquals(2.0f, geometry.y(0), 1.0e-6);
		assertEquals(3.0f, geometry.z(0), 1.0e-6);
		assertEquals(0xFF112233, geometry.color(0));
		assertEquals(1.0f, geometry.normalY(0), 1.0e-6);
		assertEquals(0.5f, geometry.lineWidth(0), 1.0e-6);
		assertEquals(4.0f, geometry.x(1), 1.0e-6);
		assertEquals(1.0f, geometry.normalX(1), 1.0e-6);
	}

	@Test
	void growKeepsExistingVertices() {
		CachedGeometry geometry = new CachedGeometry();
		geometry.begin(false, false, 1);
		for (int i = 0; i < 64; i++) {
			geometry.add(i, i + 0.5f, i + 0.25f, i, 0.0f, 0.0f, 0.0f, 0.0f);
		}

		assertEquals(64, geometry.vertexCount());
		assertEquals(0.0f, geometry.x(0), 1.0e-6);
		assertEquals(63.0f, geometry.x(63), 1.0e-6);
		assertEquals(63.5f, geometry.y(63), 1.0e-6);
		assertEquals(63, geometry.color(63));
	}

	@Test
	void beginClearsCountButReuseCapacity() {
		CachedGeometry geometry = new CachedGeometry();
		geometry.begin(false, false, 8);
		for (int i = 0; i < 8; i++) {
			geometry.add(i, 0.0f, 0.0f, 0, 0.0f, 0.0f, 0.0f, 0.0f);
		}
		assertEquals(8, geometry.vertexCount());

		geometry.begin(false, false, 8);
		assertEquals(0, geometry.vertexCount());
		geometry.add(9.0f, 0.0f, 0.0f, 7, 0.0f, 0.0f, 0.0f, 0.0f);
		assertEquals(1, geometry.vertexCount());
		assertEquals(9.0f, geometry.x(0), 1.0e-6);
	}

	@Test
	void lineWidthAbsentReadsZero() {
		CachedGeometry geometry = new CachedGeometry();
		geometry.begin(true, false, 1);
		geometry.add(0.0f, 0.0f, 0.0f, 1, 0.0f, 1.0f, 0.0f, 2.0f);

		assertEquals(0.0f, geometry.lineWidth(0), 1.0e-6);
		assertEquals(1.0f, geometry.normalY(0), 1.0e-6);
	}
}
