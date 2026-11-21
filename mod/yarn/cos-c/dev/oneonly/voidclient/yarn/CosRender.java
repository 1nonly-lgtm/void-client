package dev.oneonly.voidclient.yarn;

import dev.oneonly.voidclient.core.cosmetic.CosmeticDraw;
import dev.oneonly.voidclient.core.cosmetic.MeshSink;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.HashMap;
import java.util.Map;

/** Kosmetyki 3D na graczach (1.21.4 – 1.21.8, stan renderu). */
public final class CosRender {
    private CosRender() {}

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void register() {
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, ctx) -> {
            if (renderer instanceof PlayerEntityRenderer r) helper.register(new Feature((FeatureRendererContext) r));
        });
    }

    static final float DEG = (float) (Math.PI / 180);

    static final class Feature extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
        Feature(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> ctx) { super(ctx); }

        @Override
        public void render(MatrixStack m, VertexConsumerProvider vcp, int light, PlayerEntityRenderState s, float limbAngle, float limbDistance) {
            if (s.invisible) return;
            try {
                CosmeticDraw.Pose p = new CosmeticDraw.Pose();
                p.headPitch = s.pitch * DEG;
                p.headYaw = getContextModel().head.yaw; // 1.21.4: yawDegrees, 1.21.5+: relativeHeadYaw – model ma to samo w obu
                p.sneaking = s.isInSneakingPose;
                p.age = s.age / 20f;
                CosmeticDraw.render(Ids.uuid(s.id), p, new Out(m.peek().getPositionMatrix(), m.peek().getNormalMatrix(), vcp, light));
            } catch (Throwable ignored) {
            }
        }
    }

    static final Map<String, Identifier> IDS = new HashMap<>();

    static final class Out implements MeshSink {
        final Matrix4f pos;
        final Matrix3f nrm;
        final VertexConsumerProvider vcp;
        final int light;
        VertexConsumer vc;
        int mode;
        final Vector4f v4 = new Vector4f();
        final Vector3f n3 = new Vector3f();

        Out(Matrix4f pos, Matrix3f nrm, VertexConsumerProvider vcp, int light) { this.pos = pos; this.nrm = nrm; this.vcp = vcp; this.light = light; }

        @Override public void begin(String texture, int mode) {
            Identifier id = IDS.computeIfAbsent(texture, Identifier::tryParse);
            this.mode = mode;
            vc = vcp.getBuffer(mode == 2 ? RenderLayer.getEyes(id) : mode == 1 ? RenderLayer.getEntityTranslucentEmissive(id) : RenderLayer.getEntityTranslucent(id));
        }

        @Override public void vertex(float x, float y, float z, float u, float v, int argb, float nx, float ny, float nz) {
            pos.transform(v4.set(x / 16f, y / 16f, z / 16f, 1f));
            nrm.transform(n3.set(nx, ny, nz));
            vc.vertex(v4.x, v4.y, v4.z, argb, u, v, OverlayTexture.DEFAULT_UV, mode == 0 ? light : 0xF000F0, n3.x, n3.y, n3.z);
        }

        @Override public void end() {}
    }
}
