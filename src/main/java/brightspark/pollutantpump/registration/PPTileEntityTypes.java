package brightspark.pollutantpump.registration;

import brightspark.pollutantpump.tiles.TilePump;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.fml.RegistryObject;

import java.util.function.Supplier;

public class PPTileEntityTypes {
    public static final void register() {}

    public static final RegistryObject<TileEntityType<TilePump>> POLLUTION_PUMP_TILE = registerTE("pump", TilePump::new, PPBlocks.POLLUTION_PUMP);

    private static <T extends TileEntity> RegistryObject<TileEntityType<T>> registerTE(String name, Supplier<T> factory, RegistryObject<? extends Block> block) {
        return RegistrationManager.TILE_ENTITIES.register(name, () -> TileEntityType.Builder.create(factory, block.get()).build(null));
    }
}
