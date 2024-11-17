package slimeknights.tconstruct.world.entity;

import org.jetbrains.annotations.Nullable;

import java.time.LocalDate;
import java.time.temporal.ChronoField;

import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributeModifier.Operation;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;

public abstract class ArmoredSlimeEntity extends SlimeEntity {
    public ArmoredSlimeEntity(EntityType<? extends SlimeEntity> type, World world) {
        super(type, world);
        if (!world.isClient) {
            tryAddAttribute(EntityAttributes.GENERIC_ARMOR, new EntityAttributeModifier("tconstruct.small_armor_bonus", 3, Operation.MULTIPLY_TOTAL));
            tryAddAttribute(EntityAttributes.GENERIC_ARMOR_TOUGHNESS, new EntityAttributeModifier("tconstruct.small_toughness_bonus", 3, Operation.MULTIPLY_TOTAL));
            tryAddAttribute(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, new EntityAttributeModifier("tconstruct.small_resistence_bonus", 3, Operation.MULTIPLY_TOTAL));
        }
    }

    /**
     * Adds an attribute if possible
     */
    private void tryAddAttribute(EntityAttribute attribute, EntityAttributeModifier modifier) {
        EntityAttributeInstance instance = getAttributeInstance(attribute);
        if (instance != null) {
            instance.addTemporaryModifier(modifier);
        }
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess pLevel, LocalDifficulty difficulty, SpawnReason pReason, @Nullable EntityData pSpawnData, @Nullable NbtCompound pDataTag) {
        EntityData spawnData = super.initialize(pLevel, difficulty, pReason, pSpawnData, pDataTag);
        this.setCanPickUpLoot(this.random.nextFloat() < (0.55f * difficulty.getClampedLocalDifficulty()));

        this.initEquipment(random, difficulty);

        // pumpkins on halloween
        if (this.getEquippedStack(EquipmentSlot.HEAD).isEmpty()) {
            LocalDate localdate = LocalDate.now();
            if (localdate.get(ChronoField.MONTH_OF_YEAR) == 10 && localdate.get(ChronoField.DAY_OF_MONTH) == 31 && this.random.nextFloat() < 0.25F) {
                this.equipStack(EquipmentSlot.HEAD, new ItemStack(this.random.nextFloat() < 0.1F ? Blocks.JACK_O_LANTERN : Blocks.CARVED_PUMPKIN));
                this.armorDropChances[EquipmentSlot.HEAD.getEntitySlotId()] = 0.0F;
            }
        }

        return spawnData;
    }

    @Override
    protected abstract void initEquipment(Random random, LocalDifficulty difficulty);

    @Override
    protected void updateEnchantments(Random random, LocalDifficulty difficulty) {
        // no-op, unused
    }

    @Override
    public boolean canPickupItem(ItemStack stack) {
        // only pick up items that go in the head slot, don't have a renderer for other slots
        return getPreferredEquipmentSlot(stack) == EquipmentSlot.HEAD;
    }

    @Override
    protected void dropEquipment(DamageSource source, int looting, boolean recentlyHit) {
        ItemStack stack = this.getEquippedStack(EquipmentSlot.HEAD);
        float slotChance = this.getDropChance(EquipmentSlot.HEAD);
        // items do not always drop if a large slime, increases chance of inheritance
        // small slimes always drop, no losing gear
        if (slotChance > 0.25f && getSize() > 1) {
            slotChance = 0.25f;
        }
        boolean alwaysDrop = slotChance > 1.0F;
        if (!stack.isEmpty() && !EnchantmentHelper.hasVanishingCurse(stack) && (recentlyHit || alwaysDrop) && (this.random.nextFloat() - (looting * 0.01f)) < slotChance) {
            if (!alwaysDrop && stack.isDamageable()) {
                int max = stack.getMaxDamage();
                stack.setDamage(max - this.random.nextInt(1 + this.random.nextInt(Math.max(max - 3, 1))));
            }
            this.dropStack(stack);
            this.equipStack(EquipmentSlot.HEAD, ItemStack.EMPTY);
        }
    }

    @SuppressWarnings("IntegerDivisionInFloatingPointContext")
    @Override
    public void remove(Entity.RemovalReason reason) {
        // on death, split into multiple slimes, and let them inherit armor if it did not drop
        int size = this.getSize();
        if (!this.world.isClient && size > 1 && this.isDead()) {
            Text name = this.getCustomName();
            boolean noAi = this.isAiDisabled();
            boolean invulnerable = this.isInvulnerable();
            float offset = size / 4.0F;
            int newSize = size / 2;
            int count = 2 + this.random.nextInt(3);
            // determine which child will receive the helmet
            ItemStack helmet = getEquippedStack(EquipmentSlot.HEAD);
            int helmetIndex = -1;
            if (!helmet.isEmpty()) {
                helmetIndex = this.random.nextInt(count);
            }

            // spawn all children
            float dropChance = getDropChance(EquipmentSlot.HEAD);
            for (int i = 0; i < count; ++i) {
                float x = ((i % 2) - 0.5F) * offset;
                float z = ((i / 2) - 0.5F) * offset;
                SlimeEntity slime = this.getType().create(this.world);
                assert slime != null;
                if (this.isPersistent()) {
                    slime.setPersistent();
                }
                slime.setCustomName(name);
                slime.setAiDisabled(noAi);
                slime.setInvulnerable(invulnerable);
                slime.setSize(newSize, true);
                if (i == helmetIndex) {
                    slime.equipStack(EquipmentSlot.HEAD, helmet.copy());
                    equipStack(EquipmentSlot.HEAD, ItemStack.EMPTY);
                } else if (dropChance < 1 && random.nextFloat() < 0.25) {
                    slime.equipStack(EquipmentSlot.HEAD, helmet.copy());
                }
                slime.refreshPositionAndAngles(this.getX() + x, this.getY() + 0.5D, this.getZ() + z, this.random.nextFloat() * 360.0F, 0.0F);
                this.world.spawnEntity(slime);
            }
        }

        // calling supper does the split reason again, but we need to transfer armor
        this.setRemoved(reason);
        if (reason == Entity.RemovalReason.KILLED) {
            this.emitGameEvent(GameEvent.ENTITY_DIE);
        }
        this.invalidateCaps();
    }
}
