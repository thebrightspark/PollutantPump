package brightspark.pollutantpump.registration;

import brightspark.pollutantpump.PollutantPump;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;

public class PPTab extends ItemGroup {

    public static final PPTab INSTANCE = new PPTab();
    public PPTab() {
        super(PollutantPump.MOD_ID);
    }

    @Override
    public ItemStack createIcon() {
        return new ItemStack(PPBlocks.POLLUTION_PUMP.get());
    }
}
