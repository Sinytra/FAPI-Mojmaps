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

package net.fabricmc.fabric.impl.networking.server;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundPingPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.S2CConfigurationChannelEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.impl.networking.AbstractChanneledNetworkAddon;
import net.fabricmc.fabric.impl.networking.ChannelInfoHolder;
import net.fabricmc.fabric.impl.networking.NetworkingImpl;
import net.fabricmc.fabric.impl.networking.RegistrationPayload;
import net.fabricmc.fabric.mixin.networking.accessor.ServerCommonNetworkHandlerAccessor;

public final class ServerConfigurationNetworkAddon extends AbstractChanneledNetworkAddon<ServerConfigurationNetworking.ConfigurationPacketHandler<?>> {
	private final ServerConfigurationPacketListenerImpl handler;
	private final MinecraftServer server;
	private final ServerConfigurationNetworking.Context context;
	private RegisterState registerState = RegisterState.NOT_SENT;

	public ServerConfigurationNetworkAddon(ServerConfigurationPacketListenerImpl handler, MinecraftServer server) {
		super(ServerNetworkingImpl.CONFIGURATION, ((ServerCommonNetworkHandlerAccessor) handler).getConnection(), "ServerConfigurationNetworkAddon for " + handler.getOwner().getName());
		this.handler = handler;
		this.server = server;
		this.context = new ContextImpl(handler, this);

		// Must register pending channels via lateinit
		this.registerPendingChannels((ChannelInfoHolder) this.connection, ConnectionProtocol.CONFIGURATION);
	}

	@Override
	protected void invokeInitEvent() {
	}

	public void preConfiguration() {
		ServerConfigurationConnectionEvents.BEFORE_CONFIGURE.invoker().onSendConfiguration(handler, server);
	}

	public void configuration() {
		ServerConfigurationConnectionEvents.CONFIGURE.invoker().onSendConfiguration(handler, server);
	}

	public boolean startConfiguration() {
		if (this.registerState == RegisterState.NOT_SENT) {
			// Send the registration packet, followed by a ping
			this.sendInitialChannelRegistrationPacket();
			this.sendPacket(new ClientboundPingPacket(0xFAB71C));

			this.registerState = RegisterState.SENT;

			// Cancel the configuration for now, the response from the ping or registration packet will continue.
			return true;
		}

		// We should have received a response
		assert registerState == RegisterState.RECEIVED || registerState == RegisterState.NOT_RECEIVED;
		return false;
	}

	@Override
	protected void receiveRegistration(boolean register, RegistrationPayload resolvable) {
		super.receiveRegistration(register, resolvable);

		if (register && registerState == RegisterState.SENT) {
			// We received the registration packet, thus we know this is a modded client, continue with configuration.
			registerState = RegisterState.RECEIVED;
			handler.startConfiguration();
		}
	}

	public void onPong(int parameter) {
		if (registerState == RegisterState.SENT) {
			// We did not receive the registration packet, thus we think this is a vanilla client, continue with configuration.
			registerState = RegisterState.NOT_RECEIVED;
			handler.startConfiguration();
		}
	}

	@Override
	protected void receive(ServerConfigurationNetworking.ConfigurationPacketHandler<?> handler, CustomPacketPayload payload) {
		((ServerConfigurationNetworking.ConfigurationPacketHandler) handler).receive(payload, this.context);
	}

	// impl details

	@Override
	protected void schedule(Runnable task) {
		this.server.execute(task);
	}

	@Override
	public Packet<?> createPacket(CustomPacketPayload packet) {
		return ServerConfigurationNetworking.createS2CPacket(packet);
	}

	@Override
	protected void invokeRegisterEvent(List<ResourceLocation> ids) {
		S2CConfigurationChannelEvents.REGISTER.invoker().onChannelRegister(this.handler, this, this.server, ids);
	}

	@Override
	protected void invokeUnregisterEvent(List<ResourceLocation> ids) {
		S2CConfigurationChannelEvents.UNREGISTER.invoker().onChannelUnregister(this.handler, this, this.server, ids);
	}

	@Override
	protected void handleRegistration(ResourceLocation channelName) {
		// If we can already send packets, immediately send the register packet for this channel
		if (this.registerState != RegisterState.NOT_SENT) {
			RegistrationPayload registrationPayload = this.createRegistrationPayload(RegistrationPayload.REGISTER, Collections.singleton(channelName));

			if (registrationPayload != null) {
				this.sendPacket(registrationPayload);
			}
		}
	}

	@Override
	protected void handleUnregistration(ResourceLocation channelName) {
		// If we can already send packets, immediately send the unregister packet for this channel
		if (this.registerState != RegisterState.NOT_SENT) {
			RegistrationPayload registrationPayload = this.createRegistrationPayload(RegistrationPayload.UNREGISTER, Collections.singleton(channelName));

			if (registrationPayload != null) {
				this.sendPacket(registrationPayload);
			}
		}
	}

	@Override
	protected void invokeDisconnectEvent() {
		ServerConfigurationConnectionEvents.DISCONNECT.invoker().onConfigureDisconnect(handler, server);
	}

	@Override
	protected boolean isReservedChannel(ResourceLocation channelName) {
		return NetworkingImpl.isReservedCommonChannel(channelName);
	}

	@Override
	public void sendPacket(Packet<?> packet, PacketSendListener callback) {
		handler.send(packet, callback);
	}

	private enum RegisterState {
		NOT_SENT,
		SENT,
		RECEIVED,
		NOT_RECEIVED
	}

	public ChannelInfoHolder getChannelInfoHolder() {
		return (ChannelInfoHolder) ((ServerCommonNetworkHandlerAccessor) handler).getConnection();
	}

	private record ContextImpl(ServerConfigurationPacketListenerImpl networkHandler, PacketSender responseSender) implements ServerConfigurationNetworking.Context {
		private ContextImpl {
			Objects.requireNonNull(networkHandler, "networkHandler");
			Objects.requireNonNull(responseSender, "responseSender");
		}
	}
}
