package io.github.salvagedisc.fallow.hotes.visite;

import io.github.salvagedisc.fallow.Fallow;
import io.github.salvagedisc.fallow.hotes.Hote;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Ce que le serveur et la fenêtre d'échange se disent. Le serveur ouvre la fenêtre (ou la met à jour) avec les souhaits
 * de la visite ; le client répond par un don ({@link Donner}) ou un refus poli ({@link Refuser}), que le serveur vérifie.
 */
public final class Echanges {
    /** Au-delà, le joueur est trop loin de l'émissaire pour lui tendre quelque chose. */
    private static final double PORTEE = 8.0;

    private Echanges() {
    }

    /**
     * La fenêtre d'échange avec l'émissaire {@code entite} : ses souhaits, ceux déjà remplis (un bit chacun), si un refus
     * poli est encore possible cette saison, et s'il est là pour une visite (sinon, on ne fait que parler).
     */
    public record Echange(int entite, List<Demande> demandes, int donnes, boolean refusPossible, boolean enVisite,
            boolean ouvrir) implements CustomPacketPayload {
        public static final Type<Echange> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Fallow.MOD_ID, "hotes_echange"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Echange> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Echange::entite,
                Demande.STREAM_CODEC.apply(ByteBufCodecs.list(3)), Echange::demandes,
                ByteBufCodecs.VAR_INT, Echange::donnes,
                ByteBufCodecs.BOOL, Echange::refusPossible,
                ByteBufCodecs.BOOL, Echange::enVisite,
                ByteBufCodecs.BOOL, Echange::ouvrir,
                Echange::new);

        public boolean donne(int index) {
            return (donnes & (1 << index)) != 0;
        }

        @Override
        public Type<Echange> type() {
            return TYPE;
        }
    }

    /** Le joueur pose sur la natte le souhait {@code index}. */
    public record Donner(int entite, int index) implements CustomPacketPayload {
        public static final Type<Donner> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Fallow.MOD_ID, "hotes_donner"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Donner> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Donner::entite,
                ByteBufCodecs.VAR_INT, Donner::index,
                Donner::new);

        @Override
        public Type<Donner> type() {
            return TYPE;
        }
    }

    /** Le joueur dit poliment qu'il n'a rien à laisser cette fois. */
    public record Refuser(int entite) implements CustomPacketPayload {
        public static final Type<Refuser> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Fallow.MOD_ID, "hotes_refuser"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Refuser> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Refuser::entite,
                Refuser::new);

        @Override
        public Type<Refuser> type() {
            return TYPE;
        }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToClient(Echange.TYPE, Echange.STREAM_CODEC, (echange, context) -> context.enqueueWork(
                        () -> io.github.salvagedisc.fallow.hotes.client.HotesClient.echange(echange)))
                .playToServer(Donner.TYPE, Donner.STREAM_CODEC, (don, context) -> context.enqueueWork(() -> donner(don, context)))
                .playToServer(Refuser.TYPE, Refuser.STREAM_CODEC, (refus, context) -> context.enqueueWork(() -> refuser(refus, context)));
    }

    /** Ouvre la fenêtre d'échange du joueur avec cet émissaire (ou la met à jour). */
    public static void envoyer(ServerPlayer player, Hote hote, boolean ouvrir) {
        ServerLevel level = player.serverLevel();
        Visite visite = Liens.de(level).visiteDe(hote.getUUID());
        if (visite == null || visite.phase != Visite.Phase.VENUE) {
            PacketDistributor.sendToPlayer(player, new Echange(hote.getId(), List.of(), 0, false, false, ouvrir));
            return;
        }
        PacketDistributor.sendToPlayer(player, new Echange(hote.getId(), List.copyOf(visite.demandes), visite.donnes,
                Visites.refusPossible(level, visite), true, ouvrir));
    }

    private static Hote hote(ServerPlayer player, int id) {
        Entity entite = player.level().getEntity(id);
        if (entite instanceof Hote hote && !hote.isRemoved() && hote.distanceTo(player) <= PORTEE) {
            return hote;
        }
        return null;
    }

    private static void donner(Donner don, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        Hote hote = hote(player, don.entite());
        if (hote == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        Liens liens = Liens.de(level);
        Visite visite = liens.visiteDe(hote.getUUID());
        if (visite == null || visite.phase != Visite.Phase.VENUE || don.index() < 0 || don.index() >= visite.demandes.size()
                || visite.donne(don.index()) || visite.natte == null || visite.refus) {
            envoyer(player, hote, false);
            return;
        }
        Demande demande = visite.demandes.get(don.index());
        BlockEntity be = level.getBlockEntity(visite.natte);
        if (!(be instanceof NatteBlockEntity natte) || compte(player, demande) < demande.nombre()) {
            envoyer(player, hote, false);
            return;
        }
        natte.pose(retire(player, demande));
        visite.donnes |= 1 << don.index();
        liens.change();
        Visites.donne(level, visite, player);
        envoyer(player, hote, false);
    }

    private static void refuser(Refuser refus, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        Hote hote = hote(player, refus.entite());
        if (hote == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        Visite visite = Liens.de(level).visiteDe(hote.getUUID());
        if (visite != null && visite.phase == Visite.Phase.VENUE) {
            Visites.refuse(level, visite);
        }
        envoyer(player, hote, false);
    }

    /** Combien le joueur porte de ce que demande le souhait. */
    public static int compte(net.minecraft.world.entity.player.Player player, Demande demande) {
        int n = 0;
        for (ItemStack pile : player.getInventory().items) {
            if (demande.accepte(pile)) {
                n += pile.getCount();
            }
        }
        return n;
    }

    /** Retire de l'inventaire le nombre demandé et rend les piles retirées, pour la natte. */
    private static List<ItemStack> retire(ServerPlayer player, Demande demande) {
        List<ItemStack> out = new java.util.ArrayList<>();
        int reste = demande.nombre();
        for (ItemStack pile : player.getInventory().items) {
            if (reste <= 0) {
                break;
            }
            if (demande.accepte(pile)) {
                out.add(pile.split(Math.min(reste, pile.getCount())));
                reste -= out.get(out.size() - 1).getCount();
            }
        }
        player.getInventory().setChanged();
        return out;
    }
}
