package net.invinciblemoebius.traumaparamedicinemod.item;

import net.invinciblemoebius.traumaparamedicinemod.ParamedicineMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs
{
    private ModCreativeTabs() {}

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ParamedicineMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> PARAMEDICINE = TABS.register("paramedicine",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.traumaparamedicinemod"))
                    .icon(() -> new ItemStack(ModItems.SYRINGE.get()))
                    .displayItems((pParameters, pOutput) ->
                    {
                        pOutput.accept(ModItems.SYRINGE.get());
                        pOutput.accept(ModItems.GLASS.get());
                        pOutput.accept(ModItems.STEWPOT.get());
                    })
                    .build());

    public static void register(IEventBus modEventBus)
    {
        TABS.register(modEventBus);
    }
}
