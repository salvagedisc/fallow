package io.github.salvagedisc.fallow.hotes;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.github.salvagedisc.fallow.Fallow;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Module hôtes (pilote, hors dépôt) : les peuples de la brume qui viennent rendre visite. Ce pilote ne montre que le
 * corps (modèle du joueur, animé par Fresh Animations, armure de la bête gardienne) et la fenêtre d'échange en maquette.
 */
public final class HotesModule {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Fallow.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Fallow.MOD_ID);
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, Fallow.MOD_ID);

    /** Chaque masque se porte comme un casque : protection d'un casque de cuir, réparé avec du bois. */
    public static final Map<Peuple, DeferredHolder<ArmorMaterial, ArmorMaterial>> MATERIAUX = new EnumMap<>(Peuple.class);
    public static final Map<Peuple, DeferredItem<ArmorItem>> MASQUES = new EnumMap<>(Peuple.class);

    static {
        for (Peuple peuple : Peuple.values()) {
            DeferredHolder<ArmorMaterial, ArmorMaterial> materiau = ARMOR_MATERIALS.register("masque_" + peuple.id(),
                    () -> new ArmorMaterial(
                            Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                                for (ArmorItem.Type type : ArmorItem.Type.values()) {
                                    map.put(type, 1);
                                }
                            }),
                            15, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(ItemTags.LOGS),
                            List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Fallow.MOD_ID, "masque_" + peuple.id()))),
                            0.0F, 0.0F));
            MATERIAUX.put(peuple, materiau);
            MASQUES.put(peuple, ITEMS.register(peuple.masque(),
                    () -> new ArmorItem(materiau, ArmorItem.Type.HELMET,
                            new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(8)).rarity(Rarity.UNCOMMON))));
        }
    }

    public static final DeferredHolder<EntityType<?>, EntityType<Hote>> HOTE = ENTITY_TYPES.register("hote",
            () -> EntityType.Builder.of(Hote::new, MobCategory.MISC)
                    .sized(0.6F, 1.8F)
                    .eyeHeight(1.62F)
                    .clientTrackingRange(10)
                    .build("hote"));

    private HotesModule() {
    }

    public static void start(IEventBus modBus) {
        ENTITY_TYPES.register(modBus);
        ITEMS.register(modBus);
        ARMOR_MATERIALS.register(modBus);
        modBus.addListener(BuildCreativeModeTabContentsEvent.class, event -> {
            if (event.getTabKey() == CreativeModeTabs.COMBAT) {
                MASQUES.values().forEach(event::accept);
            }
        });
        modBus.addListener(EntityAttributeCreationEvent.class, event -> event.put(HOTE.get(), Hote.attributes().build()));
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, HotesModule::onRegisterCommands);
        if (FMLEnvironment.dist.isClient()) {
            io.github.salvagedisc.fallow.hotes.client.HotesClient.start(modBus);
        }
    }

    /** {@code /fallow hotes appeler <peuple>} : un Hôte paraît à 14 blocs devant le joueur, et s'avance. */
    private static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(Fallow.MOD_ID)
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("hotes")
                        .then(Commands.literal("appeler")
                                .then(Commands.argument("peuple", StringArgumentType.word())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                Arrays.stream(Peuple.values()).map(Peuple::id), builder))
                                        .executes(HotesModule::appeler)))));
    }

    private static int appeler(CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        Peuple peuple = Peuple.parse(StringArgumentType.getString(context, "peuple")).orElse(null);
        if (peuple == null) {
            context.getSource().sendFailure(Component.translatable("commands.fallow.hotes.inconnu"));
            return 0;
        }
        ServerLevel level = player.serverLevel();
        Vec3 dir = Vec3.directionFromRotation(0.0F, player.getYRot());
        Vec3 pos = player.position().add(dir.scale(14.0));
        int y = sol(level, (int) Math.floor(pos.x), player.getBlockY(), (int) Math.floor(pos.z));
        if (y == Integer.MIN_VALUE) {
            // Rien où se tenir devant (une falaise, la lave, le vide) : il vient à côté de vous, sinon là où vous êtes.
            Vec3 cote = player.position().add(dir.scale(2.0));
            int pres = sol(level, (int) Math.floor(cote.x), player.getBlockY(), (int) Math.floor(cote.z));
            pos = pres == Integer.MIN_VALUE ? player.position() : cote;
            y = pres == Integer.MIN_VALUE ? player.getBlockY() : pres;
        }
        Hote hote = HOTE.get().create(level);
        if (hote == null) {
            return 0;
        }
        float yaw = (float) Math.toDegrees(Math.atan2(-(-dir.x), -dir.z));
        hote.moveTo(pos.x, y, pos.z, yaw, 0.0F);
        hote.setYHeadRot(yaw);
        hote.setYBodyRot(yaw);
        hote.setPeuple(peuple);
        level.addFreshEntity(hote);
        context.getSource().sendSuccess(() -> Component.translatable("commands.fallow.hotes.appeler",
                Component.translatable("screen.fallow.hote." + peuple.id() + ".nom"),
                Component.translatable("screen.fallow.hote." + peuple.id() + ".titre")), true);
        return 1;
    }

    /**
     * Le sol où l'Hôte se tient, à hauteur du joueur : dans une grotte ou sous le toit du Nether, pas à la surface. Le
     * premier sol libre (deux blocs d'air au-dessus) à huit blocs près ; à défaut, aucun ({@code Integer.MIN_VALUE}).
     */
    private static int sol(ServerLevel level, int x, int pres, int z) {
        net.minecraft.core.BlockPos.MutableBlockPos curseur = new net.minecraft.core.BlockPos.MutableBlockPos();
        for (int d = 0; d <= 8; d++) {
            for (int y : d == 0 ? new int[] {pres} : new int[] {pres + d, pres - d}) {
                curseur.set(x, y - 1, z);
                if (level.getBlockState(curseur).isFaceSturdy(level, curseur, net.minecraft.core.Direction.UP)
                        && libre(level, curseur.setY(y)) && libre(level, curseur.setY(y + 1))) {
                    return y;
                }
            }
        }
        // Pas le dessus du monde : sous le toit du Nether c'est la roche du toit, depuis une grotte c'est la surface.
        return Integer.MIN_VALUE;
    }

    /** Ni bloc ni liquide : un Hôte ne se tient pas au fond d'un lac ou sous la lave. */
    private static boolean libre(ServerLevel level, net.minecraft.core.BlockPos pos) {
        net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
        return state.getCollisionShape(level, pos).isEmpty() && state.getFluidState().isEmpty();
    }
}
