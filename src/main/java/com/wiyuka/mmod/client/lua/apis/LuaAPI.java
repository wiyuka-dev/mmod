package com.wiyuka.mmod.client.lua.apis;

import org.squiddev.cobalt.LuaTable;

public interface LuaAPI {
    String name = "";

    void register(LuaTable env);

    void clear();
}
