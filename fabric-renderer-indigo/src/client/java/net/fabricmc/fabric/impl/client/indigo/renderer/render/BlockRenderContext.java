/*
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.fabricmc.fabric.impl.client.indigo.renderer.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.impl.client.indigo.renderer.aocalc.AoCalculator;
import net.fabricmc.fabric.impl.client.indigo.renderer.aocalc.AoLuminanceFix;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Context for non-terrain block rendering.
 */
public class BlockRenderContext extends AbstractBlockRenderContext {
	private VertexConsumer vertexConsumer;

	@Override
	protected AoCalculator createAoCalc(BlockRenderInfo blockInfo) {
		return new AoCalculator(blockInfo) {
			@Override
			public int light(BlockPos pos, BlockState state) {
				return AoCalculator.getLightmapCoordinates(blockInfo.blockView, state, pos);
			}

			@Override
			public float ao(BlockPos pos, BlockState state) {
				return AoLuminanceFix.INSTANCE.apply(blockInfo.blockView, pos, state);
			}
		};
	}

	@Override
	protected VertexConsumer getVertexConsumer(RenderType layer) {
		return vertexConsumer;
	}

	public void render(BlockAndTintGetter blockView, BakedModel model, BlockState state, BlockPos pos, PoseStack matrixStack, VertexConsumer buffer, boolean cull, RandomSource random, long seed, int overlay) {
		try {
			Vec3 offset = state.getOffset(blockView, pos);
			matrixStack.translate(offset.x, offset.y, offset.z);

			this.vertexConsumer = buffer;
			this.matrix = matrixStack.last().pose();
			this.normalMatrix = matrixStack.last().normal();
			this.overlay = overlay;

			blockInfo.random = random;
			blockInfo.seed = seed;
			blockInfo.recomputeSeed = false;

			aoCalc.clear();
			blockInfo.prepareForWorld(blockView, cull);
			blockInfo.prepareForBlock(state, pos, model.useAmbientOcclusion());

			model.emitBlockQuads(blockView, state, pos, blockInfo.randomSupplier, this);
		} catch (Throwable throwable) {
			CrashReport crashReport = CrashReport.forThrowable(throwable, "Tessellating block model - Indigo Renderer");
			CrashReportCategory crashReportSection = crashReport.addCategory("Block model being tessellated");
			CrashReportCategory.populateBlockDetails(crashReportSection, blockView, pos, state);
			throw new ReportedException(crashReport);
		} finally {
			blockInfo.release();
			blockInfo.random = null;
			this.vertexConsumer = null;
		}
	}
}
