package brightspark.pollutantpump.blocks;

import brightspark.pollutantpump.PollutantPump;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraftforge.common.ToolType;

public class BlockBase extends Block {
	public BlockBase() {
		super(Properties.create(Material.ROCK)
				.hardnessAndResistance(2f, 15f)
				.harvestLevel(0)
				.harvestTool(ToolType.PICKAXE)
				.sound(SoundType.METAL)
		);
	}
}
