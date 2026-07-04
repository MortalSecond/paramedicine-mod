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

    // === USABLE ITEMS ===
    public static final RegistryObject<Item> SYRINGE = ITEMS.register(
            "syringe", () -> new SyringeItem(new Item.Properties()
                    .stacksTo(16)));
    public static final RegistryObject<Item> GLASS = ITEMS.register(
            "glass", () -> new GlassItem(new Item.Properties()
                    .stacksTo(8)));
    public static final RegistryObject<Item> STEWPOT = ITEMS.register(
            "stewpot", () -> new BlockItem(ModBlocks.STEWPOT.get(), new Item.Properties()
                    .stacksTo(1)));
    public static final RegistryObject<Item> CLAY_STEWPOT = ITEMS.register(
            "clay_stewpot", () -> new BlockItem(ModBlocks.CLAY_STEWPOT.get(), new Item.Properties()
                    .stacksTo(1)));
    public static final RegistryObject<Item> WOODEN_SHEARS = ITEMS.register(
            "wooden_shears", () -> new WoodenShearsItem(new Item.Properties()
                    .durability(32)));
    public static final RegistryObject<Item> DRYING_RACK = ITEMS.register(
            "drying_rack", () -> new BlockItem(ModBlocks.DRYING_RACK.get(), new Item.Properties()
                    .stacksTo(1)));

    // === MISC STUFF ===
    public static final RegistryObject<Item> PLANT_FIBER = ITEMS.register(
            "plant_fiber", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CORDAGE = ITEMS.register(
            "cordage", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> LONG_LEAF = ITEMS.register(
            "long_leaf", () -> new LongLeafItem(new Item.Properties()));
    public static final RegistryObject<Item> DRIED_PLANT = ITEMS.register(
            "dried_plant", () -> new DriedPlantItem(new Item.Properties()));

    public static void register(IEventBus modEventBus)
    {
        ITEMS.register(modEventBus);
    }
}
