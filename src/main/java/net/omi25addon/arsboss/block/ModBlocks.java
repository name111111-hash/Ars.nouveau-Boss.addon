package net.omi25addon.arsboss.block;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.omi25addon.arsboss.Arsboss;
import net.omi25addon.arsboss.block.custom.omijar;
import net.omi25addon.arsboss.item.ModItems;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(Arsboss.MODID);

    public static final DeferredBlock<net.minecraft.world.level.block.Block> OMI_BLOCK = registerBlock("omi_block",
            ()->new Block(BlockBehaviour.Properties.of().strength(2f).requiresCorrectToolForDrops().sound(SoundType.AMETHYST_CLUSTER)));

    public static final DeferredBlock<omijar> OMIJAR = registerBlock("omijar",
            () -> new omijar(BlockBehaviour.Properties.of().strength(2f).requiresCorrectToolForDrops().sound(SoundType.GLASS)));

private static <T extends net.minecraft.world.level.block.Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block){
    DeferredBlock<T> toReturn = BLOCKS.register(name,block);
    registerBlockItem(name,toReturn);
    return toReturn;


}

private static <T extends net.minecraft.world.level.block.Block> void registerBlockItem(String name, DeferredBlock<T> block){
    ModItems.ITEMS.register(name, ()-> new BlockItem(block.get(), new Item.Properties()));
}
    public static void register(IEventBus eventBus){
        BLOCKS.register(eventBus);
    }
}
