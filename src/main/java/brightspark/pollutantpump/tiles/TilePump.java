package brightspark.pollutantpump.tiles;

import brightspark.pollutantpump.PPConfig;
import brightspark.pollutantpump.blocks.BlockPipe;
import com.endertech.minecraft.forge.api.IPollutant;
import com.endertech.minecraft.forge.world.ForgeWorld;
import com.endertech.minecraft.mods.adpother.blocks.Filter;
import com.endertech.minecraft.mods.adpother.blocks.Pollutant;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TilePump extends TileEntity implements ITickable
{
    private boolean checkForPipes = true;
    private BlockPos topPipe;
    private EnergyStorage energy = new EnergyStorage(PPConfig.pumpMaxEnergyStorage, Integer.MAX_VALUE);
    private long lastWork;

    @Override
    public void update()
    {
        if(checkForPipes)
        {
            topPipe = BlockPipe.findTop(world, pos);
            checkForPipes = false;
        }

        if(topPipe == null)
            return;

        if(world.getTotalWorldTime() - lastWork >= PPConfig.pumpWorkRate &&
                energy.getEnergyStored() >= PPConfig.pumpEnergyUse)
        {
            lastWork = world.getTotalWorldTime();

            //Find pollutant blocks
            List<BlockPos> pollutantBlocks = findPollutants();
            if(pollutantBlocks.size() <= 0)
                return;

            BlockPos pollutantPos = pollutantBlocks.get(world.rand.nextInt(pollutantBlocks.size()));
            IBlockState pollutantState = world.getBlockState(pollutantPos);
            Pollutant pollutantBlock = (Pollutant) pollutantState.getBlock();

            //Find any adjacent filters that have space
            List<BlockPos> filters = ForgeWorld.Position.getAroundCube(world, pos, (w, p) -> {
                IBlockState state = w.getBlockState(p);
                Block block = state.getBlock();
                TileEntity te = w.getTileEntity(p);
                if(block instanceof Filter && te instanceof Filter.BlockTile)
                {
                    Filter filterBlock = (Filter) block;
                    int capacity = filterBlock.getCapacity();
                    int amount = filterBlock.getFullnessWith(pollutantBlock, (Filter.BlockTile) te);
                    int pollutantAmount = pollutantBlock.getCarriedPollutionAmount(pollutantState);
                    return amount + pollutantAmount <= capacity;
                }
                return false;
            });

            boolean pollutantMoved = false;

            int numFilters = filters.size();
            if(numFilters > 0)
            {
                //Put the pollutant into a random filter
                BlockPos filterPos = filters.get(world.rand.nextInt(numFilters));
                IBlockState filterState = world.getBlockState(filterPos);
                Filter filterBlock = (Filter) filterState.getBlock();
                Filter.BlockTile filterTE = (Filter.BlockTile) Objects.requireNonNull(world.getTileEntity(filterPos));
                int amount = filterBlock.getFullnessWith(pollutantBlock, filterTE);
                int pollutantAmount = pollutantBlock.getCarriedPollutionAmount(pollutantState);
                filterBlock.setFullnessWith(pollutantBlock, amount + pollutantAmount, filterTE);
                pollutantMoved = true;
            }
            else
            {
                //Place the pollutant at a random empty pos
                List<BlockPos> emptyPositions = ForgeWorld.Position.getAroundCube(world, pos, World::isAirBlock);
                if(!emptyPositions.isEmpty())
                {
                    //Find a pollutant block and suck it up
                    BlockPos emptyPos = emptyPositions.get(world.rand.nextInt(emptyPositions.size()));
                    world.setBlockState(emptyPos, pollutantState);
                    pollutantMoved = true;
                }
            }

            if(pollutantMoved)
            {
                world.setBlockToAir(pollutantPos);
                energy.extractEnergy(PPConfig.pumpEnergyUse, false);
            }
        }
    }

    @Override
    public void onLoad()
    {
        lastWork = world.getTotalWorldTime();
    }

    public void clearPipe()
    {
        checkForPipes = true;
        topPipe = null;
    }

    private List<BlockPos> findPollutants()
    {
        List<BlockPos> pollutants = new ArrayList<>();
        for(int x = topPipe.getX() - PPConfig.pumpRange; x < topPipe.getX() + PPConfig.pumpRange; x++)
        {
            for(int y = topPipe.getY() - PPConfig.pumpRange; y < topPipe.getY() + PPConfig.pumpRange; y++)
            {
                for(int z = topPipe.getZ() - PPConfig.pumpRange; z < topPipe.getZ() + PPConfig.pumpRange; z++)
                {
                    BlockPos p = new BlockPos(x, y, z);
                    Block block = world.getBlockState(p).getBlock();
                    if(block instanceof Pollutant && ((IPollutant) block).getPollutantType() == IPollutant.Type.AIR)
                        pollutants.add(p);
                }
            }
        }
        return pollutants;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing)
    {
        return capability == CapabilityEnergy.ENERGY || super.hasCapability(capability, facing);
    }

    @SuppressWarnings("all")
    @Nullable
    @Override
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing)
    {
        if(capability == CapabilityEnergy.ENERGY)
            return (T) energy;
        return super.getCapability(capability, facing);
    }
}
