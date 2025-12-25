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

package net.fabricmc.fabric.impl.gametest;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.function.Consumer;

import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.gametest.framework.GameTestServer;
import net.minecraft.gametest.framework.GlobalTestReporter;
import net.minecraft.gametest.framework.TestCommand;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.loader.api.FabricLoader;

public final class FabricGameTestHelper {
	public static final boolean ENABLED = System.getProperty("fabric-api.gametest") != null;

	/**
	 * When enabled the {@link TestCommand} and related arguments will be registered.
	 *
	 * <p>When {@link EnvType#CLIENT} the default value is true.
	 *
	 * <p>When {@link EnvType#SERVER} the default value is false.
	 */
	public static final boolean COMMAND_ENABLED = Boolean.parseBoolean(System.getProperty("fabric-api.gametest.command", FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT ? "true" : "false"));

	private static final Logger LOGGER = LoggerFactory.getLogger(FabricGameTestHelper.class);

	private static final String GAMETEST_STRUCTURE_PATH = "gametest/structure";

	public static final FileToIdConverter GAMETEST_STRUCTURE_FINDER = new FileToIdConverter(GAMETEST_STRUCTURE_PATH, ".snbt");

	private FabricGameTestHelper() {
	}

	public static void runHeadlessServer(LevelStorageSource.LevelStorageAccess session, PackRepository resourcePackManager) {
		String reportPath = System.getProperty("fabric-api.gametest.report-file");

		if (reportPath != null) {
			try {
				GlobalTestReporter.replaceWith(new SavingXmlReportingTestCompletionListener(new File(reportPath)));
			} catch (ParserConfigurationException e) {
				throw new RuntimeException(e);
			}
		}

		LOGGER.info("Starting test server");
		MinecraftServer server = GameTestServer.spin(thread -> {
			return GameTestServer.create(thread, session, resourcePackManager, getTestFunctions(), BlockPos.ZERO);
		});
	}

	public static Consumer<GameTestHelper> getTestMethodInvoker(Method method) {
		return testContext -> {
			Class<?> testClass = method.getDeclaringClass();

			Constructor<?> constructor;

			try {
				constructor = testClass.getConstructor();
			} catch (NoSuchMethodException e) {
				throw new RuntimeException("Test class (%s) provided by (%s) must have a public default or no args constructor".formatted(testClass.getSimpleName(), FabricGameTestModInitializer.getModIdForTestClass(testClass)));
			}

			Object testObject;

			try {
				testObject = constructor.newInstance();
			} catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
				throw new RuntimeException("Failed to create instance of test class (%s)".formatted(testClass.getCanonicalName()), e);
			}

			if (testObject instanceof FabricGameTest fabricGameTest) {
				fabricGameTest.invokeTestMethod(testContext, method);
			} else {
				invokeTestMethod(testContext, method, testObject);
			}
		};
	}

	public static void invokeTestMethod(GameTestHelper testContext, Method method, Object testObject) {
		try {
			method.invoke(testObject, testContext);
		} catch (IllegalAccessException e) {
			throw new RuntimeException("Failed to invoke test method (%s) in (%s) because %s".formatted(method.getName(), method.getDeclaringClass().getCanonicalName(), e.getMessage()), e);
		} catch (InvocationTargetException e) {
			LOGGER.error("Exception occurred when invoking test method {} in ({})", method.getName(), method.getDeclaringClass().getCanonicalName(), e);

			if (e.getCause() instanceof RuntimeException runtimeException) {
				throw runtimeException;
			} else {
				throw new RuntimeException(e.getCause());
			}
		}
	}

	private static Collection<TestFunction> getTestFunctions() {
		return GameTestRegistry.getAllTestFunctions();
	}
}
