package dev.latvian.mods.kubejs.web.local.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

public record MovedVertexConsumer(VertexConsumer parent, PoseStack.Pose pose) implements VertexConsumer {
	@Override
	public VertexConsumer addVertex(float x, float y, float z) {
		return parent.addVertex(pose.pose(), x, y, z);
	}

	@Override
	public VertexConsumer setColor(int red, int green, int blue, int alpha) {
		return parent.setColor(red, green, blue, alpha);
	}

	@Override
	public VertexConsumer setColor(int i) {
		return parent.setColor(i);
	}

	@Override
	public VertexConsumer setUv(float u, float v) {
		return parent.setUv(u, v);
	}

	@Override
	public VertexConsumer setUv1(int u, int v) {
		return parent.setUv1(u, v);
	}

	@Override
	public VertexConsumer setUv2(int u, int v) {
		return parent.setUv2(u, v);
	}

	@Override
	public VertexConsumer setUv3(float v, float v1) {
		return this.parent.setUv3(v, v1);
	}

	@Override
	public VertexConsumer setNormal(float normalX, float normalY, float normalZ) {
		return parent.setNormal(pose, normalX, normalY, normalZ);
	}

	@Override
	public VertexConsumer setLineWidth(float v) {
		return parent.setLineWidth(v);
	}
}
