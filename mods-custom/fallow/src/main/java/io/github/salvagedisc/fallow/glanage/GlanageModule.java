package io.github.salvagedisc.fallow.glanage;

import io.github.salvagedisc.fallow.Fallow;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * Module glanage : ce que le premier jour trouve sans rien poser au sol (docs/plan/premiers-gestes.md, décision 0052).
 * Un clic droit main vide, ou un bâton à la main (le premier ramassé arrive dans la main), sur des feuilles les secoue :
 * elles bruissent, perdent quelques feuilles et lâchent parfois un bâton. Chaque bloc de feuilles ne se secoue utilement
 * qu'une fois par jour. Les cailloux, eux, se trouvent en creusant la terre et le gravier : c'est le butin des blocs,
 * réglé par le pack (server_scripts/premiers_gestes.js).
 */
public final class GlanageModule {
    /** Chance qu'une secousse fasse tomber un bâton. */
    static final float CHANCE_BATON = 0.3F;
    /** Temps (ticks) avant qu'un même bloc de feuilles se secoue de nouveau utilement : un jour de jeu. */
    static final long REPOS_FEUILLES = 24000L;
    /** Au-delà, on oublie les secousses trop anciennes. */
    private static final int MEMOIRE = 4096;

    /** Dernière secousse utile de chaque bloc de feuilles, par dimension. Perdue au redémarrage, sans conséquence. */
    private static final Map<ResourceKey<Level>, Long2LongOpenHashMap> SECOUSSES = new HashMap<>();

    private GlanageModule() {
    }

    public static void start(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(PlayerInteractEvent.RightClickBlock.class, GlanageModule::onSecouer);
        NeoForge.EVENT_BUS.addListener(ServerStoppedEvent.class, event -> SECOUSSES.clear());
        Fallow.LOGGER.info("glanage : les feuilles se secouent main vide");
    }

    private static void onSecouer(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Player player = event.getEntity();
        ItemStack main = player.getMainHandItem();
        if (player.isSpectator() || player.isSecondaryUseActive() || !(main.isEmpty() || main.is(Tags.Items.RODS_WOODEN))) {
            return;
        }
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (!state.is(BlockTags.LEAVES)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide()));
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        SoundType son = state.getSoundType(server, pos, player);
        server.playSound(null, pos, son.getBreakSound(), SoundSource.BLOCKS, 0.45F,
                son.getPitch() * (0.85F + server.random.nextFloat() * 0.2F));
        server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5, 6, 0.3, 0.1, 0.3, 0.0);
        server.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);

        long maintenant = server.getGameTime();
        Long2LongOpenHashMap secousses = SECOUSSES.computeIfAbsent(server.dimension(), d -> new Long2LongOpenHashMap());
        long derniere = secousses.getOrDefault(pos.asLong(), Long.MIN_VALUE);
        if (derniere != Long.MIN_VALUE && maintenant - derniere < REPOS_FEUILLES) {
            return;
        }
        if (secousses.size() >= MEMOIRE) {
            secousses.values().removeIf(t -> maintenant - t >= REPOS_FEUILLES);
        }
        secousses.put(pos.asLong(), maintenant);
        if (server.random.nextFloat() < CHANCE_BATON) {
            Block.popResourceFromFace(server, pos, event.getFace() == null ? Direction.DOWN : event.getFace(),
                    new ItemStack(Items.STICK));
        }
    }
}
