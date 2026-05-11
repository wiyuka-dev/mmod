package com.wiyuka.mmod.client.lua.apis;

import org.squiddev.cobalt.Constants;
import org.squiddev.cobalt.LuaError;
import org.squiddev.cobalt.LuaString;
import org.squiddev.cobalt.LuaTable;
import org.squiddev.cobalt.Varargs;
import org.squiddev.cobalt.function.LibFunction;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class ConsoleAPI implements LuaAPI {

    private BufferedReader reader;

    @Override
    public void register(LuaTable env) {
        LuaTable console = new LuaTable();

        // TODO use logger
        console.rawset("log", LibFunction.createV((state, args) -> {
            System.out.println("[INFO] " + formatArgs(args));
            return Constants.NONE;
        }));

        console.rawset("warn", LibFunction.createV((state, args) -> {
            System.err.println("[WARN] " + formatArgs(args));
            return Constants.NONE;
        }));

        console.rawset("error", LibFunction.createV((state, args) -> {
            System.err.println("[ERROR] " + formatArgs(args));
            return Constants.NONE;
        }));

        console.rawset("clearScreen", LibFunction.create(state -> {
            System.out.print("\033[H\033[2J");
            System.out.flush();
            return Constants.NIL;
        }));

        console.rawset("readLine", LibFunction.create(state -> {
            if (reader == null) {
                reader = new BufferedReader(new InputStreamReader(System.in));
            }
            try {
                String line = reader.readLine();
                if (line != null) {
                    return LuaString.valueOf(line);
                }
            } catch (IOException e) {
                throw new LuaError("Error reading from console: " + e.getMessage());
            }
            return Constants.NIL;
        }));

        env.rawset("console", console);
    }

    private String formatArgs(Varargs args) {
        StringBuilder sb = new StringBuilder();
        int count = args.count();
        for (int i = 1; i <= count; i++) {
            sb.append(args.arg(i).toString());
            if (i < count) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    @Override
    public void clear() {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            reader = null;
        }
    }
}