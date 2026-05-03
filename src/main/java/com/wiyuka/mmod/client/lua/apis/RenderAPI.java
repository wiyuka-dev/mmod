package com.wiyuka.mmod.client.lua.apis;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.squiddev.cobalt.*;
import org.squiddev.cobalt.function.Dispatch;
import org.squiddev.cobalt.function.LibFunction;

import java.util.ArrayList;
import java.util.List;

public class RenderAPI implements LuaAPI {
    private static LuaValue renderCallback = null;
    private static LuaState currentState = null;
    private static boolean isEventRegistered = false;

    private static boolean isRendering = false;
    private static final List<Triangle> triangles = new ArrayList<>();
    private static final List<Quad> quads = new ArrayList<>();

    private LuaTable drawApiContext;

    @Override
    public void register(LuaTable env) {
        LuaTable render = new LuaTable();
        drawApiContext = new LuaTable();

        drawApiContext.rawset("drawTriangle", LibFunction.createV((state, args) -> {
            if (!isRendering) {
                throw new LuaError("Render APIs can only be called inside the render callback!");
            }
            Triangle t = new Triangle(
                    (float) args.arg(1).checkDouble(), (float) args.arg(2).checkDouble(), (float) args.arg(3).checkDouble(),
                    (float) args.arg(4).checkDouble(), (float) args.arg(5).checkDouble(), (float) args.arg(6).checkDouble(),
                    (float) args.arg(7).checkDouble(), (float) args.arg(8).checkDouble(), (float) args.arg(9).checkDouble(),
                    (float) args.arg(10).optDouble(1.0), (float) args.arg(11).optDouble(1.0),
                    (float) args.arg(12).optDouble(1.0), (float) args.arg(13).optDouble(1.0)
            );
            triangles.add(t);
            return Constants.NONE;
        }));

        drawApiContext.rawset("drawQuad", LibFunction.createV((state, args) -> {
            if (!isRendering) {
                throw new LuaError("Render APIs can only be called inside the render callback!");
            }
            Quad q = new Quad(
                    (float) args.arg(1).checkDouble(), (float) args.arg(2).checkDouble(), (float) args.arg(3).checkDouble(),
                    (float) args.arg(4).checkDouble(), (float) args.arg(5).checkDouble(), (float) args.arg(6).checkDouble(),
                    (float) args.arg(7).checkDouble(), (float) args.arg(8).checkDouble(), (float) args.arg(9).checkDouble(),
                    (float) args.arg(10).checkDouble(), (float) args.arg(11).checkDouble(), (float) args.arg(12).checkDouble(),
                    (float) args.arg(13).optDouble(1.0), (float) args.arg(14).optDouble(1.0),
                    (float) args.arg(15).optDouble(1.0), (float) args.arg(16).optDouble(1.0)
            );
            quads.add(q);
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

                isRendering = true;
                triangles.clear();
                quads.clear();

                try {
                    Dispatch.call(currentState, renderCallback, drawApiContext);
                } catch (Exception | UnwindThrowable e) {
                    e.printStackTrace();
                }

                isRendering = false;

                if (triangles.isEmpty() && quads.isEmpty()) return;

                Minecraft client = Minecraft.getInstance();
                MultiBufferSource.BufferSource bufferSource = client.renderBuffers().bufferSource();

                VertexConsumer builder = bufferSource.getBuffer(RenderTypes.debugFilledBox());

                PoseStack poseStack = context.matrices();
                Vec3 cameraPos = context.gameRenderer().getMainCamera().position();

                poseStack.pushPose();
                poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
                Matrix4f pose = poseStack.last().pose();

                for (Quad q : quads) {
                    q.draw(builder, pose);
                }

                for (Triangle t : triangles) {
                    t.draw(builder, pose);
                }

                poseStack.popPose();
            });
            isEventRegistered = true;
        }
    }

    @Override
    public void clear() {
        renderCallback = null;
        triangles.clear();
        quads.clear();
    }

    private static abstract class Shape {
        protected final float r, g, b, a;

        Shape(float r, float g, float b, float a) {
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
        }

        abstract void draw(VertexConsumer consumer, Matrix4f pose);
    }

    private static class Triangle extends Shape {
        private final float x1, y1, z1, x2, y2, z2, x3, y3, z3;

        Triangle(float x1, float y1, float z1,
                 float x2, float y2, float z2,
                 float x3, float y3, float z3,
                 float r, float g, float b, float a) {
            super(r, g, b, a);
            this.x1 = x1; this.y1 = y1; this.z1 = z1;
            this.x2 = x2; this.y2 = y2; this.z2 = z2;
            this.x3 = x3; this.y3 = y3; this.z3 = z3;
        }

        @Override
        void draw(VertexConsumer consumer, Matrix4f pose) {
            consumer.addVertex(pose, x1, y1, z1).setColor(r, g, b, a);
            consumer.addVertex(pose, x2, y2, z2).setColor(r, g, b, a);
            consumer.addVertex(pose, x3, y3, z3).setColor(r, g, b, a);
            consumer.addVertex(pose, x3, y3, z3).setColor(r, g, b, a);
        }
    }

    private static class Quad extends Shape {
        private final float x1, y1, z1, x2, y2, z2, x3, y3, z3, x4, y4, z4;

        Quad(float x1, float y1, float z1,
             float x2, float y2, float z2,
             float x3, float y3, float z3,
             float x4, float y4, float z4,
             float r, float g, float b, float a) {
            super(r, g, b, a);
            this.x1 = x1; this.y1 = y1; this.z1 = z1;
            this.x2 = x2; this.y2 = y2; this.z2 = z2;
            this.x3 = x3; this.y3 = y3; this.z3 = z3;
            this.x4 = x4; this.y4 = y4; this.z4 = z4;
        }

        @Override
        void draw(VertexConsumer consumer, Matrix4f pose) {
            consumer.addVertex(pose, x1, y1, z1).setColor(r, g, b, a);
            consumer.addVertex(pose, x2, y2, z2).setColor(r, g, b, a);
            consumer.addVertex(pose, x3, y3, z3).setColor(r, g, b, a);
            consumer.addVertex(pose, x4, y4, z4).setColor(r, g, b, a);
        }
    }
}