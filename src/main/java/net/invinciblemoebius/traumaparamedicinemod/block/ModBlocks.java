package net.invinciblemoebius.traumaparamedicinemod.block;

import net.invinciblemoebius.traumaparamedicinemod.ParamedicineMod;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks
{
    private ModBlocks() {}

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ParamedicineMod.MOD_ID);

    public static final RegistryObject<Block> STEWPOT =
            BLOCKS.register("stewpot", () -> new StewpotBlock(
                    BlockBehaviour.Properties.of()
                            .strength(2.0f)
                            .sound(SoundType.METAL)
                            .noOcclusion()));
    public static final RegistryObject<Block> CLAY_STEWPOT =
            BLOCKS.register("clay_stewpot", () -> new Block(
                    BlockBehaviour.Properties.of()
                            .strength(1.0f)
                            .sound(SoundType.GRAVEL)
                            .noOcclusion()));
    public static final RegistryObject<Block> DRYING_RACK =
            BLOCKS.register("drying_rack", () -> new DryingRackBlock(
                    BlockBehaviour.Properties.of()
                            .strength(1.0f)
                            .sound(SoundType.WOOD)
                            .noOcclusion()));
    public static final RegistryObject<Block> MOLCAJETE =
            BLOCKS.register("molcajete", () -> new MolcajeteBlock(
                    BlockBehaviour.Properties.of()
                            .strength(1.5f)
                            .sound(SoundType.STONE)
                            .noOcclusion()));
    public static final RegistryObject<Block> DRESSING_STATION =
            BLOCKS.register("dressing_station", () -> new DressingStationBlock(
                    BlockBehaviour.Properties.of()
                            .strength(1.0f)
                            .sound(SoundType.WOOD).noOcclusion()));

    public static void register(IEventBus modEventBus)
    {
        BLOCKS.register(modEventBus);
    }
}