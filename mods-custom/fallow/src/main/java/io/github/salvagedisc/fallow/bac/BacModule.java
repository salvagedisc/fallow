package io.github.salvagedisc.fallow.bac;

import dev.ryanhcode.sable.platform.SableEventPlatform;
import io.github.salvagedisc.fallow.Fallow;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Module bac : le bac à traille. Une corde tendue d'un pieu à l'autre au-dessus d'un fleuve, une poulie sur un radeau
 * de quelques blocs : le radeau devient un navire de Sable qui ne fait que glisser le long de la corde, et le courant
 * de Streams, pris de biais, le fait traverser ({@link Traille}). Demande Sable.
 */
public final class BacModule {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Fallow.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Fallow.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Fallow.MOD_ID);

    /** Ce qui sert de corde de bac : les cordes de Farmer's Delight et de Supplementaries, la laisse. */
    public static final TagKey<Item> CORDES = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Fallow.MOD_ID, "cordes_de_bac"));
    private static final ResourceLocation CORDE_FD = ResourceLocation.fromNamespaceAndPath("farmersdelight", "rope");

    public static final DeferredBlock<PieuBlock> PIEU = BLOCKS.register("pieu_de_bac",
            () -> new PieuBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.0F)
                    .sound(SoundType.WOOD).noOcclusion().ignitedByLava()));
    public static final DeferredBlock<PoulieBlock> POULIE = BLOCKS.register("poulie_de_bac",
            () -> new PoulieBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(1.5F)
                    .sound(SoundType.WOOD).noOcclusion().ignitedByLava()));
    public static final DeferredItem<BlockItem> PIEU_ITEM = ITEMS.registerSimpleBlockItem(PIEU);
    public static final DeferredItem<BlockItem> POULIE_ITEM = ITEMS.registerSimpleBlockItem(POULIE);
    @SuppressWarnings("DataFlowIssue") // le type de données est facultatif (null), comme partout en 1.21.1
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PieuBlockEntity>> PIEU_BE =
            BLOCK_ENTITIES.register("pieu_de_bac", () -> BlockEntityType.Builder.of(PieuBlockEntity::new, PIEU.get()).build(null));

    private BacModule() {
    }

    public static void start(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(BuildCreativeModeTabContentsEvent.class, event -> {
            if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
                event.accept(PIEU_ITEM);
                event.accept(POULIE_ITEM);
            }
        });
        SableEventPlatform.INSTANCE.onPhysicsTick(Bac::onPhysicsTick);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            io.github.salvagedisc.fallow.bac.client.BacClient.start(modBus);
        }
        Fallow.LOGGER.info("bac : pieux, corde et poulie de bac");
    }

    /** La corde rendue quand on défait une corde de bac : celle de Farmer's Delight, sinon une laisse. */
    static Item cordeRendue() {
        return BuiltInRegistries.ITEM.getOptional(CORDE_FD).orElse(Items.LEAD);
    }
}
