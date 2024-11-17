package slimeknights.tconstruct.library.modifiers;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.netty.handler.codec.DecoderException;
import lombok.RequiredArgsConstructor;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraftforge.network.NetworkEvent.Context;
import net.minecraftforge.registries.ForgeRegistries;
import slimeknights.mantle.network.packet.IThreadsafePacket;
import slimeknights.tconstruct.library.utils.GenericTagUtil;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Packet to sync modifiers
 */
@RequiredArgsConstructor
public class UpdateModifiersPacket implements IThreadsafePacket {
    /**
     * Collection of all modifiers
     */
    private final Map<ModifierId, Modifier> allModifiers;
    /**
     * Map of all modifier tags
     */
    private final Map<TagKey<Modifier>, List<Modifier>> tags;
    /**
     * Collection of non-redirect modifiers
     */
    private Collection<Modifier> modifiers;
    /**
     * Map of modifier redirect ID pairs
     */
    private Map<ModifierId, ModifierId> redirects;
    /**
     * Map of enchantment to modifier pair
     */
    private final Map<Enchantment, Modifier> enchantmentMap;
    /**
     * Collection of all enchantment tag mappings
     */
    private final Map<TagKey<Enchantment>, Modifier> enchantmentTagMappings;

    /**
     * Ensures both the modifiers and redirects lists are calculated, allows one packet to be used multiple times without redundant work
     */
    private void ensureCalculated() {
        if (this.modifiers == null || this.redirects == null) {
            ImmutableList.Builder<Modifier> modifiers = ImmutableList.builder();
            ImmutableMap.Builder<ModifierId, ModifierId> redirects = ImmutableMap.builder();
            for (Entry<ModifierId, Modifier> entry : allModifiers.entrySet()) {
                ModifierId id = entry.getKey();
                Modifier mod = entry.getValue();
                if (id.equals(mod.getId())) {
                    modifiers.add(mod);
                } else {
                    redirects.put(id, mod.getId());
                }
            }
            this.modifiers = modifiers.build();
            this.redirects = redirects.build();
        }
    }

    /**
     * Gets a modifier by the given ID, falling back to the map if needed
     */
    private static Modifier getModifier(Map<ModifierId, Modifier> modifiers, ModifierId id) {
        Modifier modifier = ModifierManager.INSTANCE.getStatic(id);
        if (modifier == ModifierManager.INSTANCE.getDefaultValue()) {
            modifier = modifiers.get(id);
            if (modifier == null) {
                throw new DecoderException("Unknown modifier " + id);
            }
        }
        return modifier;
    }

    public UpdateModifiersPacket(PacketByteBuf buffer) {
        // read in modifiers
        int size = buffer.readVarInt();
        Map<ModifierId, Modifier> modifiers = new HashMap<>();
        for (int i = 0; i < size; i++) {
            ModifierId id = new ModifierId(buffer.readString(Short.MAX_VALUE));
            Modifier modifier = ModifierManager.MODIFIER_LOADERS.decode(buffer);
            modifier.setId(id);
            modifiers.put(id, modifier);
        }
        // read in redirects
        size = buffer.readVarInt();
        for (int i = 0; i < size; i++) {
            ModifierId from = new ModifierId(buffer.readString(Short.MAX_VALUE));
            modifiers.put(from, getModifier(modifiers, new ModifierId(buffer.readString(Short.MAX_VALUE))));
        }
        this.allModifiers = modifiers;
        this.tags = GenericTagUtil.decodeTags(buffer, ModifierManager.REGISTRY_KEY, id -> getModifier(modifiers, new ModifierId(id)));

        // read in enchantment to modifier mapping
        ImmutableMap.Builder<Enchantment, Modifier> enchantmentBuilder = ImmutableMap.builder();
        size = buffer.readVarInt();
        for (int i = 0; i < size; i++) {
            enchantmentBuilder.put(
                    buffer.readRegistryValue(Registries.ENCHANTMENT),
                    getModifier(modifiers, new ModifierId(buffer.readIdentifier())));
        }
        enchantmentMap = enchantmentBuilder.build();
        ImmutableMap.Builder<TagKey<Enchantment>, Modifier> enchantmentTagBuilder = ImmutableMap.builder();
        size = buffer.readVarInt();
        for (int i = 0; i < size; i++) {
            enchantmentTagBuilder.put(
                    TagKey.of(RegistryKeys.ENCHANTMENT, buffer.readIdentifier()),
                    getModifier(modifiers, new ModifierId(buffer.readIdentifier())));
        }
        enchantmentTagMappings = enchantmentTagBuilder.build();
    }

    @Override
    public void encode(PacketByteBuf buffer) {
        ensureCalculated();
        // write modifiers
        buffer.writeVarInt(this.modifiers.size());
        for (Modifier modifier : this.modifiers) {
            buffer.writeIdentifier(modifier.getId());
            ModifierManager.MODIFIER_LOADERS.encode(buffer, modifier);
        }
        // write redirects
        buffer.writeVarInt(this.redirects.size());
        for (Entry<ModifierId, ModifierId> entry : this.redirects.entrySet()) {
            buffer.writeIdentifier(entry.getKey());
            buffer.writeIdentifier(entry.getValue());
        }
        GenericTagUtil.encodeTags(buffer, Modifier::getId, this.tags);

        // enchantment mapping
        buffer.writeVarInt(this.enchantmentMap.size());
        for (Entry<Enchantment, Modifier> entry : this.enchantmentMap.entrySet()) {
            buffer.writeRegistryValue(Registries.ENCHANTMENT, entry.getKey());
            buffer.writeIdentifier(entry.getValue().getId());
        }
        buffer.writeVarInt(this.enchantmentTagMappings.size());
        for (Entry<TagKey<Enchantment>, Modifier> entry : this.enchantmentTagMappings.entrySet()) {
            buffer.writeIdentifier(entry.getKey().id());
            buffer.writeIdentifier(entry.getValue().getId());
        }
    }

    @Override
    public void handleThreadsafe(Context context) {
        ModifierManager.INSTANCE.updateModifiersFromServer(this.allModifiers, this.tags, this.enchantmentMap, this.enchantmentTagMappings);
    }
}
