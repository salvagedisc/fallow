package io.github.salvagedisc.fallow.eveil;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import io.github.salvagedisc.fallow.FallowConfig;

/**
 * L'attachement d'un objet à qui en prend soin, dit en un mot dans son infobulle : négligé, entretenu, chéri (rien entre les
 * deux premiers). Il monte quand on le répare avant qu'il soit à moitié usé, qu'on le nomme (une fois), qu'il passe la nuit chez
 * soi (cadre, support d'armure) et qu'on le reprend après une mort ; il baisse quand on le laisse descendre dans le rouge, traîner
 * au sol ou dans le feu, ou qu'il reste longtemps dans d'autres mains. Entretenu, il n'oublie plus son dernier éveil à bout de
 * forces ; chéri, il gagne une place d'éveil. Les points sont des notes de {@link Actes} ; l'objet ne porte que le mot
 * ({@link EveilModule#LIEN}), qui ne change qu'en passant un seuil.
 */
public final class Lien {
    public static final int NEGLIGE = -1;
    public static final int AUCUN = 0;
    public static final int ENTRETENU = 1;
    public static final int CHERI = 2;

    static final String POINTS = "lien";
    static final String NOMME = "nomme";
    static final String NUIT = "nuit";
    static final String MAITRE = "maitre";

    private static final double SEUIL_ENTRETENU = 30.0;
    private static final double SEUIL_CHERI = 100.0;
    private static final double PLANCHER = -60.0;
    private static final double PLAFOND = 160.0;

    /** Ce que valent les gestes, en points d'attachement. */
    static final double REPARE_TOT = 12.0;
    static final double REPARE = 4.0;
    static final double NOMME_POINTS = 25.0;
    static final double NUIT_CHEZ_SOI = 5.0;
    static final double REPRIS = 10.0;
    static final double ROUGE = -12.0;
    static final double AU_SOL = -1.0;
    static final double FEU = -15.0;
    static final double PRETE = -4.0;

    private Lien() {
    }

    static int niveau(double points) {
        return points < 0.0 ? NEGLIGE : points < SEUIL_ENTRETENU ? AUCUN : points < SEUIL_CHERI ? ENTRETENU : CHERI;
    }

    /** Le mot que porte l'objet (rien : {@link #AUCUN}). */
    public static int de(ItemStack stack) {
        Integer niveau = stack.get(EveilModule.LIEN.get());
        return niveau == null ? AUCUN : niveau;
    }

    /**
     * Ajoute (ou retire) des points d'attachement à l'objet ; s'il passe un seuil, son mot change, ses places suivent, et le
     * joueur qui le porte l'apprend.
     */
    static void ajouter(MinecraftServer server, ItemStack stack, EtatEveil etat, double delta, ServerPlayer porteur) {
        if (!FallowConfig.eveilEnabled() || delta == 0.0) {
            return;
        }
        Actes actes = Actes.get(server);
        double avant = actes.note(etat.id(), POINTS);
        double apres = Math.max(PLANCHER, Math.min(PLAFOND, avant + delta));
        if (apres == avant) {
            return;
        }
        actes.noter(etat.id(), POINTS, apres);
        int niveau = niveau(apres);
        if (niveau == de(stack)) {
            return;
        }
        int ancien = de(stack);
        if (niveau == AUCUN) {
            stack.remove(EveilModule.LIEN.get());
        } else {
            stack.set(EveilModule.LIEN.get(), niveau);
        }
        places(stack);
        if (porteur != null) {
            boolean monte = niveau > ancien;
            if (monte && niveau >= ENTRETENU || !monte && niveau == NEGLIGE) {
                porteur.displayClientMessage(Component.translatable("eveil.fallow.lien.message." + cle(niveau), stack.getHoverName())
                        .withStyle(monte ? ChatFormatting.GREEN : ChatFormatting.GRAY), true);
                porteur.playNotifySound(monte ? SoundEvents.AMETHYST_BLOCK_CHIME : SoundEvents.AMETHYST_CLUSTER_STEP,
                        SoundSource.PLAYERS, 0.8F, monte ? 1.2F : 0.6F);
            }
        }
    }

    public static String cle(int niveau) {
        return switch (niveau) {
            case NEGLIGE -> "neglige";
            case ENTRETENU -> "entretenu";
            case CHERI -> "cheri";
            default -> "aucun";
        };
    }

    /**
     * Les places d'éveil de l'objet : sa qualité de forge plus son métal ({@link #metal}), cinq au plus, plus une s'il est chéri ;
     * jamais moins que ce qu'il porte déjà.
     */
    static void places(ItemStack stack) {
        EtatEveil etat = stack.get(EveilModule.ETAT.get());
        if (etat == null) {
            return;
        }
        int voulues = Math.min(5, FallowConfig.eveilPlaces(Eveil.qualite(stack)) + metal(stack)) + (de(stack) == CHERI ? 1 : 0);
        voulues = Math.max(voulues, etat.occupees());
        if (voulues != etat.places()) {
            stack.set(EveilModule.ETAT.get(), etat.avecPlaces(voulues));
        }
    }

    /**
     * Ce que le métal ajoute, d'après l'échelle de progression : rien pour le fer, le cuivre et l'or, une place pour l'acier
     * d'Overgeared et l'acier noir (l'acier bleui), deux pour le diamant, trois pour l'orichalque (la netherite). Lu sur le nom
     * de l'objet, pour suivre les outils et armures de chaque mod sans dépendre d'eux. Les armures d'Archie sont des bêtes, pas
     * du métal : rien.
     */
    static int metal(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String nom = id.getPath();
        if (nom.startsWith("netherite_")) {
            return 3;
        }
        if (nom.startsWith("diamond_")) {
            return 2;
        }
        if (nom.endsWith("_en_acier_noir") || nom.startsWith("steel_") && "overgeared".equals(id.getNamespace())) {
            return 1;
        }
        return 0;
    }
}
