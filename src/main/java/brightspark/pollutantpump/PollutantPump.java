package brightspark.pollutantpump;

import brightspark.pollutantpump.registration.RegistrationManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;


@Mod(PollutantPump.MOD_ID)
public class PollutantPump {
	public static final String MOD_ID = "pollutantpump";


	public PollutantPump() {
		PPConfig.loadConfig(PPConfig.CONFIG_SPEC, FMLPaths.CONFIGDIR.get().resolve("pollutantpump.toml"));
		RegistrationManager.register();
	}

}
