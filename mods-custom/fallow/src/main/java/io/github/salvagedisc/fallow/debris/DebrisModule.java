package io.github.salvagedisc.fallow.debris;

import io.github.salvagedisc.fallow.Fallow;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Module débris : ce qui traîne au sol et se ramasse main vide (docs/plan/premiers-gestes.md). Des cailloux de la roche
 * du sol, du bois mort de l'essence de l'arbre au-dessus, des pommes de pin sous les conifères. Ils remplacent les galets
 * et les brindilles de Twigs, qui étaient les mêmes partout.
 *
 * <p>Le module enregistre les blocs et le type d'élément de génération {@code fallow:debris} ; où et combien, ce sont des
 * données du pack ({@code pack/kubejs/data/fallow/worldgen/}). Les modèles des cailloux, du bois mort et de la pomme de
 * pin viennent de This Rocks! (MidnightDust, MIT) et prennent les textures du jeu.
 */
public final class DebrisModule {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Fallow.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Fallow.MOD_ID);
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Fallow.MOD_ID);

    public static final DeferredHolder<Feature<?>, DebrisFeature> DEBRIS =
            FEATURES.register("debris", () -> new DebrisFeature(DebrisConfig.CODEC));

    /** Les cailloux, du plus courant au plus rare. */
    public static final Map<String, DeferredBlock<CaillouBlock>> CAILLOUX = new LinkedHashMap<>();
    /** Le bois mort, une essence par bois du jeu. */
    public static final Map<String, DeferredBlock<BoisMortBlock>> BOIS_MORT = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<BlockItem>> OBJETS = new LinkedHashMap<>();

    static {
        caillou("caillou", MapColor.STONE, SoundType.STONE);
        caillou("caillou_de_granite", MapColor.DIRT, SoundType.STONE);
        caillou("caillou_de_diorite", MapColor.QUARTZ, SoundType.STONE);
        caillou("caillou_d_andesite", MapColor.STONE, SoundType.STONE);
        caillou("caillou_de_gres", MapColor.SAND, SoundType.STONE);
        caillou("caillou_de_gres_rouge", MapColor.COLOR_ORANGE, SoundType.STONE);
        caillou("gravillon", MapColor.STONE, SoundType.GRAVEL);
        caillou("eclat_de_glace", MapColor.ICE, SoundType.GLASS);
        for (String bois : new String[] {"chene", "bouleau", "epicea", "acajou", "acacia", "chene_noir", "paletuvier", "cerisier"}) {
            String id = (bois.startsWith("e") || bois.startsWith("a") ? "bois_mort_d_" : "bois_mort_de_") + bois;
            DeferredBlock<BoisMortBlock> block = BLOCKS.register(id, () -> new BoisMortBlock(debris(MapColor.WOOD)
                    .sound(bois.equals("cerisier") ? SoundType.CHERRY_WOOD : SoundType.WOOD).ignitedByLava()));
            BOIS_MORT.put(id, block);
            OBJETS.put(id, ITEMS.registerSimpleBlockItem(block));
        }
    }

    public static final DeferredBlock<PommeDePinBlock> POMME_DE_PIN = enregistrer("pomme_de_pin",
            () -> new PommeDePinBlock(debris(MapColor.PODZOL).sound(SoundType.WOOD).ignitedByLava()));

    private DebrisModule() {
    }

    private static void caillou(String id, MapColor couleur, SoundType son) {
        DeferredBlock<CaillouBlock> block = enregistrer(id, () -> new CaillouBlock(debris(couleur).sound(son)));
        CAILLOUX.put(id, block);
    }

    private static <B extends DebrisBlock> DeferredBlock<B> enregistrer(String id, Supplier<B> bloc) {
        DeferredBlock<B> block = BLOCKS.register(id, bloc);
        OBJETS.put(id, ITEMS.registerSimpleBlockItem(block));
        return block;
    }

    /** Ce que les débris ont en commun : on passe au travers, ils se cassent d'un coup, un piston les écrase. */
    private static BlockBehaviour.Properties debris(MapColor couleur) {
        return BlockBehaviour.Properties.of().mapColor(couleur).instabreak().noCollission().noOcclusion()
                .offsetType(BlockBehaviour.OffsetType.XZ).dynamicShape().pushReaction(PushReaction.DESTROY);
    }

    public static void start(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        FEATURES.register(modBus);
        modBus.addListener(BuildCreativeModeTabContentsEvent.class, DebrisModule::onCreativeTab);
        Fallow.LOGGER.info("débris : {} cailloux, {} bois morts et la pomme de pin", CAILLOUX.size(), BOIS_MORT.size());
    }

    private static void onCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            OBJETS.values().forEach(event::accept);
        }
    }
}
