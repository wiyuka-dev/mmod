package com.wiyuka.mmod.client;

import net.fabricmc.api.ClientModInitializer;
import org.squiddev.cobalt.*;
public class MmodClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        try {
            LuaScriptLoader.register();
        } catch (Exception e) {
            System.err.println("MMod Lua Engine Failed to Start:");
            e.printStackTrace();
        }
    }
}
