package brightspark.pollutantpump;

import net.minecraft.block.Block;
import net.minecraftforge.fml.common.registry.GameRegistry.ObjectHolder;

import javax.annotation.Nonnull;

@ObjectHolder(PollutantPump.MOD_ID)
public class PPBlocks {
	public static final Block pump = getNull();
	public static final Block pipe = getNull();

	@SuppressWarnings("ConstantConditions")
	@Nonnull
	private static Block getNull() {
		return null;
	}
}
