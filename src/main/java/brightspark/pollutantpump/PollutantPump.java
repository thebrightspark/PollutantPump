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
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod(modid = PollutantPump.MOD_ID, name = PollutantPump.NAME, version = PollutantPump.VERSION, dependencies = PollutantPump.DEPENDENCIES)
@Mod.EventBusSubscriber
public class PollutantPump {
	public static final String MOD_ID = "pollutantpump";
	public static final String NAME = "Pollutant Pump";
	public static final String VERSION = "1.2.0";
	public static final String DEPENDENCIES = "required-after:adpother@[1.12.2-1,)";
	public static final CreativeTabs TAB = new CreativeTabs(MOD_ID) {
		@Override
		public ItemStack createIcon() {
			return new ItemStack(PPBlocks.pump);
		}
	};

	@SuppressWarnings("all")
	private static Item createItemBlock(Block block) {
		return new ItemBlock(block).setRegistryName(block.getRegistryName());
	}

	@SuppressWarnings("all")
	@SideOnly(Side.CLIENT)
	private static void regModel(Block block) {
		Item itemBlock = Item.getItemFromBlock(block);
		ModelLoader.setCustomModelResourceLocation(itemBlock, 0, new ModelResourceLocation(itemBlock.getRegistryName(), "inventory"));
	}

	@SubscribeEvent
	public static void regBlocks(RegistryEvent.Register<Block> event) {
		event.getRegistry().registerAll(
			new BlockPump(),
			new BlockPipe()
		);
	}

	@SubscribeEvent
	public static void regItems(RegistryEvent.Register<Item> event) {
		event.getRegistry().registerAll(
			createItemBlock(PPBlocks.pump),
			createItemBlock(PPBlocks.pipe)
		);
	}

	@SubscribeEvent
	public static void regModels(ModelRegistryEvent event) {
		regModel(PPBlocks.pump);
		regModel(PPBlocks.pipe);
	}

	@SubscribeEvent
	public static void configChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
		if (event.getModID().equals(MOD_ID))
			ConfigManager.sync(MOD_ID, Config.Type.INSTANCE);
	}

	@SuppressWarnings("ConstantConditions")
	@EventHandler
	public void init(FMLInitializationEvent event) {
		GameRegistry.registerTileEntity(TilePump.class, PPBlocks.pump.getRegistryName());
	}
}
