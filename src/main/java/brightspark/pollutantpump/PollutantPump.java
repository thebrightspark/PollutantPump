package brightspark.pollutantpump;

import brightspark.pollutantpump.registration.RegistrationManager;
import net.minecraftforge.fml.common.Mod;


@Mod(PollutantPump.MOD_ID)
public class PollutantPump {
	public static final String MOD_ID = "pollutantpump";
//	public static final String NAME = "Pollutant Pump";
//	public static final String VERSION = "@VERSION@";
//	public static final String DEPENDENCIES = "required-after:adpother@[1.16.4-1,)";


	public PollutantPump() {
		RegistrationManager.register();
	}

}
