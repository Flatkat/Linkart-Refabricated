plugins {
  id("dev.kikugie.stonecutter")
}

stonecutter active "1.21.1"

stonecutter parameters {
    replacements.string(current.parsed >= "1.21.11") {
        replace("ResourceLocation", "Identifier")
        replace("net.minecraft.world.entity.vehicle.AbstractMinecart", "net.minecraft.world.entity.vehicle.minecart.AbstractMinecart")
    }

    replacements.string(current.parsed >= "26.1") {
        replace("ServerWorldEvents", "ServerLevelEvents")
        replace("START_WORLD_TICK", "START_LEVEL_TICK")
        replace("DimensionDataStorage", "SavedDataStorage")
    }
}