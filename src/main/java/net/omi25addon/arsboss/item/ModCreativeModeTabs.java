package net.omi25addon.arsboss.item;
import net.omi25addon.arsboss.Arsboss;
import net.omi25addon.arsboss.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.omi25addon.arsboss.Arsboss;
import net.omi25addon.arsboss.block.ModBlocks;

import java.util.function.Supplier;

public class ModCreativeModeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TAB =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Arsboss.MODID);

    public static final Supplier<CreativeModeTab> OMI_ITEMS_TAB = CREATIVE_MODE_TAB.register("omi_items_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.OMISHARD.get()))
                    .title(Component.translatable("creativetab.tutorialmod.omi_items"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(ModItems.OMISHARD);


                        //output.accept(ModItems.CHiSEL);
                    }).build());

    public static final Supplier<CreativeModeTab> OMI_BLOCK_TAB = CREATIVE_MODE_TAB.register("omi_blocks_tab",
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModBlocks.OMI_BLOCK))
                    .withTabsBefore(ResourceLocation.fromNamespaceAndPath(Arsboss.MODID, "omi_items_tab"))
                    .title(Component.translatable("creativetab.omi25peakcreation.omi_blocks"))
                    .displayItems((itemDisplayParameters, output) -> {
                        output.accept(ModBlocks.OMI_BLOCK);
                        output.accept(ModBlocks.OMIJAR);


                        //output.accept(ModBlocks.OmiJar;

                    }).build());


    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TAB.register(eventBus);
    }
}
