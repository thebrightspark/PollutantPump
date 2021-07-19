package brightspark.pollutantpump.blocks;

import brightspark.pollutantpump.tiles.TilePump;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.Property;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class BlockPipe extends BlockBase {
	public static final Property<PipeSize> PIPE_SIZE = EnumProperty.create("size", PipeSize.class);
	private static final VoxelShape BOX_FULL = Block.makeCuboidShape(4, 0, 4, 12, 16, 12);
	private static final VoxelShape BOX_HALF = Block.makeCuboidShape(4, 0, 4, 12, 8, 12);

	public enum PipeSize implements IStringSerializable {
		FULL("full"),
		HALF("half");

		private final String name;

		PipeSize(String name) {
			this.name = name;
		}

		@Override
		public String getString() {
			return this.name;
		}
	}

	public BlockPipe() {
		super();
		setDefaultState(this.getDefaultState().with(PIPE_SIZE, PipeSize.FULL));
	}

	@Override
	protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
		builder.add(PIPE_SIZE);
	}

	@Override
	public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
		switch((PipeSize)state.get(PIPE_SIZE)) {
			case HALF:
				return BOX_HALF;
			case FULL:
			default:
				return BOX_FULL;
		}
	}

	@Override
	public VoxelShape getRaytraceShape(BlockState state, IBlockReader worldIn, BlockPos pos) {
		switch((PipeSize)state.get(PIPE_SIZE)) {
			case HALF:
				return BOX_HALF;
			case FULL:
			default:
				return BOX_FULL;
		}
	}


	@Override
	public void onBlockAdded(BlockState state, World worldIn, BlockPos pos, BlockState oldState, boolean isMoving) {
		this.notifyOnBlockAdded(worldIn, pos);
	}

	public void notifyOnBlockAdded(World worldIn, BlockPos pos) {
		notifyPump(worldIn, pos);
	}

	@Override
	public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockRayTraceResult hit) {
		//Try to add held pipe to top of the tower
		ItemStack held = player.getHeldItem(hand);
		if (held.getItem() == Item.getItemFromBlock(this)) {
			BlockPos topPipe = findTop(world, pos);
			if (topPipe == null)
				topPipe = pos;
			if (world.isAirBlock(topPipe.up()) && world.setBlockState(topPipe.up(), getDefaultState())) {
				SoundType soundType = getSoundType(state, world, pos, player);
				world.playSound(player, pos, soundType.getPlaceSound(), SoundCategory.BLOCKS, (soundType.getVolume() + 1.0F) / 2.0F, soundType.getPitch() * 0.8F);
				if (!player.isCreative())
					held.shrink(1);
				return ActionResultType.SUCCESS;
			}
		}
		return super.onBlockActivated(state, world, pos, player, hand, hit);
	}

	public static BlockPos findTop(World world, BlockPos pos) {
		BlockPos top = null;
		for (int y = pos.getY() + 1; y < world.getHeight(); y++) {
			BlockPos checkPos = new BlockPos(pos.getX(), y, pos.getZ());
			Block block = world.getBlockState(checkPos).getBlock();
			if (block instanceof BlockPipe)
				top = new BlockPos(checkPos);
			else
				break;
		}
		return top;
	}

	@Override
	public void harvestBlock(World world, PlayerEntity player, BlockPos pos, BlockState state, @Nullable TileEntity te, ItemStack stack) {
		player.addExhaustion(0.005F);

		Item item = Item.getItemFromBlock(this);
		int count = 1;

		//Destroy all pipes above
		BlockPos.Mutable towerPos = new BlockPos.Mutable().setPos(pos);
		while (world.getBlockState(towerPos.move(Direction.UP)).getBlock() instanceof BlockPipe) {
			world.destroyBlock(towerPos, false);
			count++;
		}

		if (!player.isCreative()) {
			//Drop the pipes at the same position as this block
			List<ItemStack> drops = new ArrayList<>();
			while (count > 0) {
				int dropCount = Math.min(count, 64);
				count -= dropCount;
				drops.add(new ItemStack(item, dropCount));
			}

			//ForgeEventFactory.fireBlockHarvesting(drops, world, pos, state, 0, 1F, false, player);
			drops.forEach(drop -> spawnAsEntity(world, pos, drop));
		}
	}

	@Override
	public boolean removedByPlayer(BlockState state, World world, BlockPos pos, PlayerEntity player, boolean willHarvest, FluidState fluid) {
		if (player.isCreative()) {
			//Destroy all pipes above without drops in creative
			BlockPos.Mutable towerPos = new BlockPos.Mutable().setPos(pos);
			while (world.getBlockState(towerPos.move(Direction.UP)).getBlock() instanceof BlockPipe) {
				world.destroyBlock(towerPos, false);
			}
		}

		boolean result = super.removedByPlayer(state, world, pos, player, willHarvest, fluid);

		// Notify pump that pipes have been destroyed
		notifyPump(world, pos);

		return result;
	}

	private BlockPos findBottomOfPipes(World world, BlockPos pos) {
		BlockPos.Mutable checkPos = new BlockPos.Mutable().setPos(pos).move(Direction.DOWN);
		while (world.getBlockState(checkPos).getBlock() instanceof BlockPipe)
			checkPos.move(Direction.DOWN);
		return checkPos.toImmutable();
	}

	private void notifyPump(World world, BlockPos pos) {
		BlockPos bottomPos = findBottomOfPipes(world, pos);
		Block block = world.getBlockState(bottomPos).getBlock();
		if (block instanceof BlockPump) {
			TileEntity te = world.getTileEntity(bottomPos);
			if (te instanceof TilePump)
				((TilePump) te).updatePipe();
		}
	}
}
