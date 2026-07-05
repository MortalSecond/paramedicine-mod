package net.invinciblemoebius.traumaparamedicinemod.item;

import net.invinciblemoebius.traumaparamedicinemod.wound.Dressing;

// Ultra-basic dressing base. Usable as-is, but terrible. A smooth leaf holds almost
// no fluid, isn't sterile, and only lightly clings. Instant early game dressing.
public class LongLeafItem extends DressingItem
{
    public LongLeafItem(Properties properties)
    {
        super(properties, () -> Dressing.builder()
                .cleanliness(0.30f)
                .absorption(0.05f)
                .adherence(0.40f)
                .pressure(0.20f)
                .occlusion(0.15f)
                .build());
    }
}