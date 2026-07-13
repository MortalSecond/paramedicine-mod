package net.invinciblemoebius.traumaparamedicinemod.item;

import net.invinciblemoebius.traumaparamedicinemod.ModConstants;
import net.invinciblemoebius.traumaparamedicinemod.ParamedicineMod;
import net.invinciblemoebius.traumaparamedicinemod.block.ModBlocks;
import net.invinciblemoebius.traumaparamedicinemod.item.items.*;
import net.invinciblemoebius.traumaparamedicinemod.wound.Dressing;
import net.invinciblemoebius.traumaparamedicinemod.wound.DressingType;
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

    // === CONTAINER ITEMS ===
    public static final RegistryObject<Item> SYRINGE = ITEMS.register(
            "syringe", () -> new SyringeItem(new Item.Properties()
                    .stacksTo(16)));
    public static final RegistryObject<Item> GLASS = ITEMS.register(
            "glass", () -> new GlassItem(new Item.Properties()
                    .stacksTo(8)));
    public static final RegistryObject<Item> JAR = ITEMS.register(
            "jar", () -> new PowderContainerItem(new Item.Properties()
                    .stacksTo(8),
                    ModConstants.JAR_CAPACITY_MG));

    // === MEDICAL ITEMS ===
    public static final RegistryObject<Item> CORDAGE_BANDAGE = ITEMS.register(
            "cordage_bandage", () -> new DressingItem(new Item.Properties().stacksTo(8),
                    () -> Dressing.builder()
                            .cleanliness(0.30f)
                            .pressure(0.70f)
                            .absorption(0.20f)
                            .adherence(0.30f)
                            .occlusion(0.20f)
                            .length(3f)
                            .build()));
    public static final RegistryObject<Item> WEAVE = ITEMS.register(
            "weave", () -> new DressingItem(new Item.Properties().stacksTo(8),
                    () -> Dressing.builder()
                            .cleanliness(0.30f)
                            .pressure(0.50f)
                            .absorption(0.20f)
                            .adherence(0.30f)
                            .occlusion(0.15f)
                            .length(0.5f)
                            .build()));
    public static final RegistryObject<Item> LONG_LEAF = ITEMS.register(
            "long_leaf", () -> new DressingItem(new Item.Properties().stacksTo(8),
                    () -> Dressing.builder()
                            .cleanliness(0.20f)
                            .pressure(0.20f)
                            .absorption(0.05f)
                            .adherence(0.40f)
                            .occlusion(0.15f)
                            .length(1f)
                            .build()));
    public static final RegistryObject<Item> GAUZE_PAD = ITEMS.register(
            "gauze_pad", () -> new DressingItem(new Item.Properties().stacksTo(8),
                    DressingType.GAUZE::create));

    // === BLOCK ITEMS ===
    public static final RegistryObject<Item> STEWPOT = ITEMS.register(
            "stewpot", () -> new BlockItem(ModBlocks.STEWPOT.get(), new Item.Properties()
                    .stacksTo(1)));
    public static final RegistryObject<Item> CLAY_STEWPOT = ITEMS.register(
            "clay_stewpot", () -> new BlockItem(ModBlocks.CLAY_STEWPOT.get(), new Item.Properties()
                    .stacksTo(1)));
    public static final RegistryObject<Item> WOODEN_SHEARS = ITEMS.register(
            "wooden_shears", () -> new WoodenShearsItem(new Item.Properties()
                    .durability(64)));
    public static final RegistryObject<Item> DRYING_RACK = ITEMS.register(
            "drying_rack", () -> new BlockItem(ModBlocks.DRYING_RACK.get(), new Item.Properties()
                    .stacksTo(1)));
    public static final RegistryObject<Item> MOLCAJETE = ITEMS.register(
            "molcajete", () -> new BlockItem(ModBlocks.MOLCAJETE.get(), new Item.Properties()
                    .stacksTo(1)));
    public static final RegistryObject<Item> DRESSING_STATION = ITEMS.register(
            "dressing_station", () -> new BlockItem(ModBlocks.DRESSING_STATION.get(), new Item.Properties()
                    .stacksTo(1)));


    // === MISC STUFF ===
    public static final RegistryObject<Item> PLANT_FIBER = ITEMS.register(
            "plant_fiber", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CORDAGE = ITEMS.register(
            "cordage", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> DRIED_PLANT = ITEMS.register(
            "dried_plant", () -> new DriedPlantItem(new Item.Properties()));

    public static void register(IEventBus modEventBus)
    {
        ITEMS.register(modEventBus);
    }
}
