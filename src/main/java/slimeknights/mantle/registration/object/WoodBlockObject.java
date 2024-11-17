package slimeknights.mantle.registration.object;

import lombok.Getter;
import net.minecraft.block.*;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static slimeknights.mantle.registration.RegistrationHelper.getCastedHolder;
import static slimeknights.mantle.util.RegistryHelper.getHolder;

/**
 * Extension of the fence object with all other wood blocks
 */
public class WoodBlockObject extends FenceBuildingBlockObject {
    @Getter
    private final WoodType woodType;
    // basic
    private final Supplier<? extends Block> log;
    private final Supplier<? extends Block> strippedLog;
    private final Supplier<? extends Block> wood;
    private final Supplier<? extends Block> strippedWood;
    // doors
    private final Supplier<? extends FenceGateBlock> fenceGate;
    private final Supplier<? extends DoorBlock> door;
    private final Supplier<? extends TrapdoorBlock> trapdoor;
    // redstone
    private final Supplier<? extends PressurePlateBlock> pressurePlate;
    private final Supplier<? extends ButtonBlock> button;
    // signs
    private final Supplier<? extends SignBlock> sign;
    private final Supplier<? extends WallSignBlock> wallSign;
    // tags
    @Getter
    private final TagKey<Block> logBlockTag;
    @Getter
    private final TagKey<Item> logItemTag;

    public WoodBlockObject(Identifier name, WoodType woodType, BuildingBlockObject planks,
                           Supplier<? extends Block> log, Supplier<? extends Block> strippedLog, Supplier<? extends Block> wood, Supplier<? extends Block> strippedWood,
                           Supplier<? extends FenceBlock> fence, Supplier<? extends FenceGateBlock> fenceGate, Supplier<? extends DoorBlock> door, Supplier<? extends TrapdoorBlock> trapdoor,
                           Supplier<? extends PressurePlateBlock> pressurePlate, Supplier<? extends ButtonBlock> button,
                           Supplier<? extends SignBlock> sign, Supplier<? extends WallSignBlock> wallSign) {
        super(planks, fence);
        this.woodType = woodType;
        this.log = log;
        this.strippedLog = strippedLog;
        this.wood = wood;
        this.strippedWood = strippedWood;
        this.fenceGate = fenceGate;
        this.door = door;
        this.trapdoor = trapdoor;
        this.pressurePlate = pressurePlate;
        this.button = button;
        this.sign = sign;
        this.wallSign = wallSign;
        Identifier tagName = new Identifier(name.getNamespace(), name.getPath() + "_logs");
        this.logBlockTag = TagKey.of(RegistryKeys.BLOCK, tagName);
        this.logItemTag = TagKey.of(RegistryKeys.ITEM, tagName);
    }

    public WoodBlockObject(Identifier name, WoodType woodType, BuildingBlockObject planks,
                           Block log, Block strippedLog, Block wood, Block strippedWood,
                           Block fence, Block fenceGate, Block door, Block trapdoor,
                           Block pressurePlate, Block button, Block sign, Block wallSign) {
        super(planks, fence);
        this.woodType = woodType;
        this.log = getHolder(Registries.BLOCK, log)::value;
        this.strippedLog = getHolder(Registries.BLOCK, strippedLog)::value;
        this.wood = getHolder(Registries.BLOCK, wood)::value;
        this.strippedWood = getHolder(Registries.BLOCK, strippedWood)::value;
        this.fenceGate = getCastedHolder(Registries.BLOCK, fenceGate);
        this.door = getCastedHolder(Registries.BLOCK, door);
        this.trapdoor = getCastedHolder(Registries.BLOCK, trapdoor);
        this.pressurePlate = getCastedHolder(Registries.BLOCK, pressurePlate);
        this.button = getCastedHolder(Registries.BLOCK, button);
        this.sign = getCastedHolder(Registries.BLOCK, sign);
        this.wallSign = getCastedHolder(Registries.BLOCK, wallSign);
        Identifier tagName = new Identifier(name.getNamespace(), name.getPath() + "_logs");
        this.logBlockTag = TagKey.of(RegistryKeys.BLOCK, tagName);
        this.logItemTag = TagKey.of(RegistryKeys.ITEM, tagName);
    }

    /**
     * Gets the log for this wood type
     */
    public Block getLog() {
        return this.log.get();
    }

    /**
     * Gets the stripped log for this wood type
     */
    public Block getStrippedLog() {
        return this.strippedLog.get();
    }

    /**
     * Gets the wood for this wood type
     */
    public Block getWood() {
        return this.wood.get();
    }

    /**
     * Gets the stripped wood for this wood type
     */
    public Block getStrippedWood() {
        return this.strippedWood.get();
    }

    /* Doors */

    /**
     * Gets the fence gate for this wood type
     */
    public FenceGateBlock getFenceGate() {
        return this.fenceGate.get();
    }

    /**
     * Gets the door for this wood type
     */
    public DoorBlock getDoor() {
        return this.door.get();
    }

    /**
     * Gets the trapdoor for this wood type
     */
    public TrapdoorBlock getTrapdoor() {
        return this.trapdoor.get();
    }

    /* Redstone */

    /**
     * Gets the pressure plate for this wood type
     */
    public PressurePlateBlock getPressurePlate() {
        return this.pressurePlate.get();
    }

    /**
     * Gets the button for this wood type
     */
    public ButtonBlock getButton() {
        return this.button.get();
    }

    /* Signs */

    /* Gets the sign for this wood type, can also be used to get the item */
    public SignBlock getSign() {
        return this.sign.get();
    }

    /* Gets the wall sign for this wood type */
    public WallSignBlock getWallSign() {
        return this.wallSign.get();
    }

    @Override
    public List<Block> values() {
        return Arrays.asList(
                this.get(), this.getSlab(), this.getStairs(), this.getFence(),
                this.getLog(), this.getStrippedLog(), this.getWood(), this.getStrippedWood(),
                this.getFenceGate(), this.getDoor(), this.getTrapdoor(),
                this.getPressurePlate(), this.getButton(), this.getSign(), this.getWallSign());
    }

    /**
     * Variants of wood for the register function
     */
    public enum WoodVariant {LOG, WOOD, PLANKS}
}
