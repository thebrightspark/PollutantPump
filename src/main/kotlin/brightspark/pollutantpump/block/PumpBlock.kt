package brightspark.pollutantpump.block

import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.item.BlockItemUseContext
import net.minecraft.state.BooleanProperty
import net.minecraft.state.DirectionProperty
import net.minecraft.state.StateContainer
import net.minecraft.state.properties.BlockStateProperties
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.Direction
import net.minecraft.util.Mirror
import net.minecraft.util.Rotation
import net.minecraft.world.IBlockReader

class PumpBlock(props: Properties) : Block(props) {
	companion object {
		private val FACING: DirectionProperty = BlockStateProperties.HORIZONTAL_FACING
		val POWERED: BooleanProperty = BlockStateProperties.POWERED
	}

	init {
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false))
	}

	override fun hasTileEntity(state: BlockState?): Boolean = true

	override fun createTileEntity(state: BlockState?, world: IBlockReader?): TileEntity = PumpTile()

	override fun createBlockStateDefinition(builder: StateContainer.Builder<Block, BlockState>) {
		builder.add(FACING, POWERED)
	}

	override fun getStateForPlacement(context: BlockItemUseContext): BlockState? =
		context.player?.let { defaultBlockState().setValue(FACING, it.direction.opposite) }
			?: super.getStateForPlacement(context)

	override fun rotate(state: BlockState, rotation: Rotation): BlockState =
		state.setValue(FACING, rotation.rotate(state.getValue(FACING)))

	override fun mirror(state: BlockState, mirror: Mirror): BlockState =
		state.rotate(mirror.getRotation(state.getValue(FACING)))
}
