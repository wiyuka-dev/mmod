package com.wiyuka.mmod.client.lua.apis;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.Dispatch;
import org.squiddev.cobalt.function.LibFunction;

import java.util.Optional;

public class WorldAPI implements LuaAPI {
    private static LuaValue tickCallback = null;
    private static LuaState currentState = null;
    private static boolean isEventRegistered = false;

    @Override
    public void register(LuaTable env) {
        LuaTable world = new LuaTable();

        if (!isEventRegistered) {
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                if (tickCallback != null && currentState != null) {
                    try {
                        Dispatch.call(currentState, tickCallback);
                    } catch (Exception | UnwindThrowable e) {
                        e.printStackTrace();
                    }
                }
            });
            isEventRegistered = true;
        }
        world.rawset("onTick", LibFunction.createV((state, args) -> {
            if (args.arg(1).isNil()) {
                tickCallback = null;
            } else {
                tickCallback = args.arg(1).checkFunction();
                currentState = state;
            }
            return Constants.NONE;
        }));
        world.rawset("onTick", LibFunction.createV((state, args) -> {
            if (args.arg(1).isNil()) {
                tickCallback = null;
            } else {
                tickCallback = args.arg(1).checkFunction();
                currentState = state;
            }
            return Constants.NONE;
        }));

        world.rawset("sendMessage", LibFunction.createV((state, args) -> {
            String message = args.arg(1).checkString();

            Minecraft client = Minecraft.getInstance();
            client.execute(() -> {
                if (client.player != null) {
                    client.player.displayClientMessage(Component.literal(message), false);
                }
            });
            return Constants.NONE;
        }));

        world.rawset("playSound", LibFunction.createV((state, args) -> {
            String soundId = args.arg(1).checkString();
            double x = args.arg(2).checkDouble();
            double y = args.arg(3).checkDouble();
            double z = args.arg(4).checkDouble();

            float volume = (float) args.arg(5).optDouble(1.0);
            float pitch = (float) args.arg(6).optDouble(1.0);

            Identifier id = Identifier.parse(soundId);

            Minecraft client = Minecraft.getInstance();
            client.execute(() -> {
                if (client.level != null) {
                    Optional<SoundEvent> soundEventObj = BuiltInRegistries.SOUND_EVENT.getOptional(id);
                    if (soundEventObj.isPresent()) {
                        client.level.playLocalSound(x, y, z, soundEventObj.get(), SoundSource.MASTER, volume, pitch, false);
                    } else {
                        System.err.println("[Lua] Sound not found: " + soundId);
                    }
                }
            });
            return Constants.NONE;
        }));

        world.rawset("spawnParticle", LibFunction.createV((state, args) -> {
            String particleId = args.arg(1).checkString();
            double x = args.arg(2).checkDouble();
            double y = args.arg(3).checkDouble();
            double z = args.arg(4).checkDouble();

            double vx = args.arg(5).optDouble(0.0);
            double vy = args.arg(6).optDouble(0.0);
            double vz = args.arg(7).optDouble(0.0);

            Identifier id = Identifier.parse(particleId);

            Minecraft client = Minecraft.getInstance();
            client.execute(() -> {
                if (client.level != null) {
                    Optional<ParticleType<?>> typeOptional = BuiltInRegistries.PARTICLE_TYPE.getOptional(id);

                    if (typeOptional.isEmpty()) {
                        System.err.println("[Lua] Particle not found: " + particleId);
                        return;
                    }

                    ParticleType<?> particleType = typeOptional.get();

                    if (particleType instanceof ParticleOptions options) {
                        client.level.addParticle(options, x, y, z, vx, vy, vz);
                    } else {
                        System.err.println("[Lua] Particle " + particleId + " requires special parameters.");
                    }
                }
            });
            return Constants.NONE;
        }));

        env.rawset("world", world);
    }

    @Override
    public void clear() {
    }
}