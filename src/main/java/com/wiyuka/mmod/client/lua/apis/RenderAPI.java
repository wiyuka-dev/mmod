package com.wiyuka.mmod.client.lua.apis;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.Dispatch;
import org.squiddev.cobalt.function.LibFunction;

public class RenderAPI implements LuaAPI {
    private static LuaValue renderCallback = null;
    private static LuaState currentState = null;
    private static boolean isEventRegistered = false;
    private static boolean isRendering = false;

    private static float[] triangles = new float[1300];
    private static int triangleCount = 0;

    private static float[] quads = new float[1600];
    private static int quadCount = 0;

    private static float[] lines = new float[1000];
    private static int lineCount = 0;

    private LuaTable drawApiContext;

    @Override
    public void register(LuaTable env) {
        LuaTable render = new LuaTable();
        drawApiContext = new LuaTable();

        drawApiContext.rawset("drawTriangle", LibFunction.createV((state, args) -> {
            if (!isRendering) throw new LuaError("Render APIs can only be called inside the render callback!");
            triangles = ensureCapacity(triangles, (triangleCount + 1) * 13);
            int i = triangleCount * 13;
            triangles[i]   = (float) args.arg(1).checkDouble();  triangles[i+1] = (float) args.arg(2).checkDouble();  triangles[i+2] = (float) args.arg(3).checkDouble();
            triangles[i+3] = (float) args.arg(4).checkDouble();  triangles[i+4] = (float) args.arg(5).checkDouble();  triangles[i+5] = (float) args.arg(6).checkDouble();
            triangles[i+6] = (float) args.arg(7).checkDouble();  triangles[i+7] = (float) args.arg(8).checkDouble();  triangles[i+8] = (float) args.arg(9).checkDouble();
            triangles[i+9] = (float) args.arg(10).optDouble(1.0);triangles[i+10]= (float) args.arg(11).optDouble(1.0);triangles[i+11]= (float) args.arg(12).optDouble(1.0); triangles[i+12] = (float) args.arg(13).optDouble(1.0);
            triangleCount++;
            return Constants.NONE;
        }));

        drawApiContext.rawset("drawQuad", LibFunction.createV((state, args) -> {
            if (!isRendering) throw new LuaError("Render APIs can only be called inside the render callback!");
            quads = ensureCapacity(quads, (quadCount + 1) * 16);
            int i = quadCount * 16;
            quads[i]   = (float) args.arg(1).checkDouble();  quads[i+1] = (float) args.arg(2).checkDouble();  quads[i+2] = (float) args.arg(3).checkDouble();
            quads[i+3] = (float) args.arg(4).checkDouble();  quads[i+4] = (float) args.arg(5).checkDouble();  quads[i+5] = (float) args.arg(6).checkDouble();
            quads[i+6] = (float) args.arg(7).checkDouble();  quads[i+7] = (float) args.arg(8).checkDouble();  quads[i+8] = (float) args.arg(9).checkDouble();
            quads[i+9] = (float) args.arg(10).checkDouble(); quads[i+10]= (float) args.arg(11).checkDouble(); quads[i+11]= (float) args.arg(12).checkDouble();
            quads[i+12]= (float) args.arg(13).optDouble(1.0);quads[i+13]= (float) args.arg(14).optDouble(1.0);quads[i+14]= (float) args.arg(15).optDouble(1.0); quads[i+15] = (float) args.arg(16).optDouble(1.0);
            quadCount++;
            return Constants.NONE;
        }));

        drawApiContext.rawset("drawLine", LibFunction.createV((state, args) -> {
            if (!isRendering) throw new LuaError("Render APIs can only be called inside the render callback!");
            lines = ensureCapacity(lines, (lineCount + 1) * 10);
            int i = lineCount * 10;
            lines[i]   = (float) args.arg(1).checkDouble(); lines[i+1] = (float) args.arg(2).checkDouble(); lines[i+2] = (float) args.arg(3).checkDouble();
            lines[i+3] = (float) args.arg(4).checkDouble(); lines[i+4] = (float) args.arg(5).checkDouble(); lines[i+5] = (float) args.arg(6).checkDouble();
            lines[i+6] = (float) args.arg(7).optDouble(1.0);lines[i+7] = (float) args.arg(8).optDouble(1.0);lines[i+8] = (float) args.arg(9).optDouble(1.0); lines[i+9] = (float) args.arg(10).optDouble(1.0);
            lineCount++;
            return Constants.NONE;
        }));

        render.rawset("onRender", LibFunction.createV((state, args) -> {
            if (args.arg(1).isNil()) {
                renderCallback = null;
            } else {
                renderCallback = args.arg(1).checkFunction();
                currentState = state;
            }
            return Constants.NONE;
        }));

        env.rawset("render", render);

        if (!isEventRegistered) {
            WorldRenderEvents.AFTER_ENTITIES.register(context -> {
                if (renderCallback == null || currentState == null) return;

                triangleCount = 0;
                quadCount = 0;
                lineCount = 0;
                isRendering = true;

                try {
                    Dispatch.call(currentState, renderCallback, drawApiContext);
                } catch (Exception | UnwindThrowable e) {
                    e.printStackTrace();
                } finally {
                    isRendering = false;
                }

                if (triangleCount == 0 && quadCount == 0 && lineCount == 0) return;

                Minecraft client = Minecraft.getInstance();
                MultiBufferSource.BufferSource bufferSource = client.renderBuffers().bufferSource();
                PoseStack poseStack = context.matrices();
                Vec3 cameraPos = context.gameRenderer().getMainCamera().position();

                poseStack.pushPose();
                poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
                PoseStack.Pose pose = poseStack.last();
                org.joml.Matrix4f matrix = pose.pose();

                VertexConsumer builder = bufferSource.getBuffer(RenderTypes.debugFilledBox());

                for (int c = 0; c < quadCount; c++) {
                    int i = c * 16;
                    float r = quads[i+12], g = quads[i+13], b = quads[i+14], a = quads[i+15];
                    builder.addVertex(matrix, quads[i],   quads[i+1],  quads[i+2]).setColor(r, g, b, a);
                    builder.addVertex(matrix, quads[i+3], quads[i+4],  quads[i+5]).setColor(r, g, b, a);
                    builder.addVertex(matrix, quads[i+6], quads[i+7],  quads[i+8]).setColor(r, g, b, a);
                    builder.addVertex(matrix, quads[i+9], quads[i+10], quads[i+11]).setColor(r, g, b, a);
                }

                for (int c = 0; c < triangleCount; c++) {
                    int i = c * 13;
                    float r = triangles[i+9], g = triangles[i+10], b = triangles[i+11], a = triangles[i+12];
                    builder.addVertex(matrix, triangles[i],   triangles[i+1], triangles[i+2]).setColor(r, g, b, a);
                    builder.addVertex(matrix, triangles[i+3], triangles[i+4], triangles[i+5]).setColor(r, g, b, a);
                    builder.addVertex(matrix, triangles[i+6], triangles[i+7], triangles[i+8]).setColor(r, g, b, a);
                    builder.addVertex(matrix, triangles[i+6], triangles[i+7], triangles[i+8]).setColor(r, g, b, a);
                }

                for (int c = 0; c < lineCount; c++) {
                    int i = c * 10;
                    float x1 = lines[i], y1 = lines[i+1], z1 = lines[i+2];
                    float x2 = lines[i+3], y2 = lines[i+4], z2 = lines[i+5];
                    float r = lines[i+6], g = lines[i+7], b = lines[i+8], a = lines[i+9];

                    float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1;
                    float lenSq = dx * dx + dy * dy + dz * dz;
                    if (lenSq < 1e-5f) continue;

                    float invLen = 1.0f / (float) Math.sqrt(lenSq);
                    dx *= invLen; dy *= invLen; dz *= invLen;

                    float upX = 0, upY = 1, upZ = 0;
                    if (Math.abs(dy) > 0.9f) { upX = 1; upY = 0; upZ = 0; }

                    float rX = dy * upZ - dz * upY;
                    float rY = dz * upX - dx * upZ;
                    float rZ = dx * upY - dy * upX;
                    float rLenInv = 0.02f / (float) Math.sqrt(rX*rX + rY*rY + rZ*rZ);
                    rX *= rLenInv; rY *= rLenInv; rZ *= rLenInv;

                    float uX = rY * dz - rZ * dy;
                    float uY = rZ * dx - rX * dz;
                    float uZ = rX * dy - rY * dx;
                    float uLenInv = 0.02f / (float) Math.sqrt(uX*uX + uY*uY + uZ*uZ);
                    uX *= uLenInv; uY *= uLenInv; uZ *= uLenInv;

                    builder.addVertex(matrix, x1 - rX, y1 - rY, z1 - rZ).setColor(r, g, b, a);
                    builder.addVertex(matrix, x1 + rX, y1 + rY, z1 + rZ).setColor(r, g, b, a);
                    builder.addVertex(matrix, x2 + rX, y2 + rY, z2 + rZ).setColor(r, g, b, a);
                    builder.addVertex(matrix, x2 - rX, y2 - rY, z2 - rZ).setColor(r, g, b, a);

                    builder.addVertex(matrix, x1 - uX, y1 - uY, z1 - uZ).setColor(r, g, b, a);
                    builder.addVertex(matrix, x1 + uX, y1 + uY, z1 + uZ).setColor(r, g, b, a);
                    builder.addVertex(matrix, x2 + uX, y2 + uY, z2 + uZ).setColor(r, g, b, a);
                    builder.addVertex(matrix, x2 - uX, y2 - uY, z2 - uZ).setColor(r, g, b, a);
                }

                bufferSource.endBatch(RenderTypes.debugFilledBox());
                poseStack.popPose();
            });
            isEventRegistered = true;
        }
    }

    @Override
    public void clear() {
        renderCallback = null;
        triangleCount = 0;
        quadCount = 0;
        lineCount = 0;
    }

    private static float[] ensureCapacity(float[] arr, int minCapacity) {
        if (arr.length >= minCapacity) return arr;
        float[] newArr = new float[Math.max(arr.length * 2, minCapacity)];
        System.arraycopy(arr, 0, newArr, 0, arr.length);
        return newArr;
    }
}
