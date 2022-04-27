package brightspark.pollutantpump.block

import brightspark.pollutantpump.PPConfig
import brightspark.pollutantpump.PPContent
import com.endertech.minecraft.forge.blocks.IPollutant
import com.endertech.minecraft.mods.adpother.blocks.FilterFrame
import com.endertech.minecraft.mods.adpother.blocks.Pollutant
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.tileentity.ITickableTileEntity
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.Direction
import net.minecraft.util.math.BlockPos
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.energy.CapabilityEnergy
import net.minecraftforge.energy.EnergyStorage

class PumpTile : TileEntity(PPContent.PUMP_TILE), ITickableTileEntity {
	companion object {
		private val HORIZONTALS = setOf(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST)
	}

	private val energy: EnergyStorage = EnergyStorage(PPConfig.pumpMaxEnergyStorage, Int.MAX_VALUE)
	private var checkForPipes: Boolean = true
	private var topPipe: BlockPos? = null
	private var wasPoweredLastTick: Boolean? = null
	private var lastWork: Long = -1L

	override fun tick() {
		if (level?.isClientSide() != false)
			return

		if (checkForPipes) {
			topPipe = PipeBlock.findTopOfPipes(level!!, blockPos)
			checkForPipes = false
		}

		if (topPipe == null) {
			wasPoweredLastTick = null
			updateState(false)
			return
		}

		val isPowered = energy.energyStored >= PPConfig.pumpEnergyUse

		// Update block state if necessary
		if (wasPoweredLastTick == null || isPowered != wasPoweredLastTick) {
			wasPoweredLastTick = isPowered
			updateState(isPowered)
		}

		// Do work
		if (level!!.gameTime - lastWork >= PPConfig.pumpWorkRate && isPowered) {
			lastWork = level!!.gameTime

			// Go through each position until we find a pollutant
			for (it in getAllPositionsInRange()) {
				val state = level!!.getBlockState(it)
				val block = state.block

				if (block is Pollutant<*> && (block as IPollutant).pollutantType == IPollutant.Type.AIR) {
					val pollutantAmount = block.getCarriedPollutionAmount(state)
					if (tryPumpPollutant(state, block, pollutantAmount)) {
						// If successfully moved pollutant, then set its original position to air
						level!!.setBlock(it, Blocks.AIR.defaultBlockState(), 2)
						break
					}
				}
			}
		}

		// Use energy
		energy.extractEnergy(PPConfig.pumpEnergyUse, false)
	}

	private fun updateState(isPowered: Boolean) {
		level!!.setBlock(blockPos, level!!.getBlockState(blockPos).setValue(PumpBlock.POWERED, isPowered), 2)
	}

	fun updatePipe() {
		checkForPipes = true
		topPipe = null
	}

	private fun getAllPositionsInRange(): List<BlockPos> = topPipe?.let { top ->
		buildList {
			((top.x - PPConfig.pumpRangeWidth)..(top.x + PPConfig.pumpRangeWidth)).forEach { x ->
				((top.y - PPConfig.pumpRangeHeight)..(top.y + PPConfig.pumpRangeHeight)).forEach { y ->
					((top.z - PPConfig.pumpRangeWidth)..(top.z + PPConfig.pumpRangeWidth)).forEach { z ->
						add(BlockPos(x, y, z))
					}
				}
			}
			shuffle()
		}
	} ?: emptyList()

	private fun tryPumpPollutant(
		pollutantState: BlockState,
		pollutantBlock: Pollutant<*>,
		pollutantAmount: Int
	): Boolean {
		val horizontals = HORIZONTALS.shuffled()

		// Find any adjacent filters that have space
		horizontals.forEach { side ->
			val sidePos = blockPos.relative(side)
			val sideState = level!!.getBlockState(sidePos)
			val sideBlock = sideState.block
			if (sideBlock is FilterFrame) {
				val filter: FilterFrame = sideBlock
				val movedToFilter = filter.getTile(level, blockPos)
					.map {
						val freeSpace = filter.getContent(it).getFreeSpaceFor(pollutantBlock)
						if (freeSpace > pollutantAmount) {
							// Move the pollutant into the filter
							filter.fill(it, pollutantBlock, pollutantAmount)
							return@map true
						}
						return@map false
					}
					.orElse(false)
				if (movedToFilter)
					return true
			}
		}

		// No filters available - try move pollutant to empty space adjacent to pump
		horizontals.forEach { side ->
			val sidePos = blockPos.relative(side)
			if (level!!.isEmptyBlock(sidePos)) {
				level!!.setBlock(sidePos, pollutantState, 2)
				return true
			}
		}

		return false
	}

	override fun onLoad() {
		lastWork = level!!.gameTime
	}

	override fun <T : Any?> getCapability(cap: Capability<T>, side: Direction?): LazyOptional<T> =
		if (cap == CapabilityEnergy.ENERGY) LazyOptional.of { energy }.cast() else super.getCapability(cap, side)
}
