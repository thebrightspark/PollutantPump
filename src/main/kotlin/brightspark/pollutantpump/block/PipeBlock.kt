package brightspark.pollutantpump.block

import brightspark.pollutantpump.PPContent
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.fluid.FluidState
import net.minecraft.item.BlockItemUseContext
import net.minecraft.item.ItemStack
import net.minecraft.state.EnumProperty
import net.minecraft.state.StateContainer
import net.minecraft.stats.Stats
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.*
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.BlockRayTraceResult
import net.minecraft.util.math.shapes.ISelectionContext
import net.minecraft.util.math.shapes.VoxelShape
import net.minecraft.util.math.shapes.VoxelShapes
import net.minecraft.world.IBlockReader
import net.minecraft.world.IWorld
import net.minecraft.world.World
import net.minecraft.world.server.ServerWorld

class PipeBlock(props: Properties) : Block(props) {
	companion object {
		private val PIPE_SIZE = EnumProperty.create("size", PipeSize::class.java)
		private val BOX_FULL = VoxelShapes.box(0.25, 0.0, 0.25, 0.75, 1.0, 0.75)
		private val BOX_HALF = VoxelShapes.box(0.25, 0.0, 0.25, 0.75, 0.5, 0.75)

		fun findTopOfPipes(world: IWorld, pos: BlockPos): BlockPos {
			val checkPos = pos.mutable()
			val topPos = BlockPos.ZERO.mutable()
			for (y in (pos.y + 1) until world.height) {
				checkPos.y = y
				if (world.getBlockState(checkPos).block is PipeBlock)
					topPos.set(checkPos)
				else
					break
			}
			return topPos.immutable()
		}

		private fun findBottomOfPipes(world: IWorld, pos: BlockPos): BlockPos {
			val checkPos = pos.mutable().move(Direction.DOWN)
			while (world.getBlockState(checkPos).block is PipeBlock)
				checkPos.move(Direction.DOWN)
			return checkPos.immutable()
		}

		private fun notifyPump(world: IWorld, pos: BlockPos) {
			val bottomPos = findBottomOfPipes(world, pos)
			val block = world.getBlockState(bottomPos).block
			if (block is PumpBlock) {
				val te = world.getBlockEntity(bottomPos)
				if (te is PumpTile)
					te.updatePipe()
			}
		}
	}

	init {
		registerDefaultState(stateDefinition.any().setValue(PIPE_SIZE, PipeSize.FULL))
	}

	override fun getShape(
		state: BlockState,
		world: IBlockReader,
		pos: BlockPos,
		context: ISelectionContext
	): VoxelShape = if (state.getValue(PIPE_SIZE) == PipeSize.FULL) BOX_FULL else BOX_HALF

	override fun updateShape(
		state: BlockState,
		facing: Direction,
		facingState: BlockState,
		world: IWorld,
		pos: BlockPos,
		facingPos: BlockPos
	): BlockState {
		val down = world.getBlockState(pos.below()).block
		val up = world.getBlockState(pos.above()).block
		val isHalf = (down is PipeBlock || down is PumpBlock) && up !is PipeBlock
		return state.setValue(PIPE_SIZE, if (isHalf) PipeSize.HALF else PipeSize.FULL)
	}

	override fun getStateForPlacement(context: BlockItemUseContext): BlockState {
		val world = context.level
		val pos = context.clickedPos
		val down = world.getBlockState(pos.below()).block
		val up = world.getBlockState(pos.above()).block
		val isHalf = (down is PipeBlock || down is PumpBlock) && up !is PipeBlock
		return defaultBlockState().setValue(PIPE_SIZE, if (isHalf) PipeSize.HALF else PipeSize.FULL)
	}

	override fun createBlockStateDefinition(builder: StateContainer.Builder<Block, BlockState>) {
		builder.add(PIPE_SIZE)
	}

	override fun onPlace(state: BlockState, world: World, pos: BlockPos, oldState: BlockState, isMoving: Boolean) =
		notifyPump(world, pos)

	override fun use(
		state: BlockState,
		world: World,
		pos: BlockPos,
		player: PlayerEntity,
		hand: Hand,
		rayTraceResult: BlockRayTraceResult
	): ActionResultType {
		val heldStack = player.getItemInHand(hand)
		if (heldStack.item == PPContent.PIPE_ITEM) {
			var topPipe = findTopOfPipes(world, pos)
			if (topPipe == BlockPos.ZERO)
				topPipe = pos
			val aboveTopPipe = topPipe.above()
			if (world.isEmptyBlock(aboveTopPipe) && world.setBlock(aboveTopPipe, defaultBlockState(), 3)) {
				val soundType = getSoundType(state, world, pos, player)
				world.playSound(
					player,
					pos,
					soundType.placeSound,
					SoundCategory.BLOCKS,
					(soundType.getVolume() + 1.0F) / 2.0F,
					soundType.pitch * 0.8F
				)
				if (!player.isCreative)
					heldStack.shrink(1)
				return ActionResultType.SUCCESS
			}
		}

		return ActionResultType.PASS
	}

	override fun playerDestroy(
		world: World,
		player: PlayerEntity,
		pos: BlockPos,
		state: BlockState,
		te: TileEntity?,
		stack: ItemStack
	) {
		player.awardStat(Stats.BLOCK_MINED[this])
		player.causeFoodExhaustion(0.005F)

		if (world.isClientSide())
			return

		dropTower(player, world as ServerWorld, pos)
	}

	override fun removedByPlayer(
		state: BlockState,
		world: World,
		pos: BlockPos,
		player: PlayerEntity,
		willHarvest: Boolean,
		fluid: FluidState?
	): Boolean {
		if (player.isCreative) {
			// Destroy all pipes above without drops in creative
			val towerPos = pos.mutable()
			while (world.getBlockState(towerPos.move(Direction.UP)).block is PipeBlock)
				world.destroyBlock(towerPos, false)
		}

		return super.removedByPlayer(state, world, pos, player, willHarvest, fluid).also { notifyPump(world, pos) }
	}

	private fun dropTower(player: PlayerEntity, world: ServerWorld, pos: BlockPos) {
		// Destroy all pipes above
		val drops = mutableSetOf<ItemStack>()
		val towerPos = pos.mutable().move(Direction.UP)
		var state = world.getBlockState(towerPos)
		do {
			getDrops(state, world, towerPos, null).forEach { dropStack ->
				drops.find { ItemStack.matches(it, dropStack) }?.run {
					grow(dropStack.count)
				}
			}
			world.destroyBlock(towerPos, false)
			state = world.getBlockState(towerPos.move(Direction.UP))
		} while (state.block is PipeBlock)

		if (!player.isCreative) {
			// Drop the pipes at the same position as this block
			drops.forEach { popResource(world, pos, it) }
		}
	}

	enum class PipeSize(private val serializedName: String) : IStringSerializable {
		FULL("full"),
		HALF("half");

		override fun getSerializedName(): String = serializedName
	}
}
