package net.invinciblemoebius.traumaparamedicinemod.item;

import net.invinciblemoebius.traumaparamedicinemod.ParamedicineMod;
import net.invinciblemoebius.traumaparamedicinemod.block.ModBlocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems
{
    private ModItems() {}

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ParamedicineMod.MOD_ID);

    public static final RegistryObject<Item> SYRINGE = ITEMS.register("syringe", () -> new SyringeItem(new Item.Properties()));
    public static final RegistryObject<Item> GLASS = ITEMS.register("glass", () -> new GlassItem(new Item.Properties()));
    public static final RegistryObject<Item> STEWPOT = ITEMS.register("stewpot", () -> new BlockItem(ModBlocks.STEWPOT.get(), new Item.Properties()));

    public static void register(IEventBus modEventBus)
    {
        ITEMS.register(modEventBus);
    }
}
