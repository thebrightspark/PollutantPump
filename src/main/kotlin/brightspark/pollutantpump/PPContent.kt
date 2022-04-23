package brightspark.pollutantpump

import brightspark.pollutantpump.block.PipeBlock
import brightspark.pollutantpump.block.PumpBlock
import brightspark.pollutantpump.block.PumpTile
import net.minecraft.block.AbstractBlock
import net.minecraft.block.material.Material
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.tileentity.TileEntityType
import net.minecraftforge.registries.ForgeRegistries
import thedarkcolour.kotlinforforge.forge.KDeferredRegister

object PPContent {
	val BLOCK_REGISTRY = KDeferredRegister(ForgeRegistries.BLOCKS, PollutantPump.MOD_ID)
	val TILE_REGISTRY = KDeferredRegister(ForgeRegistries.TILE_ENTITIES, PollutantPump.MOD_ID)
	val ITEM_REGISTRY = KDeferredRegister(ForgeRegistries.ITEMS, PollutantPump.MOD_ID)

	val PUMP by BLOCK_REGISTRY.registerObject("pump") { PumpBlock(blockProps()) }
	private val PIPE by BLOCK_REGISTRY.registerObject("pipe") { PipeBlock(blockProps()) }

	val PUMP_TILE: TileEntityType<PumpTile> by TILE_REGISTRY.registerObject("pump") {
		@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
		TileEntityType.Builder.of(::PumpTile, PUMP).build(null)
	}

	val PIPE_ITEM by ITEM_REGISTRY.registerObject("pipe") { BlockItem(PIPE, itemProps()) }

	init {
		ITEM_REGISTRY.registerObject("pump") { BlockItem(PUMP, itemProps()) }
	}

	fun regModels() {
	}

	private fun blockProps(): AbstractBlock.Properties = AbstractBlock.Properties.of(Material.STONE).strength(2F, 15F)

	private fun itemProps(): Item.Properties = Item.Properties().apply { tab(PollutantPump.GROUP) }
}
