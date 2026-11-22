package io.github.salvagedisc.fallow.mixin;

import io.github.salvagedisc.fallow.Fallow;
import io.github.salvagedisc.fallow.ModIds;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * N'applique un mixin que si les mods qu'il vise sont présents. La table {@link #REQUIRED} associe
 * chaque mixin (nom relatif au paquet {@code mixin}) aux identifiants de mods qu'il suppose.
 * Un mixin absent de la table s'applique toujours.
 *
 * <p>Les mixins qui visent l'intérieur d'un mod tiers listent aussi, dans {@link #MEMBERS}, les champs et méthodes
 * qu'ils supposent : si une mise à jour du mod en retire un, le mixin est écarté avec un avertissement dans le journal
 * au lieu d'empêcher le jeu de démarrer (un {@code @Shadow} manquant plante même avec {@code require = 0}).
 */
public final class FallowMixinPlugin implements IMixinConfigPlugin {
    private static final String PACKAGE = "io.github.salvagedisc.fallow.mixin.";

    private static final Map<String, List<String>> REQUIRED = Map.ofEntries(
            Map.entry("ciel.LevelRendererMixin", List.of(ModIds.ATMOSPHERICS, ModIds.ENHANCED_CELESTIALS)),
            Map.entry("ciel.RenderSystemMixin", List.of(ModIds.ATMOSPHERICS, ModIds.ENHANCED_CELESTIALS)),
            Map.entry("ciel.BiomeBlendUtilInvoker", List.of(ModIds.ATMOSPHERICS)),
            Map.entry("ciel.ClientPacketListenerMixin", List.of(ModIds.ATMOSPHERICS)),
            Map.entry("ciel.AtmosphericsFadeMixin", List.of(ModIds.ATMOSPHERICS)),
            Map.entry("ciel.AtmosphericsSkyGateMixin", List.of(ModIds.ATMOSPHERICS, ModIds.IRIS)),
            Map.entry("ciel.ShaderSkyMixin", List.of(ModIds.ATMOSPHERICS, ModIds.IRIS)),
            Map.entry("clairiere.AtmosphericsBiomeMixin", List.of(ModIds.ATMOSPHERICS)),
            Map.entry("clairiere.MusicBiomeMixin", List.of(ModIds.TRUE_ADAPTIVE_MUSIC)),
            Map.entry("clairiere.SodiumTintMixin", List.of(ModIds.SODIUM)),
            Map.entry("horizon.FogRendererMixin", List.of(ModIds.DISTANT_HORIZONS)),
            Map.entry("horizon.GLStateMixin", List.of(ModIds.DISTANT_HORIZONS)),
            Map.entry("horizon.VanillaFadeMixin", List.of(ModIds.DISTANT_HORIZONS)),
            Map.entry("horizon.FarFadeMixin", List.of(ModIds.DISTANT_HORIZONS)),
            Map.entry("horizon.ChunkyAccessorMixin", List.of(ModIds.DISTANT_HORIZONS, ModIds.CHUNKY)),
            Map.entry("rendu.IrisUniformsMixin", List.of(ModIds.IRIS)),
            Map.entry("compat.PunchyEquipMixin", List.of(ModIds.PUNCHY, ModIds.SENSIBLE_STACKABLES)),
            Map.entry("compat.IceThawMixin", List.of(ModIds.STREAMS_REFLOWING, ModIds.SPELUNKERY)),
            Map.entry("compat.InStructureMixin", List.of(ModIds.LITHOSTITCHED)),
            Map.entry("dinos.ChorusoatlArmorMixin", List.of(ModIds.ARCHIES_DINOSAURS)),
            Map.entry("dinos.ArmorModelCacheMixin", List.of(ModIds.ARCHIES_DINOSAURS)),
            Map.entry("zoo.CategoryTabsMixin", List.of(ModIds.ARCHIES_DINOSAURS)),
            Map.entry("zoo.GuidebookDataMixin", List.of(ModIds.ARCHIES_DINOSAURS)),
            Map.entry("eveil.LivreDeTableMixin", List.of(ModIds.DIEGESIS)),
            Map.entry("eveil.PosteDeTableMixin", List.of(ModIds.DIEGESIS)),
            Map.entry("postes.ForgeRendererMixin", List.of(ModIds.OVERGEARED, ModIds.DIEGESIS)),
            Map.entry("postes.MenuOuvertureMixin", List.of(ModIds.DIEGESIS)),
            Map.entry("postes.MenuCreationMixin", List.of(ModIds.DIEGESIS)),
            Map.entry("postes.StationHoverMixin", List.of(ModIds.DIEGESIS, ModIds.HANDS_ON_VANILLA)),
            Map.entry("livres.HovOpenBookMixin", List.of(ModIds.HANDS_ON_VANILLA)),
            Map.entry("livres.HovRecipeBookMixin", List.of(ModIds.HANDS_ON_VANILLA)),
            Map.entry("livres.HovRecipeSheetMixin", List.of(ModIds.HANDS_ON_VANILLA)),
            Map.entry("livres.HovLecternCanvasMixin", List.of(ModIds.HANDS_ON_VANILLA)),
            Map.entry("navires.VoileMixin", List.of(ModIds.FISH_N_SHIPS)));

    private static final Map<String, List<String>> MEMBERS = Map.ofEntries(
            Map.entry("horizon.ChunkyAccessorMixin", List.of("listenerBound", "tryRunFirstTimeSetup")),
            Map.entry("dinos.ChorusoatlArmorMixin", List.of("hasAbility", "activateServerSetAbility")),
            Map.entry("dinos.ArmorModelCacheMixin", List.of("initArmorModel", "getLayerLocation")),
            Map.entry("zoo.CategoryTabsMixin", List.of("book", "categorySubTabs", "<init>")),
            Map.entry("zoo.GuidebookDataMixin", List.of("parseXML", "loadData")),
            Map.entry("compat.PunchyEquipMixin", List.of("shouldIgnoreComponentForEquipDiff")),
            Map.entry("postes.StationHoverMixin", List.of("isOurs")),
            Map.entry("livres.HovOpenBookMixin", List.of("draw", "sheet", "spread", "fromSpread", "flip", "flipWay", "time", "riffleAt", "riffleSpan")),
            Map.entry("livres.HovRecipeBookMixin", List.of("spread", "flip", "flipWay", "footSide", "cursorX", "cursorY", "page", "pages")),
            Map.entry("livres.HovRecipeSheetMixin", List.of("arrow", "paper", "poseStack", "buffers")),
            Map.entry("livres.HovLecternCanvasMixin", List.of("arrow", "poseStack", "buffers", "light", "wide", "high")),
            Map.entry("navires.VoileMixin", List.of("moveWithSailDown")));

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        String shortName = mixinClassName.startsWith(PACKAGE) ? mixinClassName.substring(PACKAGE.length()) : mixinClassName;
        List<String> mods = REQUIRED.getOrDefault(shortName, List.of());
        for (String mod : mods) {
            if (!ModIds.isLoaded(mod)) {
                Fallow.LOGGER.debug("Mixin {} écarté : {} absent", shortName, mod);
                return false;
            }
        }
        List<String> members = MEMBERS.get(shortName);
        if (members != null) {
            String missing = missingMember(targetClassName, members);
            if (missing != null) {
                Fallow.LOGGER.warn("Mixin {} écarté : {} introuvable dans {} (le mod a-t-il changé de version ?)",
                        shortName, missing, targetClassName);
                return false;
            }
        }
        return true;
    }

    /** Le premier des champs ou méthodes attendus qui manque à la classe visée ; {@code null} si tous sont là. */
    private static String missingMember(String targetClassName, List<String> members) {
        ClassNode node;
        try {
            node = MixinService.getService().getBytecodeProvider().getClassNode(targetClassName);
        } catch (Throwable e) {
            // Impossible de lire la classe ici : on laisse Mixin décider, comme avant.
            return null;
        }
        for (String member : members) {
            boolean found = node.fields.stream().map((FieldNode field) -> field.name).anyMatch(member::equals)
                    || node.methods.stream().map((MethodNode method) -> method.name).anyMatch(member::equals);
            if (!found) {
                return member;
            }
        }
        return null;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
