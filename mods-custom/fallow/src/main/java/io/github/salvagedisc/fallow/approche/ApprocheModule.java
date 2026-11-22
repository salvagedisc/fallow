package io.github.salvagedisc.fallow.approche;

import io.github.salvagedisc.fallow.Fallow;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Module approche : les bêtes farouches sentent le joueur quand le vent porte son odeur vers elles, l'entendent
 * quand il court, le voient quand il se tient debout à découvert, et fuient (docs/plan/approche.md). Le vent est
 * celui du module vent ({@link io.github.salvagedisc.fallow.vent.WindField}), que le serveur calcule comme les
 * clients. Seules les espèces du tag {@code fallow:farouches} sont concernées : jamais les bêtes de la ferme, ni
 * une bête apprivoisée, tenue en laisse ou montée.
 */
public final class ApprocheModule {
    public static final TagKey<EntityType<?>> FAROUCHES =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(Fallow.MOD_ID, "farouches"));

    private ApprocheModule() {
    }

    public static void start(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(EntityJoinLevelEvent.class, event -> {
            if (!event.getLevel().isClientSide() && event.getEntity() instanceof PathfinderMob mob
                    && mob.getType().is(FAROUCHES)
                    && mob.goalSelector.getAvailableGoals().stream().noneMatch(goal -> goal.getGoal() instanceof FuiteGoal)) {
                mob.goalSelector.addGoal(1, new FuiteGoal(mob));
            }
        });
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, event -> Approche.tick(event.getServer()));
        Fallow.LOGGER.info("approche : les bêtes farouches sentent le joueur sous le vent");
    }
}
