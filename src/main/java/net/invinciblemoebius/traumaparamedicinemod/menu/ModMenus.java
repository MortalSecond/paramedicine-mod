package net.invinciblemoebius.traumaparamedicinemod.menu;

import net.invinciblemoebius.traumaparamedicinemod.ParamedicineMod;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModMenus
{
    private ModMenus() {}

    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, ParamedicineMod.MOD_ID);

    public static final RegistryObject<MenuType<StewpotMenu>> STEWPOT = MENUS.register("stewpot", () -> IForgeMenuType.create(StewpotMenu::new));
    public static final RegistryObject<MenuType<MolcajeteMenu>> MOLCAJETE = MENUS.register("molcajete", () -> IForgeMenuType.create(MolcajeteMenu::new));
    public static final RegistryObject<MenuType<DressingStationMenu>> DRESSING_STATION = MENUS.register("dressing_station", () -> IForgeMenuType.create(DressingStationMenu::new));

    public static void register(IEventBus modEventBus)
    {
        MENUS.register(modEventBus);
    }
}