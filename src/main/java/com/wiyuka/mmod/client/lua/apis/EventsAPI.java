package com.wiyuka.mmod.client.lua.apis;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.Dispatch;
import org.squiddev.cobalt.function.LibFunction;

import java.util.function.Consumer;

public class EventsAPI implements LuaAPI {
    private static LuaState currentState = null;
    private static boolean isEventRegistered = false;

    private static LuaValue onSwingHandCallback = null;
    private static LuaValue onAttackBlockCallback = null;
    private static LuaValue onUseBlockCallback = null;
    private static LuaValue onUseItemCallback = null;
    private static LuaValue onAttackEntityCallback = null;
    private static LuaValue onUseEntityCallback = null;
    private static LuaValue onChatCallback = null;

    @Override
    public void register(LuaTable env) {
        LuaTable events = new LuaTable();

        if (!isEventRegistered) {
            registerFabricEvents();
            isEventRegistered = true;
        }

        events.rawset("onSwingHand", createSetter(cb -> onSwingHandCallback = cb));
        events.rawset("onAttackBlock", createSetter(cb -> onAttackBlockCallback = cb));
        events.rawset("onUseBlock", createSetter(cb -> onUseBlockCallback = cb));
        events.rawset("onUseItem", createSetter(cb -> onUseItemCallback = cb));
        events.rawset("onAttackEntity", createSetter(cb -> onAttackEntityCallback = cb));
        events.rawset("onUseEntity", createSetter(cb -> onUseEntityCallback = cb));
        events.rawset("onChat", createSetter(cb -> onChatCallback = cb));

        env.rawset("events", events);
    }

    private LibFunction createSetter(Consumer<LuaValue> setter) {
        return LibFunction.createV((state, args) -> {
            if (args.arg(1).isNil()) {
                setter.accept(null);
            } else {
                setter.accept(args.arg(1).checkFunction());
                currentState = state;
            }
            return Constants.NONE;
        });
    }

    private void registerFabricEvents() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null && client.player.swinging && client.player.swingTime == 0) {
                invokeCallback(onSwingHandCallback, ValueFactory.valueOf(client.player.swingingArm.name()));
            }
        });

        AttackBlockCallback.EVENT.register((player, level, hand, pos, direction) -> {
            if (level.isClientSide() && isClientLocalPlayer(player)) {
                invokeCallback(onAttackBlockCallback,
                        ValueFactory.valueOf(pos.getX()),
                        ValueFactory.valueOf(pos.getY()),
                        ValueFactory.valueOf(pos.getZ()),
                        ValueFactory.valueOf(hand.name())
                );
            }
            return InteractionResult.PASS;
        });

        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (level.isClientSide() && isClientLocalPlayer(player)) {
                invokeCallback(onUseBlockCallback,
                        ValueFactory.valueOf(hitResult.getBlockPos().getX()),
                        ValueFactory.valueOf(hitResult.getBlockPos().getY()),
                        ValueFactory.valueOf(hitResult.getBlockPos().getZ()),
                        ValueFactory.valueOf(hand.name())
                );
            }
            return InteractionResult.PASS;
        });

        UseItemCallback.EVENT.register((player, level, hand) -> {
            if (level.isClientSide() && isClientLocalPlayer(player)) {
                invokeCallback(onUseItemCallback, ValueFactory.valueOf(hand.name()));
            }
            return InteractionResult.PASS;
        });

        AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (level.isClientSide() && isClientLocalPlayer(player)) {
                invokeCallback(onAttackEntityCallback, ValueFactory.valueOf(entity.getUUID().toString()));
            }
            return InteractionResult.PASS;
        });

        UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (level.isClientSide() && isClientLocalPlayer(player)) {
                invokeCallback(onUseEntityCallback, ValueFactory.valueOf(entity.getUUID().toString()));
            }
            return InteractionResult.PASS;
        });

        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            if (onChatCallback != null && currentState != null) {
                try {
                    LuaValue result = Dispatch.call(currentState, onChatCallback, ValueFactory.valueOf(message));

                    if (result.type() == Constants.TBOOLEAN && !result.checkBoolean()) {
                        return false;
                    }
                } catch (Exception | UnwindThrowable e) {
                    e.printStackTrace();
                }
            }
            return true;
        });
    }

    private static boolean isClientLocalPlayer(Player player) {
        Minecraft client = Minecraft.getInstance();
        return client.player != null && client.player.getUUID().equals(player.getUUID());
    }

    private static void invokeCallback(LuaValue callback, LuaValue... args) {
        if (callback != null && currentState != null) {
            try {
                Dispatch.invoke(currentState, callback, ValueFactory.varargsOf(args));
            } catch (Exception | UnwindThrowable e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void clear() {
        onSwingHandCallback = null;
        onAttackBlockCallback = null;
        onUseBlockCallback = null;
        onUseItemCallback = null;
        onAttackEntityCallback = null;
        onUseEntityCallback = null;
        onChatCallback = null;
    }
}