package net.invinciblemoebius.traumaparamedicinemod.block;

import net.invinciblemoebius.traumaparamedicinemod.ParamedicineMod;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities
{
    private ModBlockEntities(){}

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ParamedicineMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<StewpotBlockEntity>> STEWPOT =
            BLOCK_ENTITIES.register("stewpot",
                    () -> BlockEntityType.Builder.of(StewpotBlockEntity::new, ModBlocks.STEWPOT.get()).build(null));
    public static final RegistryObject<BlockEntityType<DryingRackBlockEntity>> DRYING_RACK =
            BLOCK_ENTITIES.register("drying_rack",
                    () -> BlockEntityType.Builder.of(DryingRackBlockEntity::new, ModBlocks.DRYING_RACK.get()).build(null));
    public static final RegistryObject<BlockEntityType<MolcajeteBlockEntity>> MOLCAJETE =
            BLOCK_ENTITIES.register("molcajete",
                    () -> BlockEntityType.Builder.of(MolcajeteBlockEntity::new, ModBlocks.MOLCAJETE.get()).build(null));
    public static final RegistryObject<BlockEntityType<DressingStationBlockEntity>> DRESSING_STATION =
            BLOCK_ENTITIES.register("dressing_station",
                    () -> BlockEntityType.Builder.of(DressingStationBlockEntity::new, ModBlocks.DRESSING_STATION.get()).build(null));

    public static void register(IEventBus modEventBus)
    {
        BLOCK_ENTITIES.register(modEventBus);
    }
}
