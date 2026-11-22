package io.github.salvagedisc.fallow.gabarits.client;

import io.github.salvagedisc.fallow.Fallow;
import io.github.salvagedisc.fallow.FallowConfig;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import org.jetbrains.annotations.Nullable;

/**
 * Le pack intégré du module gabarits : caché, toujours actif, posé tout en haut. Il ne contient rien d'écrit : à chaque
 * chargement des ressources, au premier appel, il lit dans les autres packs actifs le gabarit (le fer tel que le pack de
 * textures le dessine), la référence (le fer du jeu) et la texture du mod, et fait les images des {@link Regle règles}.
 *
 * <p>Une texture n'est refaite que si le gabarit vient d'un pack de textures (sinon le jeu est tel quel et le mod
 * aussi) et que la texture du mod vient bien du mod (un pack de textures qui la redessine garde la main).
 */
public final class GabaritsPack extends AbstractPackResources {
    static final String ID = Fallow.MOD_ID + "_gabarits";
    private static final String VANILLA = "vanilla";
    private static final ResourceLocation STICK = ResourceLocation.withDefaultNamespace("textures/item/stick.png");
    private static final byte[] META = ("{\"pack\":{\"pack_format\":34,\"description\":\"Fallow : métaux des mods d'après le"
            + " pack de textures actif\"}}").getBytes(StandardCharsets.UTF_8);
    private static List<Regle> regles;

    private final Object lock = new Object();
    private Map<ResourceLocation, byte[]> made;

    private GabaritsPack(PackLocationInfo location) {
        super(location);
    }

    public static void register(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) {
            return;
        }
        PackLocationInfo info = new PackLocationInfo(ID, Component.literal("Fallow : gabarits"), PackSource.BUILT_IN,
                Optional.empty());
        Pack.ResourcesSupplier supplier = new Pack.ResourcesSupplier() {
            @Override
            public PackResources openPrimary(PackLocationInfo location) {
                return new GabaritsPack(location);
            }

            @Override
            public PackResources openFull(PackLocationInfo location, Pack.Metadata metadata) {
                return new GabaritsPack(location);
            }
        };
        Pack pack = Pack.readMetaAndCreate(info, supplier, PackType.CLIENT_RESOURCES,
                new PackSelectionConfig(true, Pack.Position.TOP, false));
        if (pack == null) {
            Fallow.LOGGER.warn("gabarits : pack intégré refusé");
            return;
        }
        Pack hidden = pack.hidden();
        event.addRepositorySource(consumer -> consumer.accept(hidden));
    }

    private static synchronized List<Regle> regles() {
        if (regles == null) {
            regles = Regle.load();
        }
        return regles;
    }

    @Override
    public @Nullable IoSupplier<InputStream> getRootResource(String... elements) {
        if (elements.length == 1 && PACK_META.equals(elements[0])) {
            return () -> new ByteArrayInputStream(META);
        }
        return null;
    }

    @Override
    public @Nullable IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
        if (type != PackType.CLIENT_RESOURCES) {
            return null;
        }
        byte[] png = made().get(location);
        return png == null ? null : () -> new ByteArrayInputStream(png);
    }

    @Override
    public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
        if (type != PackType.CLIENT_RESOURCES) {
            return;
        }
        String prefix = path.endsWith("/") ? path : path + "/";
        for (Map.Entry<ResourceLocation, byte[]> entry : made().entrySet()) {
            ResourceLocation id = entry.getKey();
            if (id.getNamespace().equals(namespace) && id.getPath().startsWith(prefix)) {
                byte[] png = entry.getValue();
                output.accept(id, () -> new ByteArrayInputStream(png));
            }
        }
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        Set<String> namespaces = new HashSet<>();
        if (type == PackType.CLIENT_RESOURCES) {
            for (Regle regle : regles()) {
                namespaces.add(regle.target().getNamespace());
            }
        }
        return namespaces;
    }

    @Override
    public void close() {
    }

    private Map<ResourceLocation, byte[]> made() {
        synchronized (lock) {
            if (made == null) {
                made = new HashMap<>();
                try {
                    if (FallowConfig.gabaritsEnabled()) {
                        make(made);
                    }
                } catch (RuntimeException e) {
                    Fallow.LOGGER.warn("gabarits : échec, les textures des mods restent telles quelles", e);
                    made.clear();
                }
            }
            return made;
        }
    }

    /** Les packs actifs, du plus haut au plus bas, sans celui-ci. */
    private static List<PackResources> packs() {
        List<PackResources> packs = new ArrayList<>(Minecraft.getInstance().getResourceManager().listPacks()
                .filter(p -> !ID.equals(p.packId())).toList());
        java.util.Collections.reverse(packs);
        return packs;
    }

    private static boolean isMod(PackResources pack) {
        String id = pack.packId();
        return id.equals("mod_resources") || id.startsWith("mod/");
    }

    private static boolean isResourcePack(PackResources pack) {
        return !isMod(pack) && !VANILLA.equals(pack.packId());
    }

    private static void make(Map<ResourceLocation, byte[]> out) {
        List<PackResources> packs = packs();
        PackResources vanilla = packs.stream().filter(p -> VANILLA.equals(p.packId())).findFirst().orElse(null);
        if (vanilla == null) {
            return;
        }
        Lecteur lecteur = new Lecteur(packs, vanilla);
        double[][] templateHandle = Teinte.palette(lecteur.top(STICK));
        double[][] refHandle = Teinte.palette(lecteur.vanilla(STICK));
        Map<String, Integer> skipped = new TreeMap<>();
        for (Regle regle : regles()) {
            String why;
            try {
                why = make(regle, lecteur, templateHandle, refHandle, out);
            } catch (IOException | RuntimeException e) {
                why = "illisible";
                Fallow.LOGGER.debug("gabarits : {} illisible", regle.target(), e);
            }
            if (why != null) {
                skipped.merge(why, 1, Integer::sum);
                Fallow.LOGGER.debug("gabarits : {} laissé ({})", regle.target(), why);
            }
        }
        Fallow.LOGGER.info("gabarits : {} textures de mods refaites d'après le pack de textures actif, laissées : {}",
                out.size(), skipped);
        if (Boolean.getBoolean("fallow.gabarits.dump")) {
            dump(out);
        }
    }

    /** Fait l'image d'une règle ; rend la raison si on la laisse. */
    private static @Nullable String make(Regle regle, Lecteur lecteur, double[][] templateHandle, double[][] refHandle,
            Map<ResourceLocation, byte[]> out) throws IOException {
        PackResources sourcePack = lecteur.provider(regle.target());
        if (sourcePack == null) {
            return "absente";
        }
        if (!isMod(sourcePack)) {
            return "dessinée par un pack";
        }
        PackResources templatePack = lecteur.provider(regle.template());
        if (templatePack == null || !isResourcePack(templatePack)) {
            return "gabarit du jeu";
        }
        if (lecteur.read(sourcePack, ResourceLocation.fromNamespaceAndPath(regle.target().getNamespace(),
                regle.target().getPath() + ".mcmeta")) != null) {
            return "animée";
        }
        Planche source = lecteur.read(sourcePack, regle.target());
        Planche template = lecteur.read(templatePack, regle.template());
        if (source == null || template == null) {
            return "illisible";
        }
        if (!template.sameSize(source) && !regle.template().getPath().startsWith("textures/block/")) {
            return "taille";
        }
        ResourceLocation gateRef = regle.gateRef() != null ? regle.gateRef() : regle.ref();
        if (regle.gate() > 0 && gateRef != null && Teinte.overlap(source, lecteur.vanilla(gateRef)) < regle.gate()) {
            return "dessin propre";
        }
        Planche result;
        Planche ref = regle.ref() == null ? null : lecteur.vanilla(regle.ref());
        if (ref == null) {
            result = template.copy();
        } else {
            List<Planche> templateOthers = new ArrayList<>();
            List<Planche> refOthers = new ArrayList<>();
            for (ResourceLocation other : regle.others()) {
                Planche t = lecteur.top(other);
                Planche v = lecteur.vanilla(other);
                if (t != null) {
                    templateOthers.add(t);
                }
                if (v != null) {
                    refOthers.add(v);
                }
            }
            result = Teinte.transfer(template, templateOthers, ref, refOthers, source,
                    regle.handles() ? templateHandle : null, regle.handles() ? refHandle : null, regle.lightness(),
                    regle.chroma());
            if (result == null) {
                return "trop peu de métal";
            }
        }
        if (regle.extras() && result.sameSize(source)) {
            for (int i = 0; i < result.size(); i++) {
                if (result.a[i] == 0 && source.a[i] > 0) {
                    result.r[i] = source.r[i];
                    result.g[i] = source.g[i];
                    result.b[i] = source.b[i];
                    result.a[i] = source.a[i];
                }
            }
        }
        out.put(regle.target(), result.png());
        return null;
    }

    /** Pour vérifier les images faites : {@code -Dfallow.gabarits.dump=true} les écrit dans {@code gabarits/} du dossier du jeu. */
    private static void dump(Map<ResourceLocation, byte[]> out) {
        java.nio.file.Path dir = Minecraft.getInstance().gameDirectory.toPath().resolve("gabarits");
        for (Map.Entry<ResourceLocation, byte[]> entry : out.entrySet()) {
            try {
                java.nio.file.Path file = dir.resolve(entry.getKey().getNamespace()).resolve(entry.getKey().getPath());
                java.nio.file.Files.createDirectories(file.getParent());
                java.nio.file.Files.write(file, entry.getValue());
            } catch (IOException e) {
                Fallow.LOGGER.warn("gabarits : écriture de {} impossible", entry.getKey(), e);
            }
        }
    }

    /** Lit les images dans les packs actifs, une seule fois chacune. */
    private static final class Lecteur {
        private final List<PackResources> packs;
        private final PackResources vanilla;
        private final Map<String, Optional<Planche>> cache = new HashMap<>();

        Lecteur(List<PackResources> packs, PackResources vanilla) {
            this.packs = packs;
            this.vanilla = vanilla;
        }

        @Nullable PackResources provider(ResourceLocation id) {
            for (PackResources pack : packs) {
                if (pack.getResource(PackType.CLIENT_RESOURCES, id) != null) {
                    return pack;
                }
            }
            return null;
        }

        @Nullable Planche top(ResourceLocation id) {
            PackResources pack = provider(id);
            return pack == null ? null : read(pack, id);
        }

        @Nullable Planche vanilla(ResourceLocation id) {
            return read(vanilla, id);
        }

        @Nullable Planche read(PackResources pack, ResourceLocation id) {
            return cache.computeIfAbsent(pack.packId() + "|" + id, k -> {
                IoSupplier<InputStream> supplier = pack.getResource(PackType.CLIENT_RESOURCES, id);
                if (supplier == null) {
                    return Optional.empty();
                }
                if (id.getPath().endsWith(".mcmeta")) {
                    return Optional.of(new Planche(0, 0));
                }
                try (InputStream in = supplier.get()) {
                    return Optional.of(Planche.read(in));
                } catch (IOException | RuntimeException e) {
                    Fallow.LOGGER.debug("gabarits : {} illisible dans {}", id, pack.packId(), e);
                    return Optional.empty();
                }
            }).orElse(null);
        }
    }
}
