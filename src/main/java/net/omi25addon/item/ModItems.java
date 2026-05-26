package net.omi25addon.item;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.omi25addon.Arsboss;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Arsboss.MODID);

    public static final DeferredItem<Item>OMISHARD = ITEMS.register("omishard",
            ()->new Item(new Item.Properties()));


    public static void register(IEventBus eventBus){
        ITEMS.register(eventBus);
    }
}
