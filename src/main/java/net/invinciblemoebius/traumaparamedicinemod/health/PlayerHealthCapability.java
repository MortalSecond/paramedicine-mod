package net.invinciblemoebius.traumaparamedicinemod.health;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;

public class PlayerHealthCapability implements ICapabilitySerializable<CompoundTag>
{
    public static final Capability<PlayerHealthData> PLAYER_HEALTH = CapabilityManager.get(new CapabilityToken<>(){});

    private final PlayerHealthData data = new PlayerHealthData();
    private final LazyOptional<PlayerHealthData> optional = LazyOptional.of(() -> data);

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side)
    {
        return PLAYER_HEALTH.orEmpty(cap, optional);
    }

    @Override
    public CompoundTag serializeNBT()
    {
        CompoundTag tag = new CompoundTag();
        data.saveToNBT(tag);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag tag)
    {
        data.loadFromNBT(tag);
    }

    public void invalidate()
    {
        optional.invalidate();
    }
}
