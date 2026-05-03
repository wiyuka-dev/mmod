package com.wiyuka.mmod.client;

import com.wiyuka.mmod.client.lua.apis.ConsoleAPI;
import com.wiyuka.mmod.client.lua.apis.RenderAPI;
import com.wiyuka.mmod.client.lua.apis.WorldAPI;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.compiler.LoadState;
import org.squiddev.cobalt.function.LuaFunction;
import org.squiddev.cobalt.lib.TableLib;

import java.io.InputStream;
import java.util.Optional;

public class LuaScriptLoader {
    public static void register() {
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return Identifier.parse("mmod:lua_script_reloader");
            }

            @Override
            public void onResourceManagerReload(ResourceManager resourceManager) {
                Identifier scriptId = Identifier.parse("mmod:script/main.lua");
                Optional<Resource> resourceOpt = resourceManager.getResource(scriptId);

                if (resourceOpt.isPresent()) {
                    try (InputStream stream = resourceOpt.get().open()) {
                        executeScript(stream);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                }
            }
        });
    }

    private static void executeScript(InputStream stream) {
        LuaState state = new LuaState();
        LuaTable env = new LuaTable();


        new ConsoleAPI().register(env);
        new WorldAPI().register(env);
        new RenderAPI().register(env);

        try {
            org.squiddev.cobalt.lib.MathLib.add(state, env);
            org.squiddev.cobalt.lib.StringLib.add(state, env);
            org.squiddev.cobalt.lib.BaseLib.add(env);
            org.squiddev.cobalt.lib.CoroutineLib.add(state, env);
            LuaFunction chunk = LoadState.load(state, stream, "@main.lua", env);

            LuaThread.runMain(state, chunk);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}