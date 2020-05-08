package brightspark.pollutantpump;

import net.minecraftforge.common.config.Config;

@Config(modid = PollutantPump.MOD_ID)
public class PPConfig {
	@Config.Comment("The max amount of energy the pump will hold")
	@Config.RangeInt(min = 0)
	public static int pumpMaxEnergyStorage = 10000;

	@Config.Comment("The amount of energy the pump will use per tick")
	@Config.RangeInt(min = 0)
	public static int pumpEnergyUse = 50;

	@Config.Comment("The time in ticks between each pollutant block the pump tries to suck up")
	@Config.RangeInt(min = 1)
	public static int pumpWorkRate = 60;

	@Config.Comment("The range from the top pipe that pollution will be sucked from")
	@Config.RangeInt(min = 1)
	public static int pumpRange = 5;
}
