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

package net.fabricmc.fabric.test.rendering.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.slf4j.Logger;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.AtlasSourceTypeRegistry;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.atlas.SpriteResourceLoader;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSourceType;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceMetadata;
import net.minecraft.util.Mth;

public class CustomAtlasSourcesTest implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		AtlasSourceTypeRegistry.register(ResourceLocation.fromNamespaceAndPath("fabric-rendering-v1-testmod", "double"), DoubleAtlasSource.TYPE);
	}

	private static class DoubleAtlasSource implements SpriteSource {
		private static final Logger LOGGER = LogUtils.getLogger();
		public static final MapCodec<DoubleAtlasSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
				ResourceLocation.CODEC.fieldOf("resource").forGetter(source -> source.resource),
				ResourceLocation.CODEC.fieldOf("sprite").forGetter(source -> source.sprite)
		).apply(instance, DoubleAtlasSource::new));
		public static final SpriteSourceType TYPE = new SpriteSourceType(CODEC);

		private final ResourceLocation resource;
		private final ResourceLocation sprite;

		DoubleAtlasSource(ResourceLocation resource, ResourceLocation sprite) {
			this.resource = resource;
			this.sprite = sprite;
		}

		@Override
		public void run(ResourceManager resourceManager, Output regions) {
			ResourceLocation resourceId = TEXTURE_ID_CONVERTER.idToFile(resource);
			Optional<Resource> optionalResource = resourceManager.getResource(resourceId);

			if (optionalResource.isPresent()) {
				regions.add(sprite, new DoubleSpriteRegion(resourceId, optionalResource.get(), sprite));
			} else {
				LOGGER.warn("Missing sprite: {}", resourceId);
			}
		}

		@Override
		public SpriteSourceType type() {
			return TYPE;
		}

		private static class DoubleSpriteRegion implements SpriteSource.SpriteSupplier {
			private final ResourceLocation resourceId;
			private final Resource resource;
			private final ResourceLocation spriteId;

			DoubleSpriteRegion(ResourceLocation resourceId, Resource resource, ResourceLocation spriteId) {
				this.resourceId = resourceId;
				this.resource = resource;
				this.spriteId = spriteId;
			}

			@Override
			public SpriteContents apply(SpriteResourceLoader spriteOpener) {
				ResourceMetadata metadata;

				try {
					metadata = resource.metadata();
				} catch (Exception e) {
					LOGGER.error("Unable to parse metadata from {}", resourceId, e);
					return null;
				}

				NativeImage image;

				try (InputStream inputStream = resource.open()) {
					image = NativeImage.read(inputStream);
				} catch (IOException e) {
					LOGGER.error("Using missing texture, unable to load {}", resourceId, e);
					return null;
				}

				int imageWidth = image.getWidth();
				int imageHeight = image.getHeight();
				AnimationMetadataSection animationMetadata = metadata.getSection(AnimationResourceMetadata.field_55537)
						.orElse(new AnimationMetadataSection(Optional.empty(), Optional.empty(), Optional.empty(), 1, false));
				FrameSize dimensions = animationMetadata.calculateFrameSize(imageWidth, imageHeight);
				int frameWidth = dimensions.width();
				int frameHeight = dimensions.height();

				if (!Mth.isMultipleOf(imageWidth, frameWidth) || !Mth.isMultipleOf(imageHeight, dimensions.height())) {
					LOGGER.error("Image {} size {},{} is not multiple of frame size {},{}", resourceId, imageWidth, imageHeight, frameWidth, frameHeight);
					image.close();
					return null;
				}

				int frameCountX = imageWidth / frameWidth;
				int frameCountY = imageHeight / frameHeight;
				int offsetX = frameWidth / 16;
				int offsetY = frameHeight / 16;

				NativeImage doubleImage = new NativeImage(image.format(), image.getWidth(), image.getHeight(), false);

				for (int frameY = 0; frameY < frameCountY; frameY++) {
					for (int frameX = 0; frameX < frameCountX; frameX++) {
						blendRect(image, doubleImage, frameX * frameWidth + offsetX, frameY * frameHeight + offsetY, frameX * frameWidth, frameY * frameHeight, frameWidth - offsetX, frameHeight - offsetY);
						blendRect(image, doubleImage, frameX * frameWidth, frameY * frameHeight, frameX * frameWidth + offsetX, frameY * frameHeight + offsetY, frameWidth - offsetX, frameHeight - offsetY);
					}
				}

				return new SpriteContents(spriteId, dimensions, doubleImage, metadata);
			}

			private static void blendRect(NativeImage src, NativeImage dst, int srcX, int srcY, int destX, int destY, int width, int height) {
				for (int y = 0; y < height; ++y) {
					for (int x = 0; x < width; ++x) {
						int c = src.getPixel(srcX + x, srcY + y);
						dst.setPixel(destX + x, destY + y, c);
					}
				}
			}
		}
	}
}
