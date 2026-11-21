package io.github.salvagedisc.fallow.mixin.navires;

import io.github.salvagedisc.fallow.FallowConfig;
import io.github.salvagedisc.fallow.navires.Gros;
import io.github.salvagedisc.fallow.vent.WindField;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Le voilier de Fish 'N' Ships prend le vent de Fallow : voile baissée, il avance d'une poussée fixe dans son cap
 * (0,03 bloc par tick, quel que soit le temps). Elle devient celle du vent de fond ({@link WindField}), selon l'allure
 * ({@link Gros#sail}) et la force du vent : rien face au vent, le plus vent de travers, plus vite par grand vent.
 * Le calcul tourne là où le bateau est piloté (le client du barreur), avec le même vent que le serveur.
 */
@Pseudo
@Mixin(targets = "dev.hybridlabs.fishnships.entity.vehicle.SailboatEntity", remap = false)
abstract class VoileMixin {
    @ModifyConstant(method = "moveWithSailDown", constant = @Constant(doubleValue = 0.03), remap = false, require = 0)
    private double fallow$leVentDansLaVoile(double base) {
        if (!FallowConfig.naviresSailWind()) {
            return base;
        }
        Entity boat = (Entity) (Object) this;
        Level level = boat.level();
        if (level.dimension() != Level.OVERWORLD) {
            return base;
        }
        float rain = level.getRainLevel(1.0F);
        float thunder = level.getThunderLevel(1.0F);
        double[] wind = WindField.velocity(level.getGameTime(), rain, thunder, boat.getY());
        float yaw = boat.getYRot() * Mth.DEG_TO_RAD;
        // Le cap du bateau, comme le calcule Fish 'N' Ships : (-sin, cos) de son lacet.
        double allure = Gros.sail(-Mth.sin(yaw), Mth.cos(yaw), wind[0], wind[1]);
        return Gros.sailPush(base, allure, Math.hypot(wind[0], wind[1]), FallowConfig.naviresWindStrength());
    }
}
