package com.ciallo.playermilk;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PlayerMilkCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUB_COMMANDS = Arrays.asList("help", "reload", "toggle", "about", "set", "effects");

    private static final List<String> EFFECT_SUB_COMMANDS = Arrays.asList("list", "add", "remove", "set");

    private static final List<String> EFFECT_FIELDS = Arrays.asList("id", "duration", "amplifier", "particles",
            "particle_amount", "ambient", "show_icon", "sound_name");

    private static final List<String> COMMON_PARTICLES = Arrays.asList(
            "HEART", "HAPPY_VILLAGER", "FLAME", "CLOUD", "ENCHANT", "CRIT", "MAGIC_CRIT",
            "SOUL", "DRIPPING_HONEY", "NOTE", "VILLAGER_HAPPY", "PORTAL", "TOTEM", "GREEN_SPARKLE"
    );

    private static final List<String> COMMON_SOUNDS = Arrays.asList(
            "ENTITY_PLAYER_LEVELUP", "ENTITY_PLAYER_LEVELUP", "BLOCK_NOTE_BLOCK_PLING",
            "ENTITY_PLAYER_BURP", "ENTITY_WANDERING_TRADER_TRADE", "ENTITY_EXPERIENCE_ORB_PICKUP"
    );

    private static final Map<String, String> SETTINGS_MAP = new LinkedHashMap<>();
    static {
        SETTINGS_MAP.put("enabled",             "Milk.Enabled");
        SETTINGS_MAP.put("damage",              "Milk.Damage");
        SETTINGS_MAP.put("death-message",       "Milk.Death_Message");
        SETTINGS_MAP.put("bucket-name",         "Milk.Bucket_Name");
        SETTINGS_MAP.put("cooldown",            "Milk.Cooldown");
        SETTINGS_MAP.put("date-format",         "Date_Format");
        SETTINGS_MAP.put("success-milker",      "Messages.Success_Milker");
        SETTINGS_MAP.put("success-target",      "Messages.Success_Target");
        SETTINGS_MAP.put("cooldown-message",    "Messages.Cooldown");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("playermilk.admin")) {
            sender.sendMessage(PlayerMilk.colorize(
                    PlayerMilk.getInstance().getConfig().getString("Command.No_Permission",
                            "&cYou don't have permission to use this command!")));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                PlayerMilk.reload();
                sender.sendMessage(PlayerMilk.colorize(
                        PlayerMilk.getInstance().getConfig().getString("Command.Reload",
                                "&aConfiguration reloaded!")));
                break;

            case "toggle":
                boolean nowEnabled = PlayerMilk.toggleEnabled();
                if (nowEnabled) {
                    sender.sendMessage(PlayerMilk.colorize(
                            PlayerMilk.getInstance().getConfig().getString("Command.Toggle_On",
                                    "&aPlayerMilk has been enabled!")));
                } else {
                    sender.sendMessage(PlayerMilk.colorize(
                            PlayerMilk.getInstance().getConfig().getString("Command.Toggle_Off",
                                    "&cPlayerMilk has been disabled!")));
                }
                break;

            case "help":
                sendHelp(sender, label);
                break;

            case "about":
                sender.sendMessage(PlayerMilk.colorize("&8[&cPlayerMilk&8]&7"));
                sender.sendMessage(PlayerMilk.colorize("&7Version: &f" + PlayerMilk.getInstance().getDescription().getVersion()));
                sender.sendMessage(PlayerMilk.colorize("&7Author: &f P1ay2r"));
                sender.sendMessage(PlayerMilk.colorize("&7Description: &fRight-click players with an empty bucket to milk them!"));
                break;

            case "set":
                handleSet(sender, args, label);
                break;

            case "effects":
                handleEffects(sender, args, label);
                break;

            default:
                sendHelp(sender, label);
                break;
        }

        return true;
    }

    private void handleEffects(CommandSender sender, String[] args, String label) {
        if (args.length < 2 || args[1].equalsIgnoreCase("help")) {
            sender.sendMessage(PlayerMilk.colorize("&7/" + label + " effects list &8- &fList all effects"));
            sender.sendMessage(PlayerMilk.colorize("&7/" + label + " effects add <name> <id> <duration> <amplifier> &8- &fAdd new effect"));
            sender.sendMessage(PlayerMilk.colorize("&7/" + label + " effects remove <name> &8- &fRemove effect"));
            sender.sendMessage(PlayerMilk.colorize("&7/" + label + " effects set <name> <field> <value> &8- &fEdit effect field"));
            return;
        }

        String subCommand = args[1].toLowerCase();
        switch (subCommand) {
            case "list":
                handleEffectsList(sender, label);
                break;
            case "add":
                handleEffectsAdd(sender, args, label);
                break;
            case "remove":
                handleEffectsRemove(sender, args, label);
                break;
            case "set":
                handleEffectsSet(sender, args, label);
                break;
            default:
                sender.sendMessage(PlayerMilk.colorize("&cUnknown effects sub-command: &f" + subCommand));
                sender.sendMessage(PlayerMilk.colorize("&7Sub-commands: &f" + String.join("&7, &f", EFFECT_SUB_COMMANDS)));
                break;
        }
    }

    private void handleEffectsList(CommandSender sender, String label) {
        FileConfiguration config = PlayerMilk.getInstance().getConfig();
        ConfigurationSection effectsSection = config.getConfigurationSection("Milk.Effects");

        if (effectsSection == null || effectsSection.getKeys(false).isEmpty()) {
            sender.sendMessage(PlayerMilk.colorize("&7No effects configured."));
            sender.sendMessage(PlayerMilk.colorize("&7/" + label + " effects add <name> <id> <duration> <amplifier> &8- &fAdd new effect"));
            return;
        }

        sender.sendMessage(PlayerMilk.colorize("&8--- &fEffects &8---"));

        for (String key : effectsSection.getKeys(false)) {
            ConfigurationSection effect = effectsSection.getConfigurationSection(key);
            if (effect == null) continue;

            String id = effect.getString("id", "none");
            int duration = effect.getInt("duration", 0);
            int amplifier = effect.getInt("amplifier", 0);
            Object particlesObj = effect.get("particles");
            boolean ambient = effect.getBoolean("ambient", false);
            boolean showIcon = effect.getBoolean("show_icon", true);
            String soundName = effect.getString("sound_name", "none");

            String particleStr;
            if (particlesObj instanceof Boolean) {
                particleStr = ((Boolean) particlesObj) ? "true" : "false";
            } else if (particlesObj instanceof String) {
                particleStr = "\"" + particlesObj + "\"";
            } else {
                particleStr = "true";
            }

            String soundDisplay = (soundName.equalsIgnoreCase("none")) ? "&8none" : "&a" + soundName;
            String particleDisplay = (particleStr.equalsIgnoreCase("true"))
                    ? "&atrue" : (particleStr.equalsIgnoreCase("false"))
                    ? "&cfalse" : "&d" + particleStr;

            sender.sendMessage(PlayerMilk.colorize(
                    "&7  &f" + key + " &8| &fid=&7" + id
                            + " &8| &fdur=&7" + duration
                            + " &8| &famp=&7" + amplifier
                            + " &8| &fparticles=&7" + particleDisplay
                            + " &8| &fambient=&7" + ambient
                            + " &8| &fshow_icon=&7" + showIcon
                            + " &8| &fsound=&7" + soundDisplay));
        }

        sender.sendMessage(PlayerMilk.colorize("&8------------------------"));
    }

    private void handleEffectsAdd(CommandSender sender, String[] args, String label) {
        if (args.length < 6) {
            sender.sendMessage(PlayerMilk.colorize("&cUsage: /" + label + " effects add <name> <id> <duration> <amplifier> [particles/type] [particle_amount] [ambient] [show_icon] [sound_name]"));
            sender.sendMessage(PlayerMilk.colorize("&7Example: /" + label + " effects add speed speed 60 0 true"));
            sender.sendMessage(PlayerMilk.colorize("&7Example: /" + label + " effects add hearts speed 120 0 HEART 10 false true ENTITY_PLAYER_LEVELUP"));
            return;
        }

        String name = args[2].toLowerCase().replace(" ", "_").replace("-", "_");
        String effectId = args[3];
        int duration;
        int amplifier;

        try {
            duration = Integer.parseInt(args[4]);
            amplifier = Integer.parseInt(args[5]);
            if (duration < 1) duration = 1;
            if (amplifier < 0) amplifier = 0;
        } catch (NumberFormatException e) {
            sender.sendMessage(PlayerMilk.colorize("&cDuration and amplifier must be valid numbers!"));
            return;
        }

        FileConfiguration config = PlayerMilk.getInstance().getConfig();
        String configPath = "Milk.Effects." + name;

        if (config.contains(configPath)) {
            sender.sendMessage(PlayerMilk.colorize("&cEffect '&f" + name + "&c' already exists. Use set or remove instead."));
            return;
        }

        config.set(configPath + ".id", effectId);
        config.set(configPath + ".duration", duration);
        config.set(configPath + ".amplifier", amplifier);
        config.set(configPath + ".show_icon", true);

        if (args.length > 6) {
            String particlesValue = args[6];
            if (particlesValue.equalsIgnoreCase("true") || particlesValue.equalsIgnoreCase("false")) {
                config.set(configPath + ".particles", Boolean.parseBoolean(particlesValue.toLowerCase()));
            } else {
                config.set(configPath + ".particles", particlesValue.toUpperCase());
            }
        }

        if (args.length > 7) {
            try {
                int particleAmount = Integer.parseInt(args[7]);
                if (particleAmount < 1) particleAmount = 1;
                config.set(configPath + ".particle_amount", particleAmount);
            } catch (NumberFormatException ignored) {}
        }

        if (args.length > 8) {
            boolean ambient;
            if (args[8].equalsIgnoreCase("true") || args[8].equalsIgnoreCase("yes")) {
                ambient = true;
            } else if (args[8].equalsIgnoreCase("false") || args[8].equalsIgnoreCase("no")) {
                ambient = false;
            } else {
                sender.sendMessage(PlayerMilk.colorize("&cInvalid ambient value! Use true/false."));
                PlayerMilk.getInstance().getConfig().set(configPath, null);
                return;
            }
            config.set(configPath + ".ambient", ambient);
        }

        if (args.length > 9) {
            String sound = args[9];
            if (!sound.equalsIgnoreCase("none") && !sound.isEmpty()) {
                config.set(configPath + ".sound_name", sound.toUpperCase());
            }
        }

        PlayerMilk.getInstance().saveConfig();

        ConfigurationSection newEffect = config.getConfigurationSection(configPath);
        sender.sendMessage(PlayerMilk.colorize("&aEffect added: &f" + name));
        sender.sendMessage(PlayerMilk.colorize("&7  id: &f" + newEffect.getString("id")));
        sender.sendMessage(PlayerMilk.colorize("&7  duration: &f" + newEffect.getInt("duration") + "s"));
        sender.sendMessage(PlayerMilk.colorize("&7  amplifier: &f" + newEffect.getInt("amplifier")));
        sender.sendMessage(PlayerMilk.colorize("&7  particles: &f" + newEffect.get("particles")));
        if (newEffect.contains("particle_amount")) {
            sender.sendMessage(PlayerMilk.colorize("&7  particle_amount: &f" + newEffect.getInt("particle_amount")));
        }
        if (newEffect.contains("ambient")) {
            sender.sendMessage(PlayerMilk.colorize("&7  ambient: &f" + newEffect.getBoolean("ambient")));
        }
        sender.sendMessage(PlayerMilk.colorize("&7  show_icon: &f" + newEffect.getBoolean("show_icon")));
        if (newEffect.contains("sound_name")) {
            sender.sendMessage(PlayerMilk.colorize("&7  sound_name: &f" + newEffect.getString("sound_name")));
        }
    }

    private void handleEffectsRemove(CommandSender sender, String[] args, String label) {
        if (args.length < 3) {
            sender.sendMessage(PlayerMilk.colorize("&cUsage: /" + label + " effects remove <name>"));
            return;
        }

        String name = args[2].toLowerCase();
        FileConfiguration config = PlayerMilk.getInstance().getConfig();
        String configPath = "Milk.Effects." + name;

        if (!config.contains(configPath)) {
            sender.sendMessage(PlayerMilk.colorize("&cEffect '&f" + name + "&c' not found."));
            return;
        }

        config.set(configPath, null);
        PlayerMilk.getInstance().saveConfig();
        sender.sendMessage(PlayerMilk.colorize("&aEffect '&f" + name + "&a' removed."));
    }

    private void handleEffectsSet(CommandSender sender, String[] args, String label) {
        if (args.length < 4) {
            sender.sendMessage(PlayerMilk.colorize("&cUsage: /" + label + " effects set <name> <field> <value>"));
            sender.sendMessage(PlayerMilk.colorize("&7Fields: &f" + String.join("&7, &f", EFFECT_FIELDS)));
            return;
        }

        String name = args[2].toLowerCase();
        String field = args[3].toLowerCase();
        String fieldPath = "Milk.Effects." + name + "." + field;

        if (!EFFECT_FIELDS.contains(field)) {
            sender.sendMessage(PlayerMilk.colorize("&cUnknown field: &f" + field));
            sender.sendMessage(PlayerMilk.colorize("&7Available: &f" + String.join("&7, &f", EFFECT_FIELDS)));
            return;
        }

        FileConfiguration config = PlayerMilk.getInstance().getConfig();
        String effectPath = "Milk.Effects." + name;

        if (!config.contains(effectPath)) {
            sender.sendMessage(PlayerMilk.colorize("&cEffect '&f" + name + "&c' not found."));
            sender.sendMessage(PlayerMilk.colorize("&7Use /" + label + " effects add to create it first."));
            return;
        }

        if (field.equals("id") || field.equals("sound_name")) {
            if (args.length < 5) {
                sender.sendMessage(PlayerMilk.colorize("&cUsage: /" + label + " effects set " + name + " " + field + " <value>"));
                return;
            }
            String value = String.join(" ", Arrays.copyOfRange(args, 4, args.length));
            if (field.equals("id")) {
                config.set(fieldPath, value);
            } else {
                config.set(fieldPath, value.toUpperCase());
            }
        } else if (field.equals("duration") || field.equals("amplifier") || field.equals("particle_amount")) {
            if (args.length < 5) {
                sender.sendMessage(PlayerMilk.colorize("&cUsage: /" + label + " effects set " + name + " " + field + " <number>"));
                return;
            }
            try {
                int value = Integer.parseInt(args[4]);
                if (field.equals("duration") && value < 1) value = 1;
                if ((field.equals("amplifier") || field.equals("particle_amount")) && value < 0) value = 0;
                config.set(fieldPath, value);
            } catch (NumberFormatException e) {
                sender.sendMessage(PlayerMilk.colorize("&cInvalid number: &f" + args[4]));
                return;
            }
        } else if (field.equals("particles")) {
            if (args.length < 5) {
                sender.sendMessage(PlayerMilk.colorize("&cUsage: /" + label + " effects set " + name + " particles <true|false|PARTICLE_TYPE>"));
                return;
            }
            String value = args[4];
            if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
                config.set(fieldPath, Boolean.parseBoolean(value.toLowerCase()));
            } else {
                config.set(fieldPath, value.toUpperCase());
            }
        } else if (field.equals("ambient") || field.equals("show_icon")) {
            if (args.length < 5) {
                sender.sendMessage(PlayerMilk.colorize("&cUsage: /" + label + " effects set " + name + " " + field + " <true|false>"));
                return;
            }
            boolean boolVal;
            if (args[4].equalsIgnoreCase("true") || args[4].equalsIgnoreCase("yes") || args[4].equalsIgnoreCase("1")) {
                boolVal = true;
            } else if (args[4].equalsIgnoreCase("false") || args[4].equalsIgnoreCase("no") || args[4].equalsIgnoreCase("0")) {
                boolVal = false;
            } else {
                sender.sendMessage(PlayerMilk.colorize("&cInvalid value! Use true/false."));
                return;
            }
            config.set(fieldPath, boolVal);
        }

        PlayerMilk.getInstance().saveConfig();
        sender.sendMessage(PlayerMilk.colorize("&aSet &f" + name + "." + field + " &ato: &f" + config.get(fieldPath)));
    }

    private void handleSet(CommandSender sender, String[] args, String label) {
        FileConfiguration config = PlayerMilk.getInstance().getConfig();

        if (args.length < 2) {
            for (Map.Entry<String, String> entry : SETTINGS_MAP.entrySet()) {
                Object current = config.get(entry.getValue());
                String display = (current != null) ? current.toString() : "&onone";
                sender.sendMessage(PlayerMilk.colorize("&7  " + entry.getKey() + " &8=&f " + display));
            }
            sender.sendMessage(PlayerMilk.colorize("&7/" + label + " set <key> <value> &8- &fModify a setting"));
            return;
        }

        String key = args[1].toLowerCase();
        if (!SETTINGS_MAP.containsKey(key)) {
            sender.sendMessage(PlayerMilk.colorize("&cUnknown key: &f" + key));
            sender.sendMessage(PlayerMilk.colorize("&7Available: &f" + String.join("&7, &f", SETTINGS_MAP.keySet())));
            return;
        }

        if (args.length < 3) {
            sender.sendMessage(PlayerMilk.colorize("&cUsage: /playermilk set " + key + " <value>"));
            return;
        }

        String value = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        String configPath = SETTINGS_MAP.get(key);

        try {
            Object current = config.get(configPath);
            if (current instanceof Boolean) {
                boolean boolVal;
                if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("1")) {
                    boolVal = true;
                } else if (value.equalsIgnoreCase("false") || value.equalsIgnoreCase("no") || value.equalsIgnoreCase("0")) {
                    boolVal = false;
                } else {
                    sender.sendMessage(PlayerMilk.colorize("&cInvalid value! Use true/false."));
                    return;
                }
                config.set(configPath, boolVal);
            } else if (current instanceof Double || current instanceof Integer) {
                config.set(configPath, Double.parseDouble(value));
            } else {
                config.set(configPath, value);
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(PlayerMilk.colorize("&cInvalid number! Please provide a numeric value."));
            return;
        }

        PlayerMilk.getInstance().saveConfig();

        Object newValue = config.get(configPath);
        sender.sendMessage(PlayerMilk.colorize("&f" + key + " &7set to: &f" + newValue));
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(PlayerMilk.colorize("&7/" + label + " help &8- &fShow this help"));
        sender.sendMessage(PlayerMilk.colorize("&7/" + label + " set &8- &fList all settings"));
        sender.sendMessage(PlayerMilk.colorize("&7/" + label + " set <key> <value> &8- &fChange a setting (auto-save)"));
        sender.sendMessage(PlayerMilk.colorize("&7/" + label + " you can use {player} (active), {player2} (passive), and {time} as placeholders."));
        sender.sendMessage(PlayerMilk.colorize("&7/" + label + " toggle &8- &fEnable/disable milking"));
        sender.sendMessage(PlayerMilk.colorize("&7/" + label + " reload &8- &fReload config.yml (manual edits)"));
        sender.sendMessage(PlayerMilk.colorize("&7/" + label + " about &8- &fPlugin information"));
        sender.sendMessage(PlayerMilk.colorize("&7/" + label + " effects list &8- &fList all effects"));
        sender.sendMessage(PlayerMilk.colorize("&7/" + label + " effects add <name> <id> <dur> <amp> &8- &fAdd effect"));
        sender.sendMessage(PlayerMilk.colorize("&7/" + label + " effects remove <name> &8- &fRemove effect"));
        sender.sendMessage(PlayerMilk.colorize("&7/" + label + " effects set <name> <field> <val> &8- &fEdit field"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (!sender.hasPermission("playermilk.admin")) {
            return completions;
        }

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            for (String cmd : SUB_COMMANDS) {
                if (cmd.startsWith(partial)) {
                    completions.add(cmd);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            String partial = args[1].toLowerCase();
            for (String k : SETTINGS_MAP.keySet()) {
                if (k.startsWith(partial)) {
                    completions.add(k);
                }
            }
        } else if (args.length >= 3 && args[0].equalsIgnoreCase("set")) {
            String key = args[1].toLowerCase();
            switch (key) {
                case "enabled":
                    completions.addAll(Arrays.asList("true", "false"));
                    break;
                case "damage":
                    completions.add("1.0");
                    break;
                case "cooldown":
                    completions.add("5");
                    break;
                case "date-format":
                    completions.add("yyyy-MM-dd HH:mm:ss");
                    break;
            }
        } else if (args[0].equalsIgnoreCase("effects")) {
            handleEffectsTabComplete(sender, alias, args, completions);
        }

        return completions;
    }

    private void handleEffectsTabComplete(CommandSender sender, String alias, String[] args,
                                          List<String> completions) {
        if (args.length == 2) {
            String partial = args[1].toLowerCase();
            for (String sub : EFFECT_SUB_COMMANDS) {
                if (sub.startsWith(partial)) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 3) {
            String sub = args[1].toLowerCase();
            if (sub.equals("remove") || sub.equals("set")) {
                String partial = args[2].toLowerCase();
                FileConfiguration config = PlayerMilk.getInstance().getConfig();
                ConfigurationSection effects = config.getConfigurationSection("Milk.Effects");
                if (effects != null) {
                    for (String key : effects.getKeys(false)) {
                        if (key.startsWith(partial)) {
                            completions.add(key);
                        }
                    }
                }
            }
        } else if (args.length == 4 && args[1].equalsIgnoreCase("set")) {
            String field = args[3].toLowerCase();
            for (String f : EFFECT_FIELDS) {
                if (f.startsWith(field)) {
                    completions.add(f);
                }
            }
        } else if (args.length == 5 && args[1].equalsIgnoreCase("add")) {
            completions.addAll(Arrays.asList("speed", "slowness", "haste", "strength", "jump_boost",
                    "regeneration", "resistance", "fire_resistance", "water_breathing", "invisibility",
                    "night_vision", "hunger", "weakness", "poison", "wither", "saturation", "glowing",
                    "levitation", "luck", "slow_falling", "dolphins_grace", "conduit_power", "dolphins_grace",
                    "health_boost", "absorption", "instant_health", "instant_damage"));
        } else if (args.length >= 5 && args[1].equalsIgnoreCase("set")) {
            String field = args[3].toLowerCase();
            String valueArg = args[4].toLowerCase();
            if (field.equals("particles")) {
                if ("t".startsWith(valueArg)) completions.add("true");
                if ("f".startsWith(valueArg)) completions.add("false");
                for (String p : COMMON_PARTICLES) {
                    if (p.toLowerCase().startsWith(valueArg)) {
                        completions.add(p);
                    }
                }
            } else if (field.equals("show_icon") || field.equals("ambient")) {
                if ("t".startsWith(valueArg)) completions.add("true");
                if ("f".startsWith(valueArg)) completions.add("false");
            } else if (field.equals("sound_name")) {
                for (String s : COMMON_SOUNDS) {
                    if (s.toLowerCase().startsWith(valueArg)) {
                        completions.add(s);
                    }
                }
            }
        }
    }
}
