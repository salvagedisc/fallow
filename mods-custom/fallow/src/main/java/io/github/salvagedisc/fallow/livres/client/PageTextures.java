package io.github.salvagedisc.fallow.livres.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.lwjgl.opengl.GL11;

import io.github.salvagedisc.fallow.Fallow;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Pages painted off screen, so that a sheet being turned can bend with what is written on it: a book asks for the face of a
 * sheet while it draws the world ({@link #request}), gets back the picture painted for it at the start of this frame, and the
 * face is painted again at the start of the next one. The painting never happens while the world is drawn: a shader pack
 * (Iris) takes over the game's shaders then, and its own would not paint a flat picture. A face asked for the first time has
 * no picture yet ({@code null}): the book draws that sheet flat for that one frame, as it always did.
 */
public final class PageTextures {
    /** A face not asked for in this long, milliseconds, gives its picture back to the graphics card. */
    private static final long FORGET_AFTER = 3000L;

    private static final Map<Object, Face> FACES = new HashMap<>();
    private static boolean hooked;
    private static boolean warned;
    private static int made;

    private PageTextures() {
    }

    /** Paints a face, in pixels of its picture: x right, y down, z towards the reader, as a screen. */
    @FunctionalInterface
    public interface Painter {
        void paint(PoseStack poseStack, MultiBufferSource buffers);
    }

    /**
     * The picture of the face {@code key} ({@code wide} x {@code high} pixels) painted for this frame, or {@code null} when there is
     * none yet; {@code painter} paints it again before the next frame. {@code key} is compared by {@code equals}: a record of the
     * book (itself compared by identity), the page and the side does.
     */
    public static ResourceLocation request(Object key, int wide, int high, Painter painter) {
        if (!hooked) {
            hooked = true;
            NeoForge.EVENT_BUS.addListener(RenderFrameEvent.Pre.class, PageTextures::paintAll);
        }
        Face face = FACES.computeIfAbsent(key, k -> new Face(made++));
        face.asked = net.minecraft.Util.getMillis();
        face.painter = painter;
        face.wide = wide;
        face.high = high;
        return face.ready ? face.id : null;
    }

    private static void paintAll(RenderFrameEvent.Pre event) {
        if (FACES.isEmpty()) {
            return;
        }
        long now = net.minecraft.Util.getMillis();
        Minecraft minecraft = Minecraft.getInstance();
        boolean any = false;
        for (Iterator<Face> it = FACES.values().iterator(); it.hasNext(); ) {
            Face face = it.next();
            if (now - face.asked > FORGET_AFTER) {
                face.close();
                it.remove();
                continue;
            }
            if (face.painter == null) {
                continue;
            }
            if (!any) {
                any = true;
                begin();
            }
            try {
                face.paint(minecraft);
            } catch (RuntimeException e) {
                // A page that cannot be painted off screen stays flat: the book goes on drawing it as before.
                if (!warned) {
                    warned = true;
                    Fallow.LOGGER.warn("livres : page non peinte hors écran", e);
                }
                face.ready = false;
            }
            face.painter = null;
        }
        if (any) {
            end(minecraft);
        }
    }

    private static Matrix4f projection;
    private static com.mojang.blaze3d.vertex.VertexSorting sorting;
    private static float fogStart;
    private static float fogEnd;

    private static void begin() {
        projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        sorting = RenderSystem.getVertexSorting();
        fogStart = RenderSystem.getShaderFogStart();
        fogEnd = RenderSystem.getShaderFogEnd();
        FogRenderer.setupNoFog();
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.identity();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        // The level's lights: the icons on the pages turn their normals into the first one, as they do in the world.
        Lighting.setupLevel();
    }

    private static void end(Minecraft minecraft) {
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.popMatrix();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.setProjectionMatrix(projection, sorting);
        RenderSystem.setShaderFogStart(fogStart);
        RenderSystem.setShaderFogEnd(fogEnd);
        minecraft.getMainRenderTarget().bindWrite(true);
    }

    /** One face's picture: a frame buffer, seen by the game as a texture under its own name. */
    private static final class Face {
        final ResourceLocation id;
        final Picture picture = new Picture();
        RenderTarget target;
        Painter painter;
        int wide;
        int high;
        long asked;
        boolean ready;
        boolean registered;

        Face(int n) {
            id = ResourceLocation.fromNamespaceAndPath(Fallow.MOD_ID, "livres/page_" + n);
        }

        void paint(Minecraft minecraft) {
            if (target == null || target.width != wide || target.height != high) {
                if (target == null) {
                    target = new TextureTarget(wide, high, true, Minecraft.ON_OSX);
                } else {
                    target.resize(wide, high, Minecraft.ON_OSX);
                }
                target.setFilterMode(GL11.GL_LINEAR);
            }
            picture.target = target;
            if (!registered) {
                minecraft.getTextureManager().register(id, picture);
                registered = true;
            }
            target.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
            target.clear(Minecraft.ON_OSX);
            target.bindWrite(true);
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0.0F, wide, high, 0.0F, -4000.0F, 4000.0F),
                    VertexSorting.ORTHOGRAPHIC_Z);
            MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
            painter.paint(new PoseStack(), buffers);
            buffers.endBatch();
            ready = true;
        }

        void close() {
            if (registered) {
                Minecraft.getInstance().getTextureManager().release(id);
            }
            if (target != null) {
                target.destroyBuffers();
                target = null;
            }
        }
    }

    /** The colour of a frame buffer handed to the game as a texture; the buffer is the face's to free, not the texture's. */
    private static final class Picture extends AbstractTexture {
        RenderTarget target;

        @Override
        public void load(ResourceManager resourceManager) {
        }

        @Override
        public int getId() {
            return target == null ? super.getId() : target.getColorTextureId();
        }

        @Override
        public void releaseId() {
        }
    }
}
