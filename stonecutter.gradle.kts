plugins {
  id("dev.kikugie.stonecutter")
  id("fabric-loom") version "1.14-SNAPSHOT" apply false
}

stonecutter active "1.21.1"

stonecutter parameters {
    replacements.string(current.parsed >= "1.21.11") {
        replace("ResourceLocation", "Identifier")
        replace("net.minecraft.world.entity.vehicle.AbstractMinecart", "net.minecraft.world.entity.vehicle.minecart.AbstractMinecart")
    }
}