package brightspark.pollutantpump

import net.minecraftforge.common.ForgeConfigSpec

object PPConfig {
	private val SERVER: PPServerConfig
	val SERVER_SPEC: ForgeConfigSpec

	var pumpMaxEnergyStorage: Int = 0
	var pumpEnergyUse: Int = 0
	var pumpWorkRate: Int = 0
	var pumpRange: Int = 0

	init {
		ForgeConfigSpec.Builder().configure { PPServerConfig(it) }.apply {
			SERVER = left
			SERVER_SPEC = right
		}
	}

	fun bake() {
		pumpMaxEnergyStorage = SERVER.pumpMaxEnergyStorage.get()
		pumpEnergyUse = SERVER.pumpEnergyUse.get()
		pumpWorkRate = SERVER.pumpWorkRate.get()
		pumpRange = SERVER.pumpRange.get()
	}
}

class PPServerConfig(builder: ForgeConfigSpec.Builder) {
	val pumpMaxEnergyStorage: ForgeConfigSpec.IntValue = builder
		.comment("The max amount of energy the pump will hold")
		.defineInRange("pumpMaxEnergyStorage", 1000, 0, Int.MAX_VALUE)
	val pumpEnergyUse: ForgeConfigSpec.IntValue = builder
		.comment("The amount of energy the pump will use per tick")
		.defineInRange("pumpEnergyUse", 50, 0, Int.MAX_VALUE)
	val pumpWorkRate: ForgeConfigSpec.IntValue = builder
		.comment("The time in ticks between each pollutant block the pump tries to suck up")
		.defineInRange("pumpWorkRate", 60, 1, Int.MAX_VALUE)
	val pumpRange: ForgeConfigSpec.IntValue = builder
		.comment("The range from the top pipe that pollution will be sucked from")
		.defineInRange("pumpRange", 5, 1, Int.MAX_VALUE)
}
