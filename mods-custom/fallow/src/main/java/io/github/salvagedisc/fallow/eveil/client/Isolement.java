package io.github.salvagedisc.fallow.eveil.client;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * À la table d'éveil, le monde s'en va : une brume violette presque noire se referme à quelques blocs, le ciel et le lointain
 * disparaissent, les bruits du monde se taisent presque, et la vue s'ouvre quand on s'assoit (le fond semble reculer) puis se
 * resserre pendant le rite. La table et le livre restent ; en partant, le monde revient lentement. Mené à chaque image par
 * {@link TableBehavior}, lu par les mixins de brume, de ciel, de son et de champ de vision de {@code mixin.eveil}.
 */
public final class Isolement {
    /**
     * La brume en blocs depuis la caméra : le voile quand on s'assoit (0,6), le fond quand le monde est tout à fait parti (1) ; les
     * bibliothèques, à deux ou trois blocs, s'y effacent : ne restent que la table et le livre, à un ou deux blocs.
     */
    private static final float VOILE = 0.6F;
    private static final float VOILE_DEBUT = 1.8F;
    private static final float VOILE_FIN = 6.5F;
    private static final float FOND_DEBUT = 1.0F;
    private static final float FOND_FIN = 3.5F;
    /** La couleur de la brume, un violet proche du noir. */
    private static final float ROUGE = 0.045F;
    private static final float VERT = 0.028F;
    private static final float BLEU = 0.085F;
    /** Le plus bas où le monde descend (part de son volume), et jusqu'où un bruit est encore celui de la table, en blocs. */
    private static final float SILENCE = 0.08F;
    private static final double PRES_DE_LA_TABLE = 2.5;
    /** Le temps que le monde met à partir et à revenir, secondes. */
    private static final float ALLER = 1.4F;
    private static final float RETOUR = 3.0F;
    /** La vue s'ouvre d'autant quand on s'assoit, puis se pose ; elle se resserre d'autant au plus fort du rite. */
    private static final float ELAN = 0.10F;
    private static final float ELAN_DUREE = 2.0F;
    private static final float SERRE = 0.10F;

    private static float valeur;
    private static float cible;
    /** Les images passées sans que la table vise : au-delà de deux, elle n'est plus là (compté en images, pas en temps, pour les machines lentes). */
    private static int sansTable = 3;
    private static Vec3 centre = Vec3.ZERO;
    private static float serre;
    private static float serreCible;
    private static float elanT = -1.0F;
    private static float relache;
    private static long dernier;
    private static float applique;

    private Isolement() {
    }

    /** Appelé à chaque image par la table : combien du monde est parti (0 à 1), où est la table, et où en est le rite (0 à 1). */
    static void viser(float k, Vec3 table, float rite) {
        cible = k;
        sansTable = 0;
        centre = table;
        serreCible = rite;
    }

    /** On s'assoit à la table : la vue prend son souffle. */
    static void arriver() {
        elanT = 0.0F;
    }

    /** L'éveil s'accomplit : la vue se relâche d'un coup. */
    static void relacher() {
        relache = 1.0F;
        serre = 0.0F;
    }

    /** À chaque image, sur le fil de rendu. */
    static void avancer() {
        long now = Util.getMillis();
        float dt = dernier == 0L ? 0.0F : Math.min(0.5F, (now - dernier) / 1000.0F);
        dernier = now;
        float vise = sansTable < 2 ? cible : 0.0F;
        sansTable = Math.min(10, sansTable + 1);
        valeur = Mth.approach(valeur, vise, dt / (vise > valeur ? ALLER : RETOUR));
        serre = Mth.approach(serre, vise > 0.0F ? serreCible : 0.0F, dt / (serreCible > serre ? 1.8F : 0.4F));
        relache = Math.max(0.0F, relache - dt / 0.8F);
        if (elanT >= 0.0F) {
            elanT = vise > 0.0F && elanT < ELAN_DUREE ? elanT + dt : -1.0F;
        }
        // Les bruits qui jouent déjà suivent : un passage sur les canaux quand le silence a bougé.
        if (Math.abs(valeur - applique) > 0.01F || valeur == 0.0F && applique != 0.0F) {
            applique = valeur;
            Minecraft minecraft = Minecraft.getInstance();
            minecraft.getSoundManager().updateSourceVolume(SoundSource.AMBIENT, minecraft.options.getSoundSourceVolume(SoundSource.AMBIENT));
        }
    }

    public static boolean actif() {
        return valeur > 0.001F;
    }

    /** Jusqu'au voile, 0 à 1, adouci ; la racine garde la brume proche jusqu'au bout du retour, et la fait venir vite. */
    private static float doux() {
        float f = (float) Math.sqrt(Math.min(1.0F, valeur / VOILE));
        return f * f * (3.0F - 2.0F * f);
    }

    /**
     * La distance de la brume : du monde au voile en proportion (chaque pas rapproche autant à l'œil, de loin comme de près), puis du
     * voile au fond ; jamais plus loin que celle du monde.
     */
    private static float distance(float monde, float voile, float fond) {
        float f = doux();
        float d = (float) (Math.pow(Math.max(1.0F, monde), 1.0F - f) * Math.pow(voile, f));
        if (valeur > VOILE) {
            d = Mth.lerp((valeur - VOILE) / (1.0F - VOILE), voile, fond);
        }
        return Math.min(monde, d);
    }

    /** Le début et la fin de la brume à cette image, depuis ceux du monde. */
    public static float debut(float monde) {
        return distance(monde, VOILE_DEBUT, FOND_DEBUT);
    }

    public static float fin(float monde) {
        return distance(monde, VOILE_FIN, FOND_FIN);
    }

    /** La brume du ciel se referme plus vite que celle du terrain : le ciel part avant la terre. */
    public static float finDuCiel(float monde) {
        return Mth.lerp(Math.min(1.0F, doux() * 1.6F), monde, 0.0F);
    }

    public static float rouge(float monde) {
        return Mth.lerp(doux(), monde, ROUGE);
    }

    public static float vert(float monde) {
        return Mth.lerp(doux(), monde, VERT);
    }

    public static float bleu(float monde) {
        return Mth.lerp(doux(), monde, BLEU);
    }

    /** Ni soleil, ni lune, ni étoiles quand le monde est presque parti. */
    public static boolean cielCache() {
        return valeur > 0.55F;
    }

    /** Ce que garde un bruit : ceux de la table et de l'interface gardent tout, le reste du monde se tait. */
    public static float volume(SoundInstance sound) {
        if (!actif() || sound.getSource() == SoundSource.MASTER) {
            return 1.0F;
        }
        if (sound.getClass().getName().startsWith("com.pockyl.diegesis.")) {
            return 1.0F;
        }
        if (!sound.isRelative() && sound.getSource() == SoundSource.BLOCKS
                && centre.distanceToSqr(sound.getX(), sound.getY(), sound.getZ()) < PRES_DE_LA_TABLE * PRES_DE_LA_TABLE) {
            return 1.0F;
        }
        return 1.0F - (1.0F - SILENCE) * Math.min(1.0F, valeur * 1.25F);
    }

    /** Le facteur du champ de vision : ouvert en s'asseyant, serré quand le rite monte, relâché à l'éclair. */
    public static double champ() {
        if (!actif() && relache <= 0.0F) {
            return 1.0;
        }
        float effets = Minecraft.getInstance().options.fovEffectScale().get().floatValue();
        float elan = elanT >= 0.0F ? Mth.sin(Mth.PI * Math.min(1.0F, elanT / ELAN_DUREE)) : 0.0F;
        elan *= elan;
        float k = 1.0F + ELAN * elan - SERRE * serre * serre + 0.05F * relache * relache;
        return 1.0 + (k - 1.0) * effets;
    }
}
