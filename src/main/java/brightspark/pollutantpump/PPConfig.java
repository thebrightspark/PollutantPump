package brightspark.pollutantpump;

import net.minecraftforge.common.ForgeConfigSpec;

public class PPConfig {
	private final ForgeConfigSpec configSpec;
	//@Config.Comment("The max amount of energy the pump will hold")
	//@Config.RangeInt(min = 0)
	public static int pumpMaxEnergyStorage = 1000;

	//@Config.Comment("The amount of energy the pump will use per tick")
	//@Config.RangeInt(min = 0)
	public static int pumpEnergyUse = 50;

	//@Config.Comment("The time in ticks between each pollutant block the pump tries to suck up")
	//@Config.RangeInt(min = 1)
	public static int pumpWorkRate = 60;

	//@Config.Comment("The range from the top pipe that pollution will be sucked from")
	//@Config.RangeInt(min = 1)
	public static int pumpRange = 5;

	public PPConfig() {

		ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
		builder.comment("The max amount of energy the pump will hold")
				.defineInRange("pumpMaxEnergyStorage", 1000, 0, Integer.MAX_VALUE);
		builder.comment("The amount of energy the pump will use per tick")
				.defineInRange("pumpEnergyUse", 50, 0, Integer.MAX_VALUE);
		builder.comment("The time in ticks between each pollutant block the pump tries to suck up")
				.defineInRange("pumpWorkRate", 60, 1, Integer.MAX_VALUE);
		builder.comment("The range from the top pipe that pollution will be sucked from")
				.defineInRange("pumpRange", 5, 1, Integer.MAX_VALUE);

		this.configSpec = builder.build();
	}

	public int getPumpWorkRate() {
		return this.configSpec.get("pumpWorkRate");
	}

	public int getPumpMaxEnergyStorage() {
		return this.configSpec.get("pumpMaxEnergyStorage");
	}

	public int getPumpEnergyUse() {
		return this.configSpec.get("pumpEnergyUse");
	}

	public int getPumpRange() {
		return this.configSpec.get("pumpRange");
	}
}
