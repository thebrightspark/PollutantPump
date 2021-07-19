package brightspark.pollutantpump.tiles;

import brightspark.pollutantpump.PPConfig;
import brightspark.pollutantpump.registration.PPTileEntityTypes;
import brightspark.pollutantpump.blocks.BlockPipe;
import brightspark.pollutantpump.blocks.BlockPump;
import com.endertech.minecraft.forge.blocks.IPollutant;
import com.endertech.minecraft.mods.adpother.blocks.FilterFrame;
import com.endertech.minecraft.mods.adpother.blocks.Pollutant;
import com.google.common.collect.Lists;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TilePump extends TileEntity implements ITickableTileEntity {
	LazyOptional<IEnergyStorage> energy = LazyOptional.of(() -> new EnergyStorage(PPConfig.pumpMaxEnergyStorage.get()));

	private boolean checkForPipes = true;
	private BlockPos topPipe;
	private Boolean wasPoweredLastTick = null;
	private long lastWork;

	public TilePump() {
		super(PPTileEntityTypes.POLLUTION_PUMP_TILE.get());
	}

	@Override
	public void tick() {
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

		boolean isPowered = energy.resolve().get().getEnergyStored() >= PPConfig.pumpEnergyUse.get();

		// Update block state if necessary
		if (wasPoweredLastTick == null || isPowered != wasPoweredLastTick) {
			wasPoweredLastTick = isPowered;
			updateState(isPowered);
		}

		// Do work
		if (world.getGameTime() - lastWork >= PPConfig.pumpWorkRate.get() && isPowered) {
			lastWork = world.getGameTime();

			List<BlockPos> positions = getAllPositionsInRange();

			// Go through each position until we find a pollutant
			for (BlockPos position : positions) {
				BlockState state = world.getBlockState(position);
				Block block = state.getBlock();

				if (block instanceof Pollutant && ((IPollutant) block).getPollutantType() == IPollutant.Type.AIR) {
					Pollutant<?> pollutant = (Pollutant<?>) block;
					int pollutantAmount = pollutant.getCarriedPollutionAmount(state);

					if (tryPumpPollutant(state, pollutant, pollutantAmount)) {
						// If successfully moved pollutant, then set its original position to air
						world.setBlockState(position, Blocks.AIR.getDefaultState());
						break;
					}
				}
			}

			// Use energy
			energy.resolve().get().extractEnergy(PPConfig.pumpEnergyUse.get(), false);
		}
	}

	private void updateState(boolean isPowered) {
		world.setBlockState(pos, world.getBlockState(pos).with(BlockPump.POWERED, isPowered));
	}

	private List<BlockPos> getAllPositionsInRange() {
		List<BlockPos> positions = new ArrayList<>();
		for (int x = topPipe.getX() - PPConfig.pumpRange.get(); x <= topPipe.getX() + PPConfig.pumpRange.get(); x++) {
			for (int y = topPipe.getY() - PPConfig.pumpRange.get(); y <= topPipe.getY() + PPConfig.pumpRange.get(); y++) {
				for (int z = topPipe.getZ() - PPConfig.pumpRange.get(); z <= topPipe.getZ() + PPConfig.pumpRange.get(); z++) {
					positions.add(new BlockPos(x, y, z));
				}
			}
		}
		Collections.shuffle(positions);
		return positions;
	}

	private boolean tryPumpPollutant(BlockState pollutantState, Pollutant<?> pollutantBlock, int pollutantAmount) {
		List<Direction> horizontals = Lists.newArrayList(Direction.Plane.HORIZONTAL);
		Collections.shuffle(horizontals);

		//Find any adjacent filters that have space
		for (Direction side : horizontals) {
			BlockPos sidePos = pos.offset(side);
			BlockState sideState = world.getBlockState(sidePos);
			Block sideBlock = sideState.getBlock();

			if (sideBlock instanceof FilterFrame) {
				FilterFrame filter = (FilterFrame) sideBlock;
				FilterFrame.BlockTile filterTE = filter.getTile(world, sidePos).orElse(null);
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
		for (Direction side : horizontals) {
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
		lastWork = world.getGameTime();
	}

	public void updatePipe() {
		checkForPipes = true;
		topPipe = null;
	}


	@Nullable
	@Override
	public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction facing) {
		if (capability == CapabilityEnergy.ENERGY)
			return energy.cast();

		return super.getCapability(capability, facing);
	}

	@Override
	public void invalidateCaps() {
		super.invalidateCaps();
		energy.invalidate();

	}
}
