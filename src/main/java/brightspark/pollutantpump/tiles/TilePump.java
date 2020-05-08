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

public class TilePump extends TileEntity implements ITickable {
	private boolean checkForPipes = true;
	private BlockPos topPipe;
	private EnergyStorage energy = new EnergyStorage(PPConfig.pumpMaxEnergyStorage, Integer.MAX_VALUE);
	private long lastWork;

	@Override
	public void update() {
		if (checkForPipes) {
			topPipe = BlockPipe.findTop(world, pos);
			checkForPipes = false;
		}

		if (topPipe == null)
			return;

		if (world.getTotalWorldTime() - lastWork >= PPConfig.pumpWorkRate
			&& energy.getEnergyStored() >= PPConfig.pumpEnergyUse) {
			lastWork = world.getTotalWorldTime();

			//Find pollutant blocks
			List<BlockPos> pollutantBlocks = findPollutants();
			if (pollutantBlocks.size() <= 0)
				return;

			BlockPos pollutantPos = pollutantBlocks.get(world.rand.nextInt(pollutantBlocks.size()));
			IBlockState pollutantState = world.getBlockState(pollutantPos);
			Pollutant<?> pollutant = (Pollutant<?>) pollutantState.getBlock();
			int pollutantAmount = pollutant.getCarriedPollutionAmount(pollutantState);

			//Find any adjacent filters that have space
			List<BlockPos> filters = ForgeWorld.Position.getAroundCube(world, pos, (w, p) ->
			{
				IBlockState state = w.getBlockState(p);
				Block block = state.getBlock();
				if (block instanceof Filter) {
					Filter filter = (Filter) block;
					Filter.BlockTile tile = filter.getBlockTile(w, p);
					if (tile != null) {
						int freeSpace = filter.getContent(tile).getFreeSpaceFor(pollutant);
						return freeSpace >= pollutantAmount;
					}
				}
				return false;
			});

			boolean pollutantMoved = false;

			int numFilters = filters.size();
			if (numFilters > 0) {
				//Put the pollutant into a random filter
				BlockPos filterPos = filters.get(world.rand.nextInt(numFilters));
				Filter filter = (Filter) ForgeWorld.getBlock(world, filterPos);
				Filter.BlockTile tile = filter.getBlockTile(world, filterPos);
				filter.fill(tile, pollutant, pollutantAmount);
				pollutantMoved = true;
			} else {
				//Place the pollutant at a random empty pos
				List<BlockPos> emptyPositions = ForgeWorld.Position.getAroundCube(world, pos, World::isAirBlock);
				if (!emptyPositions.isEmpty()) {
					BlockPos emptyPos = emptyPositions.get(world.rand.nextInt(emptyPositions.size()));
					world.setBlockState(emptyPos, pollutantState);
					pollutantMoved = true;
				}
			}

			if (pollutantMoved) {
				world.setBlockToAir(pollutantPos);
				energy.extractEnergy(PPConfig.pumpEnergyUse, false);
			}
		}
	}

	@Override
	public void onLoad() {
		lastWork = world.getTotalWorldTime();
	}

	public void clearPipe() {
		checkForPipes = true;
		topPipe = null;
	}

	private List<BlockPos> findPollutants() {
		List<BlockPos> pollutants = new ArrayList<>();
		for (int x = topPipe.getX() - PPConfig.pumpRange; x < topPipe.getX() + PPConfig.pumpRange; x++) {
			for (int y = topPipe.getY() - PPConfig.pumpRange; y < topPipe.getY() + PPConfig.pumpRange; y++) {
				for (int z = topPipe.getZ() - PPConfig.pumpRange; z < topPipe.getZ() + PPConfig.pumpRange; z++) {
					BlockPos p = new BlockPos(x, y, z);
					Block block = world.getBlockState(p).getBlock();
					if (block instanceof Pollutant && ((IPollutant) block).getPollutantType() == IPollutant.Type.AIR)
						pollutants.add(p);
				}
			}
		}
		return pollutants;
	}

	@SuppressWarnings("NullableProblems")
	@Override
	public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
		return capability == CapabilityEnergy.ENERGY || super.hasCapability(capability, facing);
	}

	@SuppressWarnings("all")
	@Nullable
	@Override
	public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
		if (capability == CapabilityEnergy.ENERGY)
			return (T) energy;
		return super.getCapability(capability, facing);
	}
}
