package brightspark.pollutantpump.tiles;

import brightspark.pollutantpump.PPConfig;
import brightspark.pollutantpump.blocks.BlockPipe;
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
	private long lastWork;

	@Override
	public void update() {
		if (checkForPipes) {
			topPipe = BlockPipe.findTop(world, pos);
			checkForPipes = false;
		}

		if (topPipe == null)
			return;

		if (world.getTotalWorldTime() - lastWork >= PPConfig.pumpWorkRate && energy.getEnergyStored() >= PPConfig.pumpEnergyUse) {
			lastWork = world.getTotalWorldTime();

			// Get all block positions in range
			List<BlockPos> positions = new ArrayList<>();
			for (int x = topPipe.getX() - PPConfig.pumpRange; x <= topPipe.getX() + PPConfig.pumpRange; x++) {
				for (int y = topPipe.getY() - PPConfig.pumpRange; y <= topPipe.getY() + PPConfig.pumpRange; y++) {
					for (int z = topPipe.getZ() - PPConfig.pumpRange; z <= topPipe.getZ() + PPConfig.pumpRange; z++) {
						positions.add(new BlockPos(x, y, z));
					}
				}
			}
			// Shuffle the positions
			Collections.shuffle(positions);

			// Go through each position until we find a pollutant
			for (BlockPos position : positions) {
				IBlockState state = world.getBlockState(position);
				Block block = state.getBlock();

				if (block instanceof Pollutant && ((IPollutant) block).getPollutantType() == IPollutant.Type.AIR) {
					Pollutant<?> pollutant = (Pollutant<?>) block;
					int pollutantAmount = pollutant.getCarriedPollutionAmount(state);

					//Find any adjacent filters that have space
					List<EnumFacing> horizontals = Lists.newArrayList(EnumFacing.HORIZONTALS);
					Collections.shuffle(horizontals);

					boolean moved = false;
					for (EnumFacing side : horizontals) {
						BlockPos sidePos = pos.offset(side);
						IBlockState sideState = world.getBlockState(sidePos);
						Block sideBlock = sideState.getBlock();

						if (sideBlock instanceof Filter) {
							Filter filter = (Filter) sideBlock;
							Filter.BlockTile filterTE = filter.getBlockTile(world, sidePos);
							if (filterTE != null) {
								int freeSpace = filter.getContent(filterTE).getFreeSpaceFor(pollutant);
								if (freeSpace >= pollutantAmount) {
									// Move the pollutant into the filter
									filter.fill(filterTE, pollutant, pollutantAmount);
									moved = true;
									break;
								}
							}
						}
					}

					if (!moved) {
						// No filters available - try move the pollutant to empty space adjacent to pump
						for (EnumFacing side : horizontals) {
							BlockPos sidePos = pos.offset(side);
							if (world.isAirBlock(sidePos)) {
								world.setBlockState(sidePos, state);
								moved = true;
								break;
							}
						}
					}

					if (moved) {
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

	@Override
	public void onLoad() {
		lastWork = world.getTotalWorldTime();
	}

	public void clearPipe() {
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
}
