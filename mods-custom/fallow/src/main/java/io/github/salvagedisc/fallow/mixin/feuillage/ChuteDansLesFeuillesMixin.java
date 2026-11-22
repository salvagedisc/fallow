package io.github.salvagedisc.fallow.mixin.feuillage;

import io.github.salvagedisc.fallow.feuillage.FeuillageModule;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Passable Foliage efface presque toute chute dans ses feuilles, dans {@code onEntityCollidedWithLeaves}, appelé à
 * chaque contact. On entre après ses refus (chauve-souris, créatif en vol, contact qui n'en est pas un), à la lecture
 * de son réglage de sons, quand la chute est encore entière : {@link FeuillageModule#entre} en rend la moitié des
 * dégâts. Appliqué seulement avec Passable Foliage.
 */
@Pseudo
@Mixin(targets = "snownee.passablefoliage.PassableFoliage", remap = false)
abstract class ChuteDansLesFeuillesMixin {
    @Inject(method = "onEntityCollidedWithLeaves", at = @At(value = "FIELD", opcode = Opcodes.GETSTATIC,
            target = "Lsnownee/passablefoliage/PassableFoliageCommonConfig;soundsPlayerOnly:Z"), require = 0)
    private static void fallow$chute(Level level, BlockPos pos, BlockState state, Entity entity, CallbackInfo ci) {
        FeuillageModule.entre(level, entity);
    }
}
