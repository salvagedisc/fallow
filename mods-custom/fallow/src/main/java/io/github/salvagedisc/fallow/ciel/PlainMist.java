package io.github.salvagedisc.fallow.ciel;

import io.github.salvagedisc.fallow.FallowConfig;
import io.github.salvagedisc.fallow.ModIds;
import io.github.salvagedisc.fallow.clairiere.ClearingClient;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;

/**
 * La brume du pack sans pack de shaders. Sous Brumaire, la brume et ses clairières sont celles du shader ; sans lui,
 * c'est la brume d'ATMOSPHERICS, dont le réglage d'origine ne connaît pas les clairières et reste légère de jour.
 * Ici, à l'air libre sur la Surface, sa distance de fin est ramenée à celle d'une brume ({@code mistDistance}, plus
 * courte la nuit), sauf au désert et dans les badlands ; dans une clairière, elle s'ouvre jusqu'à
 * {@code clearingDistance}. La part de brume levée se calcule comme celle que le module rendu donne au shader :
 * nulle au bord, entière huit blocs à l'intérieur. Le fondu ralenti de {@link FogCache#smooth}, appliqué ensuite,
 * garde la levée progressive quand la vague d'une clairière passe.
 *
 * <p>Ne charger cette classe que si ATMOSPHERICS est présent.
 */
public final class PlainMist {
    /** Sur combien de blocs, en entrant dans une clairière, la brume finit de se lever. */
    private static final double LIFT_BLOCKS = 8.0;
    /** Constante de temps de la part de brume levée, en secondes. */
    private static final float LIFT_SECONDS = 0.8F;
    /** La nuit, la brume et la clairière se referment à cette part de leur distance de jour. */
    private static final float NIGHT_SHARE = 0.55F;
    /** Sous une pluie franche, la clairière s'ouvre à cette part de sa distance. */
    private static final float RAIN_SHARE = 0.45F;
    private static final boolean IRIS = ModIds.isLoaded(ModIds.IRIS);

    /** En dessous de cette lumière du ciel, on est sous terre. */
    private static final int MIN_SKY_LIGHT = 8;

    private static float lift;
    private static long last;
    private static ClientLevel seen;

    private PlainMist() {
    }

    /** Reçoit la brume d'ATMOSPHERICS ({@code r, g, b, début, fin}) et en change la distance, sans pack de shaders. */
    public static void apply(float[] fog) {
        if (!FallowConfig.cielMistWithoutShader() || fog[4] <= 0.0F || shaderInUse()) {
            last = 0L;
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        Camera view = minecraft.gameRenderer.getMainCamera();
        if (level == null || level.dimension() != Level.OVERWORLD || view.getFluidInCamera() != FogType.NONE) {
            last = 0L;
            return;
        }
        Vec3 camera = view.getPosition();
        BlockPos eye = BlockPos.containing(camera);
        // Sous terre, la brume des grottes d'ATMOSPHERICS reste. On lit la lumière du ciel : la carte des hauteurs
        // sans feuillage n'est pas envoyée au client, et sous les arbres il fait encore assez clair.
        if (level.getBrightness(LightLayer.SKY, eye) < MIN_SKY_LIGHT) {
            last = 0L;
            return;
        }
        long now = System.nanoTime();
        float partial = minecraft.getTimer().getGameTimeDeltaPartialTick(false);
        float target = ClearingClient.liftAt(level, camera, partial, LIFT_BLOCKS);
        if (last == 0L || level != seen) {
            // Au retour (sous terre, sous l'eau, autre monde, nouvelle connexion), la brume repart de l'endroit.
            lift = target;
            seen = level;
        } else {
            float dt = Math.min(0.25F, (now - last) / 1.0E9F);
            lift += (target - lift) * (1.0F - (float) Math.exp(-dt / LIFT_SECONDS));
        }
        last = now;

        // Le jour : 1 à midi, 0 la nuit, un passage d'une heure de jeu environ à l'aube et au crépuscule.
        float sun = Mth.cos(level.getTimeOfDay(partial) * Mth.TWO_PI);
        float day = Mth.clamp(sun * 2.5F + 0.5F, 0.0F, 1.0F);
        float share = Mth.lerp(day, NIGHT_SHARE, 1.0F);

        Holder<Biome> biome = level.getBiome(eye);
        boolean dry = biome.is(Tags.Biomes.IS_DESERT) || biome.is(Tags.Biomes.IS_BADLANDS);
        float end = fog[4];
        float misty = dry ? end : Math.min(end, FallowConfig.cielMistDistance() * share);
        // Sous la pluie, la clairière s'ouvre moins loin : l'averse garde son voile, la brume seule se lève.
        float rain = level.getRainLevel(partial);
        float clear = Math.max(end, FallowConfig.cielClearingDistance() * share * Mth.lerp(rain, 1.0F, RAIN_SHARE));
        float newEnd = Mth.lerp(lift, misty, clear);
        // Le début garde la proportion voulue par ATMOSPHERICS.
        fog[3] = newEnd * fog[3] / end;
        fog[4] = Math.max(newEnd, fog[3] + 1.0F);
    }

    private static boolean shaderInUse() {
        return IRIS && IrisHolder.inUse();
    }

    /** Iris n'est chargé qu'ici : sans lui, la classe n'est jamais lue. */
    private static final class IrisHolder {
        static boolean inUse() {
            return net.irisshaders.iris.api.v0.IrisApi.getInstance().isShaderPackInUse();
        }
    }
}
