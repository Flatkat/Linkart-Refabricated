package com.github.vini2003.linkart.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

@Mixin(DimensionDataStorage.class)
public interface PersistentStateAccessor {
    @Accessor("cache")
    Map<String, SavedData> linkart$loadedStates();
}
