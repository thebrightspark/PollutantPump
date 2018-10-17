package brightspark.pollutantpump.blocks;

import brightspark.pollutantpump.PollutantPump;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;

public class BlockBase extends Block
{
    public BlockBase(String name)
    {
        super(Material.ROCK);
        setRegistryName(name);
        setTranslationKey(name);
        setCreativeTab(PollutantPump.TAB);
    }
}
