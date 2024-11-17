package slimeknights.tconstruct.library.fluid;

import lombok.Getter;
import lombok.Setter;
import slimeknights.mantle.block.entity.MantleBlockEntity;

@Getter
public class FluidTankAnimated extends FluidTankBase<MantleBlockEntity> {
    @Setter
    private float renderOffset;

    public FluidTankAnimated(int capacity, MantleBlockEntity parent) {
        super(capacity, parent);
    }
}
