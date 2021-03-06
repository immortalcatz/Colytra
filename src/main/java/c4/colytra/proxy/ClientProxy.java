/*
 * Copyright (c) 2017-2018 <C4>
 *
 * This Java class is distributed as a part of the Colytra mod for Minecraft.
 * Colytra is open source and distributed under the GNU Lesser General Public License v3.
 * View the source code and license file on github: https://github.com/TheIllusiveC4/Colytra
 */

package c4.colytra.proxy;

import c4.colytra.client.EventHandlerClient;
import c4.colytra.client.renderer.entity.layers.LayerColytra;
import c4.colytra.client.renderer.entity.layers.LayerColytraCape;
import c4.colytra.common.config.ConfigHandler;
import c4.colytra.util.ClientUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.LayerCape;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;

import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(Side.CLIENT)
public class ClientProxy extends CommonProxy {

    public void preInit(FMLPreInitializationEvent e) {
        super.preInit(e);
    }

    public void init(FMLInitializationEvent e) {
        super.init(e);
        MinecraftForge.EVENT_BUS.register(new EventHandlerClient());
        ClientUtil.init();
    }

    public void postInit(FMLPostInitializationEvent e) {
        super.postInit(e);
        editRenderLayers();
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent e) {

        if (CommonProxy.baublesLoaded && !ConfigHandler.baubles.disableBauble) {
            ModelLoader.setCustomModelResourceLocation(CommonProxy.elytraBauble, 0, new ModelResourceLocation(CommonProxy.elytraBauble.getRegistryName(), "inventory"));
        }
    }

    private static void editRenderLayers() {
        Minecraft mc = Minecraft.getMinecraft();
        RenderManager manager = mc.getRenderManager();
        Map<String, RenderPlayer> renderPlayerMap = manager.getSkinMap();
        for(RenderPlayer render : renderPlayerMap.values()) {
            render.addLayer(new LayerColytra(render));
            List<LayerRenderer> list = ReflectionHelper.getPrivateValue(RenderLivingBase.class, render, "h", "field_177097_h", "layerRenderers");
            LayerRenderer remove = null;
            for(LayerRenderer layer : list) {
                if (layer instanceof LayerCape) {
                    remove = layer;
                    break;
                }
            }

            list.remove(remove);
            list.add(new LayerColytraCape(render));
        }
        Render<?> render = manager.getEntityClassRenderObject(EntityArmorStand.class);
        ((RenderLivingBase<?>) render).addLayer(new LayerColytra((RenderLivingBase<?>) render));
    }
}
