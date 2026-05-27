package net.omi25addon.arsboss.block;

import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.omi25addon.arsboss.Arsboss;
import net.omi25addon.arsboss.block.entity.OmiJarBlockEntity;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Arsboss.MODID);

    public static final Supplier<BlockEntityType<OmiJarBlockEntity>> OMI_JAR =
            BLOCK_ENTITIES.register("omijar",
                    () -> BlockEntityType.Builder.of(OmiJarBlockEntity::new, ModBlocks.OMIJAR.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
