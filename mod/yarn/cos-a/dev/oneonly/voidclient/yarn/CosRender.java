package dev.oneonly.voidclient.yarn;

import dev.oneonly.voidclient.core.cosmetic.CosmeticDraw;
import dev.oneonly.voidclient.core.cosmetic.MeshSink;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.HashMap;
import java.util.Map;

/** Kosmetyki 3D na graczach (1.20.x): warstwa renderu postaci + wyjście wierzchołków. */
public final class CosRender {
    private CosRender() {}

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void register() {
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, ctx) -> {
            if (renderer instanceof PlayerEntityRenderer r) helper.register(new Feature((FeatureRendererContext) r));
        });
    }

    static final class Feature extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
        Feature(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> ctx) { super(ctx); }

        @Override
        public void render(MatrixStack m, VertexConsumerProvider vcp, int light, AbstractClientPlayerEntity e, float limbAngle, float limbDistance,
                           float tickDelta, float age, float headYaw, float headPitch) {
            if (e.isInvisible()) return;
            try {
                PlayerEntityModel<?> model = getContextModel();
                CosmeticDraw.Pose p = new CosmeticDraw.Pose();
                p.headPitch = model.head.pitch;
                p.headYaw = model.head.yaw;
                p.headRoll = model.head.roll;
                p.sneaking = e.isInSneakingPose();
                p.age = age / 20f;
                CosmeticDraw.render(e.getUuid(), p, new Out(m.peek().getPositionMatrix(), m.peek().getNormalMatrix(), vcp, light));
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
            vc.vertex(v4.x, v4.y, v4.z, (argb >> 16 & 255) / 255f, (argb >> 8 & 255) / 255f, (argb & 255) / 255f, (argb >>> 24) / 255f,
                    u, v, OverlayTexture.DEFAULT_UV, mode == 0 ? light : 0xF000F0, n3.x, n3.y, n3.z);
        }

        @Override public void end() {}
    }
}
