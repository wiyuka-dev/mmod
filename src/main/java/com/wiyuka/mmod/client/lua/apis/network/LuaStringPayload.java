package com.wiyuka.mmod.client.lua.apis.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.NonNull;

public record LuaStringPayload(String luaChannel, String data) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<LuaStringPayload> ID = new CustomPacketPayload.Type<>(Identifier.parse("mmod:lua_packet"));

    public static final StreamCodec<RegistryFriendlyByteBuf, LuaStringPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, LuaStringPayload::luaChannel,
            ByteBufCodecs.STRING_UTF8, LuaStringPayload::data,
            LuaStringPayload::new
    );

    @Override
    public CustomPacketPayload.@NonNull Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}