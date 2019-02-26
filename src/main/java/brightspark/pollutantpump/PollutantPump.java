package brightspark.pollutantpump;

import brightspark.pollutantpump.blocks.BlockPipe;
import brightspark.pollutantpump.blocks.BlockPump;
import brightspark.pollutantpump.tiles.TilePump;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.Logger;

@Mod(modid = PollutantPump.MOD_ID, name = PollutantPump.NAME, version = PollutantPump.VERSION, dependencies = PollutantPump.DEPENDENCIES)
@Mod.EventBusSubscriber
public class PollutantPump
{
    public static final String MOD_ID = "pollutantpump";
    public static final String NAME = "Pollutant Pump";
    public static final String VERSION = "@VERSION@";
    public static final String DEPENDENCIES = "requires-after:adpother@[1.12.2-1,)";
    public static final CreativeTabs TAB = new CreativeTabs(MOD_ID)
    {
        @Override
        public ItemStack createIcon()
        {
            return new ItemStack(blockPipe);
        }
    };

    public static Logger logger;

    public static Block blockPump, blockPipe;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();
    }

    @SubscribeEvent
    public static void regBlocks(RegistryEvent.Register<Block> event)
    {
        event.getRegistry().registerAll(
                blockPump = new BlockPump(),
                blockPipe = new BlockPipe()
        );
        regTE(TilePump.class, blockPump);
    }

    @SubscribeEvent
    public static void regItems(RegistryEvent.Register<Item> event)
    {
        event.getRegistry().registerAll(
                createItemBlock(blockPump),
                createItemBlock(blockPipe)
        );
    }

    @SubscribeEvent
    public static void regModels(ModelRegistryEvent event)
    {
        regModel(blockPump);
        regModel(blockPipe);
    }

    @SuppressWarnings("all")
    private static Item createItemBlock(Block block)
    {
        return new ItemBlock(block).setRegistryName(block.getRegistryName());
    }

    @SuppressWarnings("all")
    private static void regTE(Class<? extends TileEntity> te, Block block)
    {
        GameRegistry.registerTileEntity(te, block.getRegistryName());
    }

    @SuppressWarnings("all")
    @SideOnly(Side.CLIENT)
    private static void regModel(Block block)
    {
        Item itemBlock = Item.getItemFromBlock(block);
        ModelLoader.setCustomModelResourceLocation(itemBlock, 0, new ModelResourceLocation(itemBlock.getRegistryName(), "inventory"));
    }
}
