package io.github.salvagedisc.fallow.vent;

import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Un grain que le vent emporte : poussière, pollen, sable d'une tempête. Les particules du jeu ne s'y prêtent pas :
 * leur constructeur ramène toute vitesse de départ à quelques centièmes de bloc par tick dans une direction tirée en
 * partie au hasard, puis la poussière la freine encore. Ce grain-ci suit à chaque tick le vent du pack là où il se
 * trouve ({@link Wind#velocity}), rafales comprises, et apparaît puis s'efface en douceur.
 */
public final class Mote extends TextureSheetParticle {
    /** Les petits points du jeu (generic_1 à generic_3, de deux à quatre pixels sur huit). */
    private static final ResourceLocation[] SPRITES = {
            ResourceLocation.withDefaultNamespace("generic_1"),
            ResourceLocation.withDefaultNamespace("generic_2"),
            ResourceLocation.withDefaultNamespace("generic_3"),
    };

    /** Lumière du ciel seule, pleine (bloc 0, ciel 15). */
    private static final int SKY_ONLY = LightTexture.pack(0, 15);
    /** Part de l'écart au vent rattrapée en un tick : un grain léger le rejoint en moins d'une demi-seconde. */
    private static final float FOLLOW = 0.25F;
    /** Le vent qui l'emporte, rapporté au vent du pack (une tempête souffle plus fort que lui). */
    private final double push;
    private final float opacity;
    /** Vrai si un pack de shaders l'éclaire. */
    private final boolean shaded;

    private Mote(ClientLevel level, double x, double y, double z, double push, TextureAtlasSprite sprite) {
        super(level, x, y, z);
        this.push = push;
        this.shaded = Shaders.inUse();
        this.gravity = 0.0F;
        this.friction = 1.0F;
        // Sans collisions : le jeu fige pour de bon une particule qui touche le sol en descendant. Le grain
        // s'éteint plutôt dès qu'il entre dans un bloc plein.
        this.hasPhysics = false;
        this.opacity = 0.75F + random.nextFloat() * 0.25F;
        this.alpha = 0.0F;
        Vec3 wind = Wind.velocity(x, z).scale(push / 20.0);
        this.xd = wind.x;
        this.yd = 0.0;
        this.zd = wind.z;
        setSprite(sprite);
    }

    /**
     * Lâche un grain en (x, y, z), s'il en reste la place selon le réglage des particules du joueur.
     *
     * @param rgb   couleur en plein jour, de 0 à 1 (la clarté du ciel s'y ajoute ici)
     * @param size  demi-côté du grain, en blocs
     * @param push  vent qui l'emporte, rapporté au vent du pack
     * @param ticks durée de vie
     */
    public static void release(ClientLevel level, double x, double y, double z, float[] rgb, float size, double push,
            int ticks) {
        Minecraft minecraft = Minecraft.getInstance();
        ParticleStatus status = minecraft.options.particles().get();
        if (status == ParticleStatus.MINIMAL
                || status == ParticleStatus.DECREASED && level.random.nextInt(3) == 0) {
            return;
        }
        // L'atlas des particules appartient au moteur de particules, pas aux modèles : on le lit dans le
        // gestionnaire de textures, où il est inscrit.
        if (!(minecraft.getTextureManager().getTexture(TextureAtlas.LOCATION_PARTICLES) instanceof TextureAtlas atlas)) {
            return;
        }
        Mote mote = new Mote(level, x, y, z, push, atlas.getSprite(SPRITES[level.random.nextInt(SPRITES.length)]));
        float light = mote.shaded ? 1.0F : level.getSkyDarken(1.0F);
        mote.setColor(rgb[0] * light, rgb[1] * light, rgb[2] * light);
        mote.quadSize = size;
        mote.lifetime = Math.max(1, ticks);
        minecraft.particleEngine.add(mote);
    }

    @Override
    public void tick() {
        xo = x;
        yo = y;
        zo = z;
        if (age++ >= lifetime) {
            remove();
            return;
        }
        Vec3 wind = Wind.velocity(x, z).scale(push / 20.0);
        xd += (wind.x - xd) * FOLLOW;
        zd += (wind.z - zd) * FOLLOW;
        // Des remous : le grain monte et descend un peu, sans tomber.
        yd = yd * 0.9 + (random.nextFloat() - 0.5F) * 0.012;
        move(xd, yd, zd);
        if (level.getBlockState(BlockPos.containing(x, y, z)).blocksMotion()) {
            remove();
            return;
        }
        float life = age / (float) lifetime;
        alpha = opacity * Mth.clamp(life * 6.0F, 0.0F, 1.0F) * Mth.clamp((1.0F - life) * 4.0F, 0.0F, 1.0F);
    }

    /**
     * Le grain vole toujours à ciel ouvert. Sans pack de shaders, il est en pleine lumière et sa couleur porte la
     * clarté du ciel (éclairé comme un bloc, il sortait gris sombre sur l'air clair d'une tempête). Sous un pack de
     * shaders, qui lit la lumière des blocs comme celle d'une torche, il reçoit la seule lumière du ciel et le pack
     * l'éclaire comme le reste de la scène.
     */
    @Override
    protected int getLightColor(float partialTick) {
        return shaded ? SKY_ONLY : LightTexture.FULL_BRIGHT;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
}
