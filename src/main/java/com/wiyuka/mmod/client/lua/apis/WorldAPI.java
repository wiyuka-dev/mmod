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
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.Dispatch;
import org.squiddev.cobalt.function.LibFunction;

import java.util.Optional;

public class WorldAPI implements LuaAPI {
    private static final Logger LOGGER = LoggerFactory.getLogger(WorldAPI.class);

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
                        LOGGER.error("[Lua] Error executing tick callback", e);
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

        world.rawset("sendMessage", LibFunction.createV((state, args) -> {
            String message = args.arg(1).checkString();
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                client.player.displayClientMessage(Component.literal(message), false);
            }
            return Constants.NONE;
        }));

        world.rawset("playSound", LibFunction.createV((state, args) -> {
            String soundId = args.arg(1).checkString();
            double x = args.arg(2).checkDouble();
            double y = args.arg(3).checkDouble();
            double z = args.arg(4).checkDouble();
            float volume = (float) args.arg(5).optDouble(1.0);
            float pitch = (float) args.arg(6).optDouble(1.0);

            Identifier id = Identifier.tryParse(soundId);
            if (id == null) {
                LOGGER.error("[Lua] Invalid sound identifier: {}", soundId);
                return Constants.NONE;
            }

            Minecraft client = Minecraft.getInstance();
            if (client.level != null) {
                Optional<SoundEvent> soundEventObj = BuiltInRegistries.SOUND_EVENT.getOptional(id);
                if (soundEventObj.isPresent()) {
                    client.level.playLocalSound(x, y, z, soundEventObj.get(), SoundSource.MASTER, volume, pitch, false);
                } else {
                    LOGGER.error("[Lua] Sound not found: {}", soundId);
                }
            }
            return Constants.NONE;
        }));

        world.rawset("setPlayerVelocity", LibFunction.createV((state, args) -> {
            double vx = args.arg(1).checkDouble();
            double vy = args.arg(2).checkDouble();
            double vz = args.arg(3).checkDouble();
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                client.player.setDeltaMovement(vx, vy, vz);
            }
            return Constants.NONE;
        }));

        world.rawset("getPlayerLookVec", LibFunction.createV((state, args) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                Vec3 lookVec = client.player.getViewVector(1.0f);
                return ValueFactory.varargsOf(
                        ValueFactory.valueOf(lookVec.x),
                        ValueFactory.valueOf(lookVec.y),
                        ValueFactory.valueOf(lookVec.z)
                );
            }
            return Constants.NIL;
        }));

        world.rawset("getMainHandItem", LibFunction.createV((state, args) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                net.minecraft.world.item.ItemStack stack = client.player.getMainHandItem();
                String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                String itemName = stack.getHoverName().getString();

                LuaTable result = new LuaTable();
                result.rawset("id", ValueFactory.valueOf(itemId));
                result.rawset("name", ValueFactory.valueOf(itemName));
                return result;
            }
            return Constants.NIL;
        }));

        world.rawset("getPlayerEyePos", LibFunction.createV((state, args) -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player != null) {
                Vec3 eyePos = client.player.getEyePosition(1.0F);
                return ValueFactory.varargsOf(
                        ValueFactory.valueOf(eyePos.x),
                        ValueFactory.valueOf(eyePos.y),
                        ValueFactory.valueOf(eyePos.z)
                );
            }
            return Constants.NIL;
        }));

        world.rawset("raycastEntity", LibFunction.createV((state, args) -> {
            double maxDist = args.arg(1).optDouble(15.0);
            Minecraft client = Minecraft.getInstance();
            if (client.player != null && client.level != null) {
                Vec3 eyePos = client.player.getEyePosition(1.0f);
                Vec3 viewVec = client.player.getViewVector(1.0f);
                Vec3 endPos = eyePos.add(viewVec.scale(maxDist));
                AABB searchBox = client.player.getBoundingBox().expandTowards(viewVec.scale(maxDist)).inflate(1.0D);
                EntityHitResult hitResult = ProjectileUtil.getEntityHitResult(
                        client.level, client.player, eyePos, endPos, searchBox,
                        e -> !e.isSpectator() && e.isPickable(), (float)(maxDist * maxDist)
                );
                if (hitResult != null) {
                    LuaTable result = new LuaTable();
                    result.rawset("uuid", ValueFactory.valueOf(hitResult.getEntity().getUUID().toString()));
                    result.rawset("x", ValueFactory.valueOf(hitResult.getLocation().x()));
                    result.rawset("y", ValueFactory.valueOf(hitResult.getLocation().y()));
                    result.rawset("z", ValueFactory.valueOf(hitResult.getLocation().z()));
                    return result;
                }
            }
            return Constants.NIL;
        }));

        world.rawset("spawnParticle", LibFunction.createV((state, args) -> {
            String particleId = args.arg(1).checkString();
            double x = args.arg(2).checkDouble();
            double y = args.arg(3).checkDouble();
            double z = args.arg(4).checkDouble();
            double vx = args.arg(5).optDouble(0.0);
            double vy = args.arg(6).optDouble(0.0);
            double vz = args.arg(7).optDouble(0.0);

            Identifier id = Identifier.tryParse(particleId);
            if (id == null) {
                LOGGER.error("[Lua] Invalid particle identifier: {}", particleId);
                return Constants.NONE;
            }

            Minecraft client = Minecraft.getInstance();
            if (client.level != null) {
                Optional<ParticleType<?>> typeOptional = BuiltInRegistries.PARTICLE_TYPE.getOptional(id);

                if (typeOptional.isEmpty()) {
                    LOGGER.error("[Lua] Particle not found: {}", particleId);
                    return Constants.NONE;
                }

                ParticleType<?> particleType = typeOptional.get();

                if (particleType instanceof ParticleOptions options) {
                    client.level.addParticle(options, x, y, z, vx, vy, vz);
                } else {
                    LOGGER.error("[Lua] Particle {} requires special parameters (like Block/Dust colors) and cannot be spawned simply by ID.", particleId);
                }
            }
            return Constants.NONE;
        }));

        env.rawset("world", world);
    }

    @Override
    public void clear() {
        tickCallback = null;
        currentState = null;
    }
}
