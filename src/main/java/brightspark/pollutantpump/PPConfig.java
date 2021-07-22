package brightspark.pollutantpump;

import java.nio.file.Path;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import net.minecraftforge.common.ForgeConfigSpec;

public class PPConfig {
	public static final ForgeConfigSpec CONFIG_SPEC;
	//@Config.Comment("The max amount of energy the pump will hold")
	//@Config.RangeInt(min = 0)
	public static ForgeConfigSpec.IntValue pumpMaxEnergyStorage;

	//@Config.Comment("The amount of energy the pump will use per tick")
	//@Config.RangeInt(min = 0)
	public static ForgeConfigSpec.IntValue pumpEnergyUse;

	//@Config.Comment("The time in ticks between each pollutant block the pump tries to suck up")
	//@Config.RangeInt(min = 1)
	public static ForgeConfigSpec.IntValue pumpWorkRate;

	//@Config.Comment("The range from the top pipe that pollution will be sucked from")
	//@Config.RangeInt(min = 1)
	public static ForgeConfigSpec.IntValue pumpRange;

	static {

		ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
		builder.comment("Config for pump").push("pollutantpump");

		pumpMaxEnergyStorage = builder.comment("The max amount of energy the pump will hold")
				.defineInRange("pumpMaxEnergyStorage", 1000, 0, Integer.MAX_VALUE);
		pumpEnergyUse = builder.comment("The amount of energy the pump will use per tick")
				.defineInRange("pumpEnergyUse", 50, 0, Integer.MAX_VALUE);
		pumpWorkRate = builder.comment("The time in ticks between each pollutant block the pump tries to suck up")
				.defineInRange("pumpWorkRate", 60, 1, Integer.MAX_VALUE);
		pumpRange = builder.comment("The range from the top pipe that pollution will be sucked from")
				.defineInRange("pumpRange", 5, 1, Integer.MAX_VALUE);
		builder.pop();

		CONFIG_SPEC = builder.build();
	}

	public static void loadConfig(ForgeConfigSpec spec, Path path)
	{
		final CommentedFileConfig configData = CommentedFileConfig.builder(path).sync().autosave().writingMode(WritingMode.REPLACE).build();

		configData.load();

		spec.setConfig(configData);
	}
}
