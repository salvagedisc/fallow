package io.github.salvagedisc.fallow.mixin.vent;

import io.github.salvagedisc.fallow.FallowConfig;
import io.github.salvagedisc.fallow.vent.SmokePlume;
import io.github.salvagedisc.fallow.vent.Wind;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.CampfireSmokeParticle;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * La fumée d'un feu de camp suit le vent du pack : elle monte droit au-dessus du feu puis se couche, d'autant plus
 * que le vent forcit ; celles de feux voisins se rejoignent ; celle d'un feu des âmes bleuit en montant. Une fumée de
 * feu de camp lancée par un autre mod (les cheminées de Sooty Chimneys) suit le même vent, sans changer d'aspect.
 */
@Mixin(CampfireSmokeParticle.class)
abstract class CampfireSmokeParticleMixin extends TextureSheetParticle implements SmokePlume {
    @Unique
    private boolean fallow$led;
    @Unique
    private boolean fallow$soul;
    @Unique
    private double fallow$mergeX;
    @Unique
    private double fallow$mergeZ;

    protected CampfireSmokeParticleMixin(ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
    }

    @Override
    public void fallow$plume(float plume, boolean soul, double mergeX, double mergeZ) {
        fallow$led = true;
        fallow$soul = soul;
        fallow$mergeX = mergeX;
        fallow$mergeZ = mergeZ;
        quadSize *= Mth.lerp(0.6F, 1.0F, plume);
        lifetime = (int) (lifetime * (0.85F + 0.35F * plume));
        // Une fumée un peu moins blanche que celle du jeu, qui ne pique pas les yeux sous le soleil ; bleutée
        // au-dessus d'un feu des âmes.
        if (soul) {
            setColor(0.66F, 0.70F, 0.76F);
        } else {
            setColor(0.86F, 0.86F, 0.86F);
        }
        // Plus légère que celle du jeu : on voit à travers, surtout au bout du panache.
        alpha *= 0.75F;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void fallow$wind(CallbackInfo ci) {
        if (removed) {
            return;
        }
        if (!fallow$led) {
            if (!FallowConfig.ventEnabled()) {
                return;
            }
            // Une fumée qui ne vient pas d'un feu de camp du pack : elle garde son aspect et suit le vent, seule.
            fallow$led = true;
            fallow$mergeX = x;
            fallow$mergeZ = z;
        }
        // Les premières secondes, la chaleur la porte droit ; le vent la prend ensuite, à sa vitesse à elle.
        float rise = Mth.clamp(age / 30.0F, 0.0F, 1.0F);
        Vec3 wind = Wind.velocity(x, z);
        double ease = 0.035 * rise;
        xd += (wind.x / 20.0 * 0.85 - xd) * ease;
        zd += (wind.z / 20.0 * 0.85 - zd) * ease;
        // Les fumées de feux voisins se rapprochent en montant.
        if (age < 40) {
            xd += (fallow$mergeX - x) * 0.0015;
            zd += (fallow$mergeZ - z) * 0.0015;
        }
        // Un vent fort la couche : elle monte moins vite.
        yd *= 1.0 - 0.012 * Math.min(Wind.strength(), 1.2F) * rise;
        float t = Mth.clamp(age / (float) lifetime, 0.0F, 1.0F);
        alpha = Math.min(alpha, 0.7F * (1.0F - t * t) + 0.02F);
        if (fallow$soul) {
            setColor(Mth.lerp(t, 0.66F, 0.56F), Mth.lerp(t, 0.70F, 0.70F), Mth.lerp(t, 0.76F, 0.92F));
        }
    }
}
