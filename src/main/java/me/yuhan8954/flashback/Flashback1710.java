package me.yuhan8954.flashback;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

@Mod(
    modid = Flashback1710.MODID,
    version = Flashback1710.VERSION,
    name = "Flashback 1710",
    acceptedMinecraftVersions = "[1.7.10]",
    acceptableRemoteVersions = "*")
public class Flashback1710 {

    public static final String MODID = "flashback1710";
    public static final Logger LOG = LogManager.getLogger(MODID);
    public static final String VERSION = Tags.VERSION;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        FlashbackRuntime.initialize();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FlashbackRuntime.onInitialized();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {

    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {

    }
}
