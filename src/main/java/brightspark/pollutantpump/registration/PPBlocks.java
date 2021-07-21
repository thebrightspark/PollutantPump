package brightspark.pollutantpump.registration;

import brightspark.pollutantpump.blocks.BlockPipe;
import brightspark.pollutantpump.blocks.BlockPump;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraftforge.fml.RegistryObject;

import java.util.function.Supplier;

public class PPBlocks {
	public static final void register() {}

	public static final RegistryObject<Block> POLLUTION_PIPE = register(
			"pipe",
			BlockPipe::new
	);
	public static final RegistryObject<Block> POLLUTION_PUMP= register(
			"pump",
			BlockPump::new
	);

	private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
		return RegistrationManager.BLOCKS.register(name, block);
	}

	private static <T extends Block> RegistryObject<T> register(String name, Supplier<T> block) {
		RegistryObject<T> ret = registerBlock(name, block);
		RegistrationManager.ITEMS.register(name, () -> new BlockItem(ret.get(), new Item.Properties().group(PPTab.INSTANCE)));
		return  ret;
	}
}
