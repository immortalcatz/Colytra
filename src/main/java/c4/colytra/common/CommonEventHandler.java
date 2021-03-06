/*
 * Copyright (c) 2017-2018 <C4>
 *
 * This Java class is distributed as a part of the Colytra mod for Minecraft.
 * Colytra is open source and distributed under the GNU Lesser General Public License v3.
 * View the source code and license file on github: https://github.com/TheIllusiveC4/Colytra
 */

package c4.colytra.common;

import c4.colytra.common.config.ConfigHandler;
import c4.colytra.common.items.ItemElytraBauble;
import c4.colytra.util.ColytraUtil;
import c4.colytra.proxy.CommonProxy;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemElytra;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.entity.player.PlayerPickupXpEvent;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import vazkii.quark.vanity.feature.DyableElytra;

import java.util.Map;

public class CommonEventHandler {

    @SubscribeEvent
    public void onPlayerXPPickUp(PlayerPickupXpEvent e) {

        if (!e.getEntityPlayer().world.isRemote) {
            ItemStack colytra = ColytraUtil.wornElytra(e.getEntityLiving());

            if (colytra != ItemStack.EMPTY && EnchantmentHelper.getEnchantmentLevel(Enchantments.MENDING, colytra) > 0) {
                boolean isBauble = colytra.getItem() instanceof ItemElytraBauble;
                int durability;

                if (isBauble) {
                    durability = colytra.getMaxDamage() - colytra.getItemDamage();
                } else if (ConfigHandler.durabilityMode != ConfigHandler.DurabilityMode.NORMAL) {
                    return;
                } else {
                    durability = colytra.getSubCompound("Elytra Upgrade").getInteger("Durability");
                }

                if (durability >= 432) {
                    return;
                }

                e.setCanceled(true);
                EntityXPOrb xpOrb = e.getOrb();
                EntityPlayer player = e.getEntityPlayer();

                if (xpOrb.delayBeforeCanPickup == 0 && player.xpCooldown == 0) {
                    player.xpCooldown = 2;
                    player.onItemPickup(xpOrb, 1);
                    int i = Math.min(xpToDurability(xpOrb.xpValue), 432 - durability);
                    xpOrb.xpValue -= durabilityToXp(i);

                    if (isBauble) {
                        colytra.setItemDamage(colytra.getMaxDamage() - (durability + i));
                    } else {
                        colytra.getSubCompound("Elytra Upgrade").setInteger("Durability", durability + i);
                    }

                    if (xpOrb.xpValue > 0) {
                        player.addExperience(xpOrb.xpValue);
                    }
                    xpOrb.setDead();
                }
            }
        }
    }

    private static int durabilityToXp(int durability)
    {
        return durability / 2;
    }

    private static int xpToDurability(int xp)
    {
        return xp * 2;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void anvilUpdate(AnvilUpdateEvent evt) {
        ItemStack left = evt.getLeft();
        boolean isChestplate = EntityLiving.getSlotForItemStack(left) == EntityEquipmentSlot.CHEST;
        boolean isAllowed;

        if (ConfigHandler.permissionMode == ConfigHandler.PermissionMode.BLACKLIST) {
            isAllowed = !ColytraUtil.itemList.contains(left.getItem());
        } else {
            isAllowed = ColytraUtil.itemList.contains(left.getItem());
        }

        if (isChestplate) {
            ItemStack right = evt.getRight();

            if (right.getItem() instanceof ItemElytra && !(left.getItem() instanceof ItemElytra) && isAllowed) {

                if (!ColytraUtil.hasElytraUpgrade(left)) {
                    handleElytraUpgrade(evt);
                }
            }

            if (ColytraUtil.hasElytraUpgrade(left) && right.getItem() == Items.LEATHER) {
                handleLeatherRepair(evt);
            }
        }
    }

    private static void handleElytraUpgrade(AnvilUpdateEvent evt) {
        ItemStack chestplate = evt.getLeft();
        ItemStack elytra = evt.getRight();
        ItemStack output = handleEnchantments(evt);

        NBTTagCompound compound = output.getOrCreateSubCompound("Elytra Upgrade");
        compound.setInteger("Active", 1);
        compound.setInteger("Durability", elytra.getMaxDamage() - elytra.getItemDamage());
        output.setRepairCost(Math.max(chestplate.getRepairCost(), elytra.getRepairCost()) * 2 + 1);
        int xpCost = 30;

        if (evt.getName() != null && !evt.getName().isEmpty() && !evt.getName().equals(chestplate.getDisplayName())) {
            output.setStackDisplayName(evt.getName());
            xpCost++;
        }

        if (CommonProxy.quarkLoaded) {
            copyElytraColor(output, elytra);
        }
        evt.setCost(xpCost);
        evt.setOutput(output);
    }

    private static void handleLeatherRepair(AnvilUpdateEvent evt) {
        ItemStack colytra = evt.getLeft();
        ItemStack leather = evt.getRight();

        int durability = colytra.getSubCompound("Elytra Upgrade").getInteger("Durability");
        int maxDurability = 432;
        int toRepair = maxDurability - durability;

        if (toRepair == 0) {
            return;
        }

        int leatherToUse = 0;

        while (toRepair > 0) {
            toRepair -= 108;
            leatherToUse++;
        }

        leatherToUse = Math.min(leatherToUse, leather.getCount());
        int newDurability = Math.min(durability + leatherToUse * 108, maxDurability);

        ItemStack output = colytra.copy();
        output.getSubCompound("Elytra Upgrade").setInteger("Durability", newDurability);
        output.setRepairCost(colytra.getRepairCost() * 2 + 1);
        int xpCost = leatherToUse + colytra.getRepairCost() + leather.getRepairCost();

        if (!evt.getName().isEmpty() && !evt.getName().equals(colytra.getDisplayName())) {
            output.setStackDisplayName(evt.getName());
            xpCost++;
        }
        evt.setMaterialCost(leatherToUse);
        evt.setCost(xpCost);
        evt.setOutput(output);
    }

    private static ItemStack handleEnchantments(AnvilUpdateEvent evt) {
        ItemStack chestplate = evt.getLeft();
        ItemStack elytra = evt.getRight();

        Map<Enchantment, Integer> map = EnchantmentHelper.getEnchantments(chestplate);
        Map<Enchantment, Integer> map1 = EnchantmentHelper.getEnchantments(elytra);
        boolean flag2 = false;
        boolean flag3 = false;

        for (Enchantment enchantment1 : map1.keySet()) {

            if (enchantment1 != null) {
                int i2 = map.getOrDefault(enchantment1, 0);
                int j2 = map1.get(enchantment1);
                j2 = i2 == j2 ? j2 + 1 : Math.max(j2, i2);
                boolean flag1 = enchantment1.canApply(chestplate);

                for (Enchantment enchantment : map.keySet()) {

                    if (enchantment != enchantment1 && !enchantment1.isCompatibleWith(enchantment)) {
                        flag1 = false;
                    }
                }

                if (!flag1) {
                    flag3 = true;
                } else {
                    flag2 = true;

                    if (j2 > enchantment1.getMaxLevel()) {
                        j2 = enchantment1.getMaxLevel();
                    }
                    map.put(enchantment1,j2);
                }
            }
        }

        if (flag3 && !flag2) {
            return ItemStack.EMPTY;
        }
        ItemStack output = chestplate.copy();
        EnchantmentHelper.setEnchantments(map, output);
        return output;
    }

    @Optional.Method(modid = "quark")
    private static void copyElytraColor(ItemStack output, ItemStack elytra) {

        if (elytra.hasTagCompound() && elytra.getTagCompound().hasKey(DyableElytra.TAG_ELYTRA_DYE)) {

            if (!output.hasTagCompound()) {
                output.setTagCompound(new NBTTagCompound());
            }
            output.getTagCompound().setInteger(DyableElytra.TAG_ELYTRA_DYE, elytra.getTagCompound().getInteger(DyableElytra.TAG_ELYTRA_DYE));
        }
    }
}
