package slimeknights.tconstruct.tools.item;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;
import slimeknights.tconstruct.common.Sounds;
import slimeknights.tconstruct.tools.TinkerTools;

import java.util.ArrayList;
import java.util.List;

/**
 * Internal item used by crystalshot modifier
 */
public class CrystalshotItem extends ArrowItem {
    /**
     * Possible variants for a random crystalshot, so addons can register their own if desired
     */
    public static final List<String> RANDOM_VARIANTS;

    static {
        RANDOM_VARIANTS = new ArrayList<>();
        RANDOM_VARIANTS.add("amethyst");
        RANDOM_VARIANTS.add("earthslime");
        RANDOM_VARIANTS.add("skyslime");
        RANDOM_VARIANTS.add("ichor");
        RANDOM_VARIANTS.add("enderslime");
        RANDOM_VARIANTS.add("quartz");
    }

    /**
     * NBT key for variants on the stack and entity
     */
    private static final String TAG_VARIANT = "variant";

    public CrystalshotItem(Settings props) {
        super(props);
    }

    @Override
    public PersistentProjectileEntity createArrow(World pLevel, ItemStack pStack, LivingEntity pShooter) {
        CrystalshotEntity arrow = new CrystalshotEntity(pLevel, pShooter);
        String variant = "random";
        NbtCompound tag = pStack.getNbt();
        if (tag != null) {
            variant = tag.getString(TAG_VARIANT);
        }
        if ("random".equals(variant)) {
            variant = RANDOM_VARIANTS.get(pShooter.getRandom().nextInt(RANDOM_VARIANTS.size()));
        }
        arrow.setVariant(variant);
        return arrow;
    }



    @Override
    public boolean isInfinite(ItemStack stack, ItemStack bow, PlayerEntity player) {
        return EnchantmentHelper.getLevel(net.minecraft.enchantment.Enchantments.INFINITY, bow) > 0;
    }



    @Override
    public void fillItemCategory(ItemGroup pCategory, DefaultedList<ItemStack> pItems) {
    }

    /**
     * Creates a crystal shot with the given variant
     */
    public static ItemStack withVariant(String variant, int size) {
        ItemStack stack = new ItemStack(TinkerTools.crystalshotItem);
        stack.setCount(size);
        stack.getOrCreateNbt().putString(TAG_VARIANT, variant);
        return stack;
    }

    public static class CrystalshotEntity extends PersistentProjectileEntity {
        private static final TrackedData<String> SYNC_VARIANT = DataTracker.registerData(CrystalshotEntity.class, TrackedDataHandlerRegistry.STRING);

        public CrystalshotEntity(EntityType<? extends CrystalshotEntity> type, World level) {
            super(type, level);
            this.pickupType = PickupPermission.CREATIVE_ONLY;
            this.setSound(Sounds.CRYSTALSHOT.getSound());
        }

        public CrystalshotEntity(World level, LivingEntity shooter) {
            super(TinkerTools.crystalshotEntity.get(), shooter, level);
            this.pickupType = PickupPermission.CREATIVE_ONLY;
            this.setSound(Sounds.CRYSTALSHOT.getSound());
        }

        @Override
        public void setSound(SoundEvent sound) {
            if (sound != SoundEvents.ENTITY_ARROW_HIT && sound != SoundEvents.ITEM_CROSSBOW_HIT) {
                super.setSound(sound);
            }
        }

        @Override
        protected void initDataTracker() {
            super.initDataTracker();
            this.dataTracker.startTracking(SYNC_VARIANT, "");
        }

        /**
         * Gets the texture variant of this shot
         */
        public String getVariant() {
            String variant = this.dataTracker.get(SYNC_VARIANT);
            if (variant.isEmpty()) {
                return "amethyst";
            }
            return variant;
        }

        /**
         * Sets the arrow's variant
         */
        public void setVariant(String variant) {
            this.dataTracker.set(SYNC_VARIANT, variant);
        }

        @Override
        public ItemStack asItemStack() {
            return withVariant(this.getVariant(), 1);
        }

        @Override
        public void writeCustomDataToNbt(NbtCompound tag) {
            super.writeCustomDataToNbt(tag);
            tag.putString(TAG_VARIANT, this.getVariant());
        }

        @Override
        public void readCustomDataFromNbt(NbtCompound tag) {
            super.readCustomDataFromNbt(tag);
            this.setVariant(tag.getString(TAG_VARIANT));
        }
    }
}
