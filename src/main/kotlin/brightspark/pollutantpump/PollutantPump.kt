package brightspark.pollutantpump

import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemStack
import net.minecraftforge.client.event.ModelRegistryEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.config.ModConfig
import net.minecraftforge.fml.config.ModConfig.ModConfigEvent
import thedarkcolour.kotlinforforge.forge.MOD_BUS
import thedarkcolour.kotlinforforge.forge.registerConfig

@Mod(PollutantPump.MOD_ID)
object PollutantPump {
	const val MOD_ID = "pollutantpump"

	val GROUP = object : ItemGroup(MOD_ID) {
		override fun makeIcon(): ItemStack = ItemStack(PPContent.PUMP)
	}

	init {
		MOD_BUS.apply {
			PPContent.BLOCK_REGISTRY.register(this)
			PPContent.TILE_REGISTRY.register(this)
			PPContent.ITEM_REGISTRY.register(this)

			addListener<ModConfigEvent> { if (it.config.modId == MOD_ID) PPConfig.bake() }
			addListener<ModelRegistryEvent> {

			}
		}

		registerConfig(ModConfig.Type.SERVER, PPConfig.SERVER_SPEC)
	}
}
