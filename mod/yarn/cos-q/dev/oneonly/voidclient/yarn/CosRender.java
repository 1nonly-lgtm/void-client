package dev.oneonly.voidclient.yarn;

import dev.oneonly.voidclient.core.cosmetic.CosmeticDraw;
import dev.oneonly.voidclient.core.cosmetic.MeshSink;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/** Kosmetyki 3D na graczach (1.21.9+): geometria trafia do kolejki renderu jako własne polecenie. */
public final class CosRender {
    private CosRender() {}

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void register() {
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, ctx) -> {
            if (renderer instanceof PlayerEntityRenderer r) helper.register(new Feature((FeatureRendererContext) r));
        });
    }

    static final float DEG = (float) (Math.PI / 180);

    @SuppressWarnings("rawtypes")
    static final class Feature extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
        Feature(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> ctx) { super(ctx); }

        @Override
        public void render(MatrixStack m, OrderedRenderCommandQueue q, int light, PlayerEntityRenderState s, float limbAngle, float limbDistance) {
            if (s.invisible) return;
            try {
                CosmeticDraw.Pose p = new CosmeticDraw.Pose();
                p.headPitch = s.pitch * DEG;
                p.headYaw = s.relativeHeadYaw * DEG;
                p.sneaking = s.isInSneakingPose;
                p.age = s.age / 20f;
                CosmeticDraw.render(Ids.uuid(s.id), p, new Out(m, q, light));
            } catch (Throwable ignored) {
            }
        }
    }

    static final class Out implements MeshSink {
        final MatrixStack m;
        final OrderedRenderCommandQueue q;
        final int light;
        String tex;
        int mode;
        float[] buf = new float[9 * 64];
        int n;

        Out(MatrixStack m, OrderedRenderCommandQueue q, int light) { this.m = m; this.q = q; this.light = light; }

        @Override public void begin(String texture, int mode) { this.tex = texture; this.mode = mode; n = 0; }

        @Override public void vertex(float x, float y, float z, float u, float v, int argb, float nx, float ny, float nz) {
            if (n + 9 > buf.length) buf = java.util.Arrays.copyOf(buf, buf.length * 2);
            buf[n++] = x / 16f; buf[n++] = y / 16f; buf[n++] = z / 16f; buf[n++] = u; buf[n++] = v;
            buf[n++] = Float.intBitsToFloat(argb); buf[n++] = nx; buf[n++] = ny; buf[n++] = nz;
        }

        @Override public void end() {
            final float[] data = java.util.Arrays.copyOf(buf, n);
            final int lt = mode == 0 ? light : 0xF000F0;
            q.submitCustom(m, Layers.of(tex, mode), (entry, vc) -> {
                Matrix4f pos = entry.getPositionMatrix();
                Matrix3f nrm = entry.getNormalMatrix();
                Vector4f v4 = new Vector4f();
                Vector3f n3 = new Vector3f();
                for (int i = 0; i < data.length; i += 9) {
                    pos.transform(v4.set(data[i], data[i + 1], data[i + 2], 1f));
                    nrm.transform(n3.set(data[i + 6], data[i + 7], data[i + 8]));
                    vc.vertex(v4.x, v4.y, v4.z, Float.floatToRawIntBits(data[i + 5]), data[i + 3], data[i + 4], OverlayTexture.DEFAULT_UV, lt, n3.x, n3.y, n3.z);
                }
            });
        }
    }
}
