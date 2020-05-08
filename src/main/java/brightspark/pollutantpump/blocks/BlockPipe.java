package brightspark.pollutantpump.blocks;

import brightspark.pollutantpump.tiles.TilePump;
import com.google.common.collect.Lists;
import net.minecraft.block.Block;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class BlockPipe extends BlockBase {
	public static final PropertyEnum<PipeSize> PIPE_SIZE = PropertyEnum.create("size", PipeSize.class);
	private static final AxisAlignedBB BOX_FULL = new AxisAlignedBB(0.25D, 0.0D, 0.25D, 0.75D, 1.0D, 0.75D);
	private static final AxisAlignedBB BOX_HALF = new AxisAlignedBB(0.25D, 0.0D, 0.25D, 0.75D, 0.5D, 0.75D);

	public enum PipeSize implements IStringSerializable {
		FULL("full"),
		HALF("half");

		private final String name;

		PipeSize(String name) {
			this.name = name;
		}

		@Override
		public String getName() {
			return name;
		}
	}

	public BlockPipe() {
		super("pipe");
		setDefaultState(blockState.getBaseState().withProperty(PIPE_SIZE, PipeSize.FULL));
	}

	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
		return getActualState(state, source, pos).getValue(PIPE_SIZE) == PipeSize.FULL ? BOX_FULL : BOX_HALF;
	}

	@Override
	public boolean isFullCube(IBlockState state) {
		return false;
	}

	@Override
	public boolean isOpaqueCube(IBlockState state) {
		return false;
	}

	@Override
	public IBlockState getActualState(IBlockState state, IBlockAccess worldIn, BlockPos pos) {
		Block down = worldIn.getBlockState(pos.down()).getBlock();
		Block up = worldIn.getBlockState(pos.up()).getBlock();
		boolean isHalf = (down instanceof BlockPipe || down instanceof BlockPump) && !(up instanceof BlockPipe);
		return state.withProperty(PIPE_SIZE, isHalf ? PipeSize.HALF : PipeSize.FULL);
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return 0;
	}

	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, PIPE_SIZE);
	}

	@Override
	public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
		notifyPump(worldIn, pos);
	}

	@Override
	public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		//Try to add held pipe to top of the tower
		ItemStack held = playerIn.getHeldItem(hand);
		if (held.getItem() == Item.getItemFromBlock(this)) {
			BlockPos topPipe = findTop(worldIn, pos);
			if (topPipe == null)
				topPipe = pos;
			if (worldIn.isAirBlock(topPipe.up()) && worldIn.setBlockState(topPipe.up(), getDefaultState())) {
				if (!playerIn.isCreative())
					held.shrink(1);
				return true;
			}
		}
		return false;
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
	public void harvestBlock(World worldIn, EntityPlayer player, BlockPos pos, IBlockState state, @Nullable TileEntity te, ItemStack stack) {
		player.addStat(StatList.getBlockStats(this));
		player.addExhaustion(0.005F);

		Item item = Item.getItemFromBlock(this);
		int count = 1;

		//Destroy all pipes above
		BlockPos.MutableBlockPos towerPos = new BlockPos.MutableBlockPos(pos);
		while (worldIn.getBlockState(towerPos.move(EnumFacing.UP)).getBlock() instanceof BlockPipe) {
			worldIn.destroyBlock(towerPos, false);
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

			ForgeEventFactory.fireBlockHarvesting(drops, worldIn, pos, state, 0, 1F, false, player);
			drops.forEach(drop -> spawnAsEntity(worldIn, pos, drop));
		}

		notifyPump(worldIn, pos);
	}

	private void notifyPump(World world, BlockPos pos) {
		for (int y = pos.getY(); y > 0; y--) {
			BlockPos checkPos = new BlockPos(pos.getX(), y, pos.getZ());
			Block block = world.getBlockState(checkPos).getBlock();
			if (block instanceof BlockPump) {
				TileEntity te = world.getTileEntity(checkPos);
				if (te instanceof TilePump)
					((TilePump) te).clearPipe();
				return;
			} else if (!(block instanceof BlockPipe))
				return;
		}
	}
}
