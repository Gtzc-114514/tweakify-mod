package com.gtzc.tweakify.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import java.util.Arrays;

/**
 * 预构建的顶点缓存：保存相对某个基准点的顶点数据（位置/颜色/法线/线宽）。
 *
 * <p>重建时把所有几何算一次（含三角函数与法线归一化），渲染帧只把缓存数据平移后
 * 写入 {@link VertexConsumer}，不再重复计算线段/方块坐标。
 */
final class CachedGeometry {
	private float[] positions = new float[0];
	private int[] colors = new int[0];
	private float[] normals = new float[0];
	private float[] lineWidths = new float[0];
	private int vertexCount;
	private boolean hasNormals;
	private boolean hasLineWidths;

	// 开新一轮构建：清空计数并预分配容量；属性数组按需保留。
	void begin(boolean normals, boolean lineWidths, int expectedVertices) {
		this.hasNormals = normals;
		this.hasLineWidths = lineWidths;
		this.vertexCount = 0;
		ensureCapacity(Math.max(expectedVertices, 16));
	}

	void add(float x, float y, float z, int color, float normalX, float normalY, float normalZ,
			 float lineWidth) {
		int index = this.vertexCount;
		ensureCapacity(index + 1);
		int position = index * 3;
		this.positions[position] = x;
		this.positions[position + 1] = y;
		this.positions[position + 2] = z;
		this.colors[index] = color;
		if (this.hasNormals) {
			this.normals[position] = normalX;
			this.normals[position + 1] = normalY;
			this.normals[position + 2] = normalZ;
		}
		if (this.hasLineWidths) {
			this.lineWidths[index] = lineWidth;
		}
		this.vertexCount = index + 1;
	}

	private void ensureCapacity(int vertices) {
		int capacity = this.positions.length / 3;
		if (capacity >= vertices) {
			return;
		}
		int newCapacity = Math.max(vertices, Math.max(16, capacity * 2));
		this.positions = Arrays.copyOf(this.positions, newCapacity * 3);
		this.colors = Arrays.copyOf(this.colors, newCapacity);
		this.normals = Arrays.copyOf(this.normals, newCapacity * 3);
		this.lineWidths = Arrays.copyOf(this.lineWidths, newCapacity);
	}

	int vertexCount() {
		return this.vertexCount;
	}

	float x(int vertex) {
		return this.positions[vertex * 3];
	}

	float y(int vertex) {
		return this.positions[vertex * 3 + 1];
	}

	float z(int vertex) {
		return this.positions[vertex * 3 + 2];
	}

	int color(int vertex) {
		return this.colors[vertex];
	}

	float normalX(int vertex) {
		return this.normals[vertex * 3];
	}

	float normalY(int vertex) {
		return this.normals[vertex * 3 + 1];
	}

	float normalZ(int vertex) {
		return this.normals[vertex * 3 + 2];
	}

	float lineWidth(int vertex) {
		return this.lineWidths[vertex];
	}

	// 渲染帧：把 [start, start+count) 的顶点平移后写入消费器，不再做任何几何计算。
	void writeRange(VertexConsumer vertices, PoseStack.Pose pose, int start, int count,
					float offsetX, float offsetY, float offsetZ) {
		int end = Math.min(start + count, this.vertexCount);
		for (int vertex = start; vertex < end; vertex++) {
			int position = vertex * 3;
			VertexConsumer consumer = vertices.addVertex(pose,
				this.positions[position] + offsetX,
				this.positions[position + 1] + offsetY,
				this.positions[position + 2] + offsetZ);
			consumer.setColor(this.colors[vertex]);
			if (this.hasNormals) {
				consumer.setNormal(pose, this.normals[position], this.normals[position + 1],
					this.normals[position + 2]);
			}
			if (this.hasLineWidths) {
				consumer.setLineWidth(this.lineWidths[vertex]);
			}
		}
	}

	void writeTo(VertexConsumer vertices, PoseStack.Pose pose,
				 float offsetX, float offsetY, float offsetZ) {
		writeRange(vertices, pose, 0, this.vertexCount, offsetX, offsetY, offsetZ);
	}
}
