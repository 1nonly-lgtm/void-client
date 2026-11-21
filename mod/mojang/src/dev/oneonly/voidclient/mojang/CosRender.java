package dev.oneonly.voidclient.mojang;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.oneonly.voidclient.core.cosmetic.CosmeticDraw;
import dev.oneonly.voidclient.core.cosmetic.MeshSink;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Kosmetyki 3D na graczach (26.1+): geometria jako własne polecenie w kolejce renderu. */
public final class CosRender {
    private CosRender() {}

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void register() {
        LivingEntityRenderLayerRegistrationCallback.EVENT.register((type, renderer, helper, ctx) -> {
            if (renderer instanceof AvatarRenderer r) helper.register(new Layer((RenderLayerParent) r));
        });
    }

    public static UUID uuid(int id) {
        var level = Minecraft.getInstance().level;
        if (level == null) return null;
        Entity e = level.getEntity(id);
        return e == null ? null : e.getUUID();
    }

    static final float DEG = (float) (Math.PI / 180);

    static final class Layer extends RenderLayer<AvatarRenderState, PlayerModel> {
        Layer(RenderLayerParent<AvatarRenderState, PlayerModel> parent) { super(parent); }

        @Override
        public void submit(PoseStack m, SubmitNodeCollector q, int light, AvatarRenderState s, float limbAngle, float limbDistance) {
            if (s.isInvisible) return;
            try {
                CosmeticDraw.Pose p = new CosmeticDraw.Pose();
                p.headPitch = s.xRot * DEG;
                p.headYaw = s.yRot * DEG;
                p.sneaking = s.isCrouching;
                p.age = s.ageInTicks / 20f;
                CosmeticDraw.render(uuid(s.id), p, new Out(m, q, light));
            } catch (Throwable ignored) {
            }
        }
    }

    private static final Map<String, RenderType> LAYERS = new HashMap<>();

    static RenderType layer(String tex, int mode) {
        return LAYERS.computeIfAbsent(tex + mode, k -> {
            Identifier id = Identifier.tryParse(tex);
            return mode == 2 ? RenderTypes.eyes(id) : mode == 1 ? RenderTypes.entityTranslucentEmissive(id) : RenderTypes.entityTranslucent(id);
        });
    }

    static final class Out implements MeshSink {
        final PoseStack m;
        final SubmitNodeCollector q;
        final int light;
        String tex;
        int mode;
        float[] buf = new float[9 * 64];
        int n;

        Out(PoseStack m, SubmitNodeCollector q, int light) { this.m = m; this.q = q; this.light = light; }

        @Override public void begin(String texture, int mode) { this.tex = texture; this.mode = mode; n = 0; }

        @Override public void vertex(float x, float y, float z, float u, float v, int argb, float nx, float ny, float nz) {
            if (n + 9 > buf.length) buf = java.util.Arrays.copyOf(buf, buf.length * 2);
            buf[n++] = x / 16f; buf[n++] = y / 16f; buf[n++] = z / 16f; buf[n++] = u; buf[n++] = v;
            buf[n++] = Float.intBitsToFloat(argb); buf[n++] = nx; buf[n++] = ny; buf[n++] = nz;
        }

        @Override public void end() {
            final float[] data = java.util.Arrays.copyOf(buf, n);
            final int lt = mode == 0 ? light : 0xF000F0;
            q.submitCustomGeometry(m, layer(tex, mode), (pose, vc) -> {
                Matrix4f pos = pose.pose();
                Matrix3f nrm = pose.normal();
                Vector4f v4 = new Vector4f();
                Vector3f n3 = new Vector3f();
                for (int i = 0; i < data.length; i += 9) {
                    pos.transform(v4.set(data[i], data[i + 1], data[i + 2], 1f));
                    nrm.transform(n3.set(data[i + 6], data[i + 7], data[i + 8]));
                    vc.addVertex(v4.x, v4.y, v4.z, Float.floatToRawIntBits(data[i + 5]), data[i + 3], data[i + 4], OverlayTexture.NO_OVERLAY, lt, n3.x, n3.y, n3.z);
                }
            });
        }
    }
}
