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

    public static final RegistryObject<BlockEntityType<StewpotBlockEntity>> STEWPOT = BLOCK_ENTITIES.register("stewpot", () -> BlockEntityType.Builder.of(StewpotBlockEntity::new, ModBlocks.STEWPOT.get()).build(null));

    public static void register(IEventBus modEventBus)
    {
        BLOCK_ENTITIES.register(modEventBus);
    }
}
