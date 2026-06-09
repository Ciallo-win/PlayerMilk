package com.ciallo.playermilk;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public final class MilkListener implements Listener {

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND && event.getHand() != null) {
            return;
        }

        if (!(event.getRightClicked() instanceof Player)) {
            return;
        }

        Player milker = event.getPlayer();
        Player target = (Player) event.getRightClicked();

        FileConfiguration config = PlayerMilk.getInstance().getConfig();
        if (!config.getBoolean("Milk.Allow_Chestplate_Milk", false)) {
            ItemStack chest = target.getInventory().getChestplate();
            if (chest != null && chest.getType() != Material.AIR) {
                String msg = config.getString("Messages.Chestplate_Block", "&c不能给穿胸甲的玩家挤奶！");
                if (!msg.equalsIgnoreCase("none")) {
                    milker.sendMessage(PlayerMilk.colorize(msg));
                }
                return;
            }
        }

        ItemStack hand = milker.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() != Material.BUCKET || hand.getAmount() <= 0) {
            return;
        }

        event.setCancelled(true);

        PlayerMilk.milkPlayer(milker, target);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player target = event.getEntity();

        UUID milkerId = PlayerMilk.getLastMilker(target.getUniqueId());
        if (milkerId == null) {
            return;
        }

        Player milker = target.getServer().getPlayer(milkerId);
        if (milker == null) {
            return;
        }

        String deathMsg = PlayerMilk.getDeathMessage(milker, target);

        if (deathMsg == null) {
            event.deathMessage(null);
        } else {

            event.deathMessage(
                    net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(deathMsg)
            );
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerConsume(PlayerItemConsumeEvent event) {
        ItemStack item = event.getItem();

        if (!PlayerMilk.isPlayerMilkBucket(item)) {
            return;
        }

        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(PlayerMilk.getInstance(), () -> {
            PlayerMilk.applyMilkEffects(player);
        }, 1L);
    }
}