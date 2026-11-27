package io.github.salvagedisc.fallow.mixin.postes;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.salvagedisc.fallow.postes.client.MenusAvecDonnees;

/** Un menu de poste rejoué en paquet vanilla est créé avec ses données de NeoForge ({@link MenusAvecDonnees}). */
@Mixin(MenuType.class)
abstract class MenuCreationMixin<T extends AbstractContainerMenu> {
    @Unique
    private static boolean dedans;

    @Inject(method = "create(ILnet/minecraft/world/entity/player/Inventory;)Lnet/minecraft/world/inventory/AbstractContainerMenu;", at = @At("HEAD"),
            cancellable = true)
    private void fallow$avecSesDonnees(int containerId, Inventory inventory, CallbackInfoReturnable<T> callback) {
        // Un menu sans données (celui de la table d'empennage d'Overgeared, ouvert par le même chemin) revient ici par la
        // création avec données de NeoForge, qui le crée sans elles : on le laisse alors se créer.
        if (dedans) {
            return;
        }
        RegistryFriendlyByteBuf data = MenusAvecDonnees.donnees((MenuType<?>) (Object) this);
        if (data != null) {
            @SuppressWarnings("unchecked")
            IMenuTypeExtension<T> self = (IMenuTypeExtension<T>) (Object) this;
            dedans = true;
            try {
                callback.setReturnValue(self.create(containerId, inventory, data));
            } finally {
                dedans = false;
                data.release();
            }
        }
    }
}
