package brightspark.pollutantpump.tiles;

import brightspark.pollutantpump.PPConfig;
import brightspark.pollutantpump.blocks.BlockPipe;
import brightspark.pollutantpump.blocks.BlockPump;
import com.endertech.minecraft.forge.api.IPollutant;
import com.endertech.minecraft.mods.adpother.blocks.Filter;
import com.endertech.minecraft.mods.adpother.blocks.Pollutant;
import com.google.common.collect.Lists;
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
import java.util.Collections;
import java.util.List;

public class TilePump extends TileEntity implements ITickable {
	private final EnergyStorage energy = new EnergyStorage(PPConfig.pumpMaxEnergyStorage, Integer.MAX_VALUE);
	private boolean checkForPipes = true;
	private BlockPos topPipe;
	private Boolean wasPoweredLastTick = null;
	private long lastWork;

	@Override
	public void update() {
		if (world.isRemote)
			return;

		if (checkForPipes) {
			topPipe = BlockPipe.findTop(world, pos);
			checkForPipes = false;
		}

		if (topPipe == null) {
			wasPoweredLastTick = null;
			updateState(false);
			return;
		}

		boolean isPowered = energy.getEnergyStored() >= PPConfig.pumpEnergyUse;

		// Update block state if necessary
		if (wasPoweredLastTick == null || isPowered != wasPoweredLastTick) {
			wasPoweredLastTick = isPowered;
			updateState(isPowered);
		}

		// Do work
		if (world.getTotalWorldTime() - lastWork >= PPConfig.pumpWorkRate && isPowered) {
			lastWork = world.getTotalWorldTime();

			List<BlockPos> positions = getAllPositionsInRange();

			// Go through each position until we find a pollutant
			for (BlockPos position : positions) {
				IBlockState state = world.getBlockState(position);
				Block block = state.getBlock();

				if (block instanceof Pollutant && ((IPollutant) block).getPollutantType() == IPollutant.Type.AIR) {
					Pollutant<?> pollutant = (Pollutant<?>) block;
					int pollutantAmount = pollutant.getCarriedPollutionAmount(state);

					if (tryPumpPollutant(state, pollutant, pollutantAmount)) {
						// If successfully moved pollutant, then set its original position to air
						world.setBlockToAir(position);
						break;
					}
				}
			}

			// Use energy
			energy.extractEnergy(PPConfig.pumpEnergyUse, false);
		}
	}

	private void updateState(boolean isPowered) {
		world.setBlockState(pos, world.getBlockState(pos).withProperty(BlockPump.POWERED, isPowered));
	}

	private List<BlockPos> getAllPositionsInRange() {
		List<BlockPos> positions = new ArrayList<>();
		for (int x = topPipe.getX() - PPConfig.pumpRange; x <= topPipe.getX() + PPConfig.pumpRange; x++) {
			for (int y = topPipe.getY() - PPConfig.pumpRange; y <= topPipe.getY() + PPConfig.pumpRange; y++) {
				for (int z = topPipe.getZ() - PPConfig.pumpRange; z <= topPipe.getZ() + PPConfig.pumpRange; z++) {
					positions.add(new BlockPos(x, y, z));
				}
			}
		}
		Collections.shuffle(positions);
		return positions;
	}

	private boolean tryPumpPollutant(IBlockState pollutantState, Pollutant<?> pollutantBlock, int pollutantAmount) {
		List<EnumFacing> horizontals = Lists.newArrayList(EnumFacing.HORIZONTALS);
		Collections.shuffle(horizontals);

		//Find any adjacent filters that have space
		for (EnumFacing side : horizontals) {
			BlockPos sidePos = pos.offset(side);
			IBlockState sideState = world.getBlockState(sidePos);
			Block sideBlock = sideState.getBlock();

			if (sideBlock instanceof Filter) {
				Filter filter = (Filter) sideBlock;
				Filter.BlockTile filterTE = filter.getBlockTile(world, sidePos);
				if (filterTE != null) {
					int freeSpace = filter.getContent(filterTE).getFreeSpaceFor(pollutantBlock);
					if (freeSpace >= pollutantAmount) {
						// Move the pollutant into the filter
						filter.fill(filterTE, pollutantBlock, pollutantAmount);
						return true;
					}
				}
			}
		}

		// No filters available - try move the pollutant to empty space adjacent to pump
		for (EnumFacing side : horizontals) {
			BlockPos sidePos = pos.offset(side);
			if (world.isAirBlock(sidePos)) {
				world.setBlockState(sidePos, pollutantState);
				return true;
			}
		}

		return false;
	}

	@Override
	public void onLoad() {
		lastWork = world.getTotalWorldTime();
	}

	public void updatePipe() {
		checkForPipes = true;
		topPipe = null;
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

	@Override
	public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newSate) {
		return oldState.getBlock() != newSate.getBlock();
	}
}
