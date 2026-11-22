package io.github.salvagedisc.fallow.tannerie;

import io.github.salvagedisc.fallow.Fallow;
import io.github.salvagedisc.fallow.commun.Foyers;
import io.github.salvagedisc.fallow.FallowConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Module tannerie : de la peau brute au cuir, avec trois blocs (docs/plan/tannerie.md).
 *
 * <ol>
 *   <li>Le chevalet : la peau brute s'y racle au couteau ({@link ChevaletBlock}), et laisse parfois du gras.</li>
 *   <li>La cuve à tan : eau et écorce, les peaux raclées y trempent quelques minutes ({@link CuveBlock}).</li>
 *   <li>Le séchoir : le cuir mouillé y sèche, vite au soleil ou près d'un feu, pas sous la pluie ({@link SechoirBlock}).</li>
 * </ol>
 *
 * <p>L'écorce vient des bûches écorcées à la hache (celle de Farmer's Delight, qui sort aussi de sa planche à
 * découper). Les blocs et les objets sont toujours enregistrés ; l'interrupteur du module ne fait qu'arrêter le travail.
 */
public final class TannerieModule {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Fallow.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Fallow.MOD_ID);

    /** Ce qui donne du tan dans la cuve : l'écorce. */
    public static final TagKey<Item> TANINS = TagKey.create(Registries.ITEM, id("tanins"));
    /** Les couteaux qui raclent, ceux de Farmer's Delight et des autres mods. */
    public static final TagKey<Item> COUTEAUX = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "tools/knife"));
    /** Les feux qui sèchent le cuir, les mêmes que ceux du repos. */
    public static final TagKey<Block> FOYERS = Foyers.TAG;
    /** Les bûches qui ne donnent pas d'écorce (déjà écorcées, ou tenues à part par un pack). */
    public static final TagKey<Block> SANS_ECORCE = TagKey.create(Registries.BLOCK, id("sans_ecorce"));

    static final ResourceLocation ECORCE = ResourceLocation.fromNamespaceAndPath("farmersdelight", "tree_bark");
    public static final ResourceLocation GRAS = ResourceLocation.fromNamespaceAndPath("naturalist", "fat");

    public static final DeferredItem<Item> PEAU_BRUTE = ITEMS.registerSimpleItem("peau_brute");
    public static final DeferredItem<Item> PEAU_RACLEE = ITEMS.registerSimpleItem("peau_raclee");
    public static final DeferredItem<Item> CUIR_MOUILLE = ITEMS.registerSimpleItem("cuir_mouille");

    public static final DeferredBlock<ChevaletBlock> CHEVALET = BLOCKS.register("chevalet",
            () -> new ChevaletBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.5F)
                    .sound(SoundType.WOOD).noOcclusion().ignitedByLava()));
    public static final DeferredBlock<CuveBlock> CUVE_A_TAN = BLOCKS.register("cuve_a_tan",
            () -> new CuveBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F)
                    .sound(SoundType.WOOD).noOcclusion().ignitedByLava()));
    public static final DeferredBlock<SechoirBlock> SECHOIR = BLOCKS.register("sechoir",
            () -> new SechoirBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.0F)
                    .sound(SoundType.WOOD).noOcclusion().ignitedByLava().pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY)));

    public static final DeferredItem<BlockItem> CHEVALET_ITEM = ITEMS.registerSimpleBlockItem(CHEVALET);
    public static final DeferredItem<BlockItem> CUVE_A_TAN_ITEM = ITEMS.registerSimpleBlockItem(CUVE_A_TAN);
    public static final DeferredItem<BlockItem> SECHOIR_ITEM = ITEMS.registerSimpleBlockItem(SECHOIR);

    private TannerieModule() {
    }

    static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Fallow.MOD_ID, path);
    }

    public static void start(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener(BuildCreativeModeTabContentsEvent.class, TannerieModule::onCreativeTab);
        NeoForge.EVENT_BUS.addListener(BlockEvent.BlockToolModificationEvent.class, TannerieModule::onStrip);
        Fallow.LOGGER.info("tannerie : chevalet, cuve à tan et séchoir");
    }

    private static void onCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(CHEVALET_ITEM);
            event.accept(CUVE_A_TAN_ITEM);
            event.accept(SECHOIR_ITEM);
        } else if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(PEAU_BRUTE);
            event.accept(PEAU_RACLEE);
            event.accept(CUIR_MOUILLE);
        }
    }

    /** Une bûche écorcée à la hache lâche parfois un morceau d'écorce, à ses pieds. */
    private static void onStrip(BlockEvent.BlockToolModificationEvent event) {
        if (event.isSimulated() || event.getItemAbility() != ItemAbilities.AXE_STRIP || !FallowConfig.tannerieEnabled()) {
            return;
        }
        if (!(event.getLevel() instanceof Level level) || level.isClientSide()) {
            return;
        }
        BlockState before = event.getState();
        if (before.is(SANS_ECORCE)) {
            return;
        }
        // L'événement passe avant que le bloc donne son état écorcé : on le lui demande (sans repasser par l'événement).
        BlockState after = event.getFinalState() != before ? event.getFinalState()
                : before.getBlock().getToolModifiedState(before, event.getContext(), ItemAbilities.AXE_STRIP, true);
        if (after == null || after.getBlock() == before.getBlock()) {
            return;
        }
        if (level.getRandom().nextDouble() >= FallowConfig.tannerieBarkChance()) {
            return;
        }
        Item bark = BuiltInRegistries.ITEM.getOptional(ECORCE).orElse(Items.AIR);
        if (bark == Items.AIR) {
            return;
        }
        Block.popResourceFromFace(level, event.getPos(), event.getContext().getClickedFace(), new ItemStack(bark));
    }
}
