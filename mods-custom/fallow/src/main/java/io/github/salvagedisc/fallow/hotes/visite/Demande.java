package io.github.salvagedisc.fallow.hotes.visite;

import com.google.gson.JsonObject;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

/**
 * Un souhait d'un peuple : un objet précis, ou n'importe quel objet d'un tag (montré par son icône), et le nombre
 * attendu. Lu dans {@code data/<espace>/hotes/<peuple>.json}, gardé dans la visite, envoyé à la fenêtre d'échange.
 */
public record Demande(Item icone, Optional<TagKey<Item>> tag, int nombre) {
    public static final StreamCodec<RegistryFriendlyByteBuf, Demande> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.registry(Registries.ITEM), Demande::icone,
            ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC.map(id -> TagKey.create(Registries.ITEM, id), TagKey::location)),
            Demande::tag,
            ByteBufCodecs.VAR_INT, Demande::nombre,
            Demande::new);

    /** Vrai si la pile compte pour ce souhait. */
    public boolean accepte(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return tag.map(stack::is).orElseGet(() -> stack.is(icone));
    }

    /** L'icône, au nombre demandé : ce que montre la fenêtre. */
    public ItemStack pile() {
        return new ItemStack(icone, Math.max(1, Math.min(nombre, 99)));
    }

    /** Le même souhait, au nombre multiplié (les peuples proches demandent un peu plus). */
    public Demande fois(float facteur) {
        return new Demande(icone, tag, Math.max(1, Math.round(nombre * facteur)));
    }

    public CompoundTag save() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("icone", BuiltInRegistries.ITEM.getKey(icone).toString());
        tag.ifPresent(t -> nbt.putString("tag", t.location().toString()));
        nbt.putInt("nombre", nombre);
        return nbt;
    }

    @Nullable
    public static Demande load(CompoundTag nbt) {
        Item icone = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(nbt.getString("icone")));
        if (icone == Items.AIR) {
            return null;
        }
        Optional<TagKey<Item>> tag = nbt.contains("tag")
                ? Optional.of(TagKey.create(Registries.ITEM, ResourceLocation.parse(nbt.getString("tag"))))
                : Optional.empty();
        return new Demande(icone, tag, Math.max(1, nbt.getInt("nombre")));
    }

    /**
     * {@code {"item": "minecraft:glow_berries", "nombre": 8}} ou {@code {"tag": "c:foods/cooked_meat", "icone":
     * "minecraft:cooked_mutton", "nombre": 4}} ; {@code null} si l'objet n'existe pas (mod absent).
     */
    @Nullable
    public static Demande lire(JsonObject json) {
        int nombre = GsonHelper.getAsInt(json, "nombre", 1);
        if (json.has("tag")) {
            Item icone = BuiltInRegistries.ITEM.get(ResourceLocation.parse(GsonHelper.getAsString(json, "icone")));
            if (icone == Items.AIR) {
                return null;
            }
            return new Demande(icone, Optional.of(TagKey.create(Registries.ITEM, ResourceLocation.parse(GsonHelper.getAsString(json, "tag")))), nombre);
        }
        ResourceLocation id = ResourceLocation.parse(GsonHelper.getAsString(json, "item"));
        if (!BuiltInRegistries.ITEM.containsKey(id)) {
            return null;
        }
        return new Demande(BuiltInRegistries.ITEM.get(id), Optional.empty(), nombre);
    }
}
