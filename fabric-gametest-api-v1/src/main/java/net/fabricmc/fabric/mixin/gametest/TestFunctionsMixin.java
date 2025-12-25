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

package net.fabricmc.fabric.mixin.gametest;

import java.lang.reflect.Method;
import java.util.Locale;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.fabricmc.fabric.impl.gametest.FabricGameTestHelper;
import net.fabricmc.fabric.impl.gametest.FabricGameTestModInitializer;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.gametest.framework.TestFunction;

@Mixin(GameTestRegistry.class)
public abstract class TestFunctionsMixin {
	@Inject(at = @At("HEAD"), method = "turnMethodIntoTestFunction(Ljava/lang/reflect/Method;)Lnet/minecraft/gametest/framework/TestFunction;", cancellable = true)
	private static void getTestFunction(Method method, CallbackInfoReturnable<TestFunction> cir) {
		GameTest gameTest = method.getAnnotation(GameTest.class);
		String testSuiteName = method.getDeclaringClass().getSimpleName().toLowerCase(Locale.ROOT);
		String testCaseName = testSuiteName + "." + method.getName().toLowerCase(Locale.ROOT);

		String modId = FabricGameTestModInitializer.getModIdForTestClass(method.getDeclaringClass());
		String structureName = "%s:%s".formatted(modId, testCaseName);

		if (!gameTest.template().isEmpty()) {
			structureName = gameTest.template();
		}

		TestFunction testFunction = new TestFunction(gameTest.batch(),
				testCaseName,
				structureName,
				StructureUtils.getRotationForRotationSteps(gameTest.rotationSteps()),
				gameTest.timeoutTicks(),
				gameTest.setupTicks(),
				gameTest.required(),
				gameTest.manualOnly(),
				gameTest.attempts(),
				gameTest.requiredSuccesses(),
				gameTest.skyAccess(),
				FabricGameTestHelper.getTestMethodInvoker(method)
		);

		cir.setReturnValue(testFunction);
	}
}
