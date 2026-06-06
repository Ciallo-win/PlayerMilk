package com.ciallo.playermilk;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public final class PlayerMilk extends JavaPlugin {

    private static PlayerMilk instance;
    private static final Map<UUID, Long> cooldowns = new HashMap<>();

    private static final Map<UUID, UUID> milkDamageTracker = new HashMap<>();
    private File cooldownFile;
    private FileConfiguration cooldownConfig;

    private static NamespacedKey milkTagKey;

    private static final Map<Integer, PotionEffectType> POTION_ID_MAP = new HashMap<>();
    static {
        POTION_ID_MAP.put(1, PotionEffectType.SPEED);
        POTION_ID_MAP.put(2, PotionEffectType.SLOWNESS);
        POTION_ID_MAP.put(3, PotionEffectType.HASTE);
        POTION_ID_MAP.put(4, PotionEffectType.MINING_FATIGUE);
        POTION_ID_MAP.put(5, PotionEffectType.STRENGTH);
        POTION_ID_MAP.put(6, PotionEffectType.INSTANT_HEALTH);
        POTION_ID_MAP.put(7, PotionEffectType.INSTANT_DAMAGE);
        POTION_ID_MAP.put(8, PotionEffectType.JUMP_BOOST);
        POTION_ID_MAP.put(9, PotionEffectType.NAUSEA);
        POTION_ID_MAP.put(10, PotionEffectType.REGENERATION);
        POTION_ID_MAP.put(11, PotionEffectType.RESISTANCE);
        POTION_ID_MAP.put(12, PotionEffectType.FIRE_RESISTANCE);
        POTION_ID_MAP.put(13, PotionEffectType.WATER_BREATHING);
        POTION_ID_MAP.put(14, PotionEffectType.INVISIBILITY);
        POTION_ID_MAP.put(15, PotionEffectType.BLINDNESS);
        POTION_ID_MAP.put(16, PotionEffectType.NIGHT_VISION);
        POTION_ID_MAP.put(17, PotionEffectType.HUNGER);
        POTION_ID_MAP.put(18, PotionEffectType.WEAKNESS);
        POTION_ID_MAP.put(19, PotionEffectType.POISON);
        POTION_ID_MAP.put(20, PotionEffectType.WITHER);
        POTION_ID_MAP.put(21, PotionEffectType.HEALTH_BOOST);
        POTION_ID_MAP.put(22, PotionEffectType.ABSORPTION);
        POTION_ID_MAP.put(23, PotionEffectType.SATURATION);
        POTION_ID_MAP.put(24, PotionEffectType.GLOWING);
        POTION_ID_MAP.put(25, PotionEffectType.LEVITATION);
        POTION_ID_MAP.put(26, PotionEffectType.LUCK);
        POTION_ID_MAP.put(27, PotionEffectType.UNLUCK);
        POTION_ID_MAP.put(28, PotionEffectType.SLOW_FALLING);
        POTION_ID_MAP.put(29, PotionEffectType.CONDUIT_POWER);
        POTION_ID_MAP.put(30, PotionEffectType.DOLPHINS_GRACE);
        POTION_ID_MAP.put(31, PotionEffectType.BAD_OMEN);
        POTION_ID_MAP.put(32, PotionEffectType.HERO_OF_THE_VILLAGE);
        POTION_ID_MAP.put(33, PotionEffectType.DARKNESS);
    }

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        reloadConfig();

        loadCooldowns();

        milkTagKey = new NamespacedKey(this, "playermilk_bucket");

        getCommand("playermilk").setExecutor(new PlayerMilkCommand());

        Bukkit.getPluginManager().registerEvents(new MilkListener(), this);

        getLogger().info("PlayerMilk v" + getDescription().getVersion() + " enabled!");
    }

    @Override
    public void onDisable() {

        saveCooldowns();
        instance = null;
    }

    public static void reload() {
        instance.reloadConfig();
        loadCooldowns();
        instance.getLogger().info("Configuration reloaded.");
    }

    public static boolean milkPlayer(Player milker, Player target) {
        FileConfiguration config = instance.getConfig();

        if (!config.getBoolean("Milk.Enabled", true)) {
            return false;
        }

        int cooldownSeconds = config.getInt("Milk.Cooldown", 0);
        if (cooldownSeconds > 0) {
            long lastUsed = cooldowns.getOrDefault(milker.getUniqueId(), 0L);
            long now = System.currentTimeMillis();
            long cooldownMs = cooldownSeconds * 1000L;

            if (now - lastUsed < cooldownMs) {
                long remaining = (cooldownMs - (now - lastUsed)) / 1000L;
                String msg = config.getString("Messages.Cooldown", "&cPlease wait {time} seconds before milking again!")
                        .replace("{time}", String.valueOf(remaining + 1));
                milker.sendMessage(colorize(msg));
                return false;
            }
        }

        ItemStack hand = milker.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() != Material.BUCKET || hand.getAmount() <= 0) {
            return false;
        }

        hand.setAmount(hand.getAmount() - 1);
        milker.getInventory().setItemInMainHand(hand);

        double damage = config.getDouble("Milk.Damage", 1.0);
        if (damage > 0) {

            milkDamageTracker.put(target.getUniqueId(), milker.getUniqueId());
            target.damage(damage);
        }

        ItemStack milkBucket = new ItemStack(Material.MILK_BUCKET);
        ItemMeta meta = milkBucket.getItemMeta();
        String bucketName = config.getString("Milk.Bucket_Name", "&f{player2}'s Milk");
        if (meta != null && bucketName != null && !bucketName.isEmpty() && !bucketName.equalsIgnoreCase("none")) {
            String dateStr = new SimpleDateFormat(config.getString("Date_Format", "yyyy-MM-dd HH:mm:ss")).format(new Date());
            String name = bucketName
                    .replace("{player}", milker.getName())
                    .replace("{player2}", target.getName())
                    .replace("{time}", dateStr);
            meta.setDisplayName(colorize(name));
            milkBucket.setItemMeta(meta);
        }

        if (meta != null) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(milkTagKey, PersistentDataType.BOOLEAN, true);
            milkBucket.setItemMeta(meta);
        }

        milker.getInventory().addItem(milkBucket);

        target.getWorld().playSound(target.getLocation(), org.bukkit.Sound.ENTITY_COW_MILK, 1.0f, 1.0f);

        if (cooldownSeconds > 0) {
            cooldowns.put(milker.getUniqueId(), System.currentTimeMillis());
        }

        String milkerMsg = config.getString("Messages.Success_Milker", "&aYou milked {player2}!")
                .replace("{player}", milker.getName())
                .replace("{player2}", target.getName());
        String targetMsg = config.getString("Messages.Success_Target", "&e{player} milked you!")
                .replace("{player}", milker.getName())
                .replace("{player2}", target.getName());

        if (!milkerMsg.equalsIgnoreCase("none")) milker.sendMessage(colorize(milkerMsg));
        if (!targetMsg.equalsIgnoreCase("none")) target.sendMessage(colorize(targetMsg));

        return true;
    }

    public static String getDeathMessage(Player milker, Player target) {
        String deathMsg = instance.getConfig().getString("Milk.Death_Message", "{player2} was milked to death by {player}");
        if (deathMsg == null || deathMsg.equalsIgnoreCase("none")) {
            return null;
        }
        return colorize(deathMsg
                .replace("{player}", milker.getName())
                .replace("{player2}", target.getName()));
    }

    public static boolean toggleEnabled() {
        FileConfiguration config = instance.getConfig();
        boolean current = config.getBoolean("Milk.Enabled", true);
        config.set("Milk.Enabled", !current);
        instance.saveConfig();
        return !current;
    }

    private static void loadCooldowns() {
        cooldowns.clear();
        if (instance.cooldownFile == null) {
            instance.cooldownFile = new File(instance.getDataFolder(), "cooldowns.yml");
        }
        if (!instance.cooldownFile.exists()) return;

        instance.cooldownConfig = YamlConfiguration.loadConfiguration(instance.cooldownFile);
        if (instance.cooldownConfig.contains("cooldowns")) {
            for (String key : instance.cooldownConfig.getConfigurationSection("cooldowns").getKeys(false)) {
                UUID uuid = UUID.fromString(key);
                long time = instance.cooldownConfig.getLong("cooldowns." + key);
                cooldowns.put(uuid, time);
            }
        }
    }

    private static void saveCooldowns() {
        if (instance == null) return;
        if (instance.cooldownFile == null) {
            instance.cooldownFile = new File(instance.getDataFolder(), "cooldowns.yml");
        }
        instance.cooldownConfig = new YamlConfiguration();
        for (Map.Entry<UUID, Long> entry : cooldowns.entrySet()) {
            instance.cooldownConfig.set("cooldowns." + entry.getKey().toString(), entry.getValue());
        }
        try {
            instance.cooldownConfig.save(instance.cooldownFile);
        } catch (IOException e) {
            instance.getLogger().log(Level.WARNING, "Failed to save cooldowns", e);
        }
    }

    public static String colorize(String message) {
        if (message == null) return null;
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', message);
    }

    public static PlayerMilk getInstance() {
        return instance;
    }

    public static UUID getLastMilker(UUID targetId) {
        return milkDamageTracker.remove(targetId);
    }

    public static boolean isPlayerMilkBucket(ItemStack item) {
        if (item == null || item.getType() != Material.MILK_BUCKET || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(milkTagKey, PersistentDataType.BOOLEAN);
    }

    public static void applyMilkEffects(Player player) {
        FileConfiguration config = instance.getConfig();
        ConfigurationSection effectsSection = config.getConfigurationSection("Milk.Effects");
        if (effectsSection == null) return;

        for (String key : effectsSection.getKeys(false)) {
            ConfigurationSection effectCfg = effectsSection.getConfigurationSection(key);
            if (effectCfg == null) continue;

            String effectId = effectCfg.getString("id");
            if (effectId == null) continue;

            PotionEffectType type = null;

            try {
                int numericId = Integer.parseInt(effectId);
                type = POTION_ID_MAP.get(numericId);
            } catch (NumberFormatException ignored) {}

            if (type == null) {
                type = PotionEffectType.getByName(effectId.toUpperCase());
            }
            if (type == null) continue;

            int duration = effectCfg.getInt("duration", 60) * 20;
            int amplifier = effectCfg.getInt("amplifier", 0);

            boolean showPotionParticles = true;
            String particleTypeName = null;
            int particleAmount = 5;

            Object particlesObj = effectCfg.get("particles");
            if (particlesObj instanceof Boolean) {
                showPotionParticles = (Boolean) particlesObj;
            } else if (particlesObj instanceof String) {
                showPotionParticles = true;
                particleTypeName = (String) particlesObj;
                particleAmount = effectCfg.getInt("particle_amount", particleAmount);
            }

            boolean ambient = effectCfg.getBoolean("ambient", false);
            boolean showIcon = effectCfg.getBoolean("show_icon", true);

            player.addPotionEffect(new PotionEffect(type, duration, amplifier, ambient, showPotionParticles, showIcon));

            String soundName = effectCfg.getString("sound_name");
            if (soundName != null && !soundName.isEmpty()) {
                try {
                    player.playSound(player.getLocation(), Sound.valueOf(soundName.toUpperCase()),
                            1.0f, 1.0f);
                } catch (IllegalArgumentException ignored) {}
            }

            if (particleTypeName != null && !particleTypeName.isEmpty()) {
                try {
                    Particle particleType = Particle.valueOf(particleTypeName.toUpperCase());
                    player.getWorld().spawnParticle(particleType, player.getLocation().add(0, 0.5, 0),
                            particleAmount);
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }
}
