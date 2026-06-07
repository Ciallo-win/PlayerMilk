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

    private static final List<String> EFFECT_SUB_COMMANDS = Arrays.asList("list", "add", "remove");

    private static final List<String> EFFECT_IDS = Arrays.asList(
            "speed", "slowness", "haste", "mining_fatigue", "strength", "instant_health",
            "instant_damage", "jump_boost", "nausea", "regeneration", "resistance",
            "fire_resistance", "water_breathing", "invisibility", "blindness",
            "night_vision", "hunger", "weakness", "poison", "wither",
            "health_boost", "absorption", "saturation", "glowing", "levitation",
            "luck", "unluck", "slow_falling", "conduit_power", "dolphins_grace",
            "bad_omen", "hero_of_the_village", "darkness"
    );

    private static final Map<String, String> SETTINGS_MAP = new LinkedHashMap<>();
    static {
        SETTINGS_MAP.put("enabled",             "Milk.Enabled");
        SETTINGS_MAP.put("damage",              "Milk.Damage");
        SETTINGS_MAP.put("death-message",       "Milk.Death_Message");
        SETTINGS_MAP.put("bucket-name",         "Milk.Bucket_Name");
        SETTINGS_MAP.put("cooldown",            "Milk.Cooldown");
        SETTINGS_MAP.put("allow-chestplate-milk", "Milk.Allow_Chestplate_Milk");
        SETTINGS_MAP.put("bucket-lore",         "Milk.Bucket_Lore");
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
            case "help":
                sendHelp(sender, label);
                break;
            case "about":
                sender.sendMessage(PlayerMilk.colorize("&8[&cPlayerMilk&8]&7"));
                sender.sendMessage(PlayerMilk.colorize("&7Version: &f" + PlayerMilk.getInstance().getDescription().getVersion()));
                sender.sendMessage(PlayerMilk.colorize("&7Author: &f P1ay2r"));
                sender.sendMessage(PlayerMilk.colorize("&7Description: &fRight-click players with an empty bucket to milk them!"));
                break;
            case "reload":
                if (!sender.hasPermission("playermilk.reload")) {
                    sender.sendMessage(PlayerMilk.colorize("&c没有权限使用此子命令。需要: playermilk.reload"));
                    return true;
                }
                PlayerMilk.reload();
                sender.sendMessage(PlayerMilk.colorize(
                        PlayerMilk.getInstance().getConfig().getString("Command.Reload",
                                "&aConfiguration reloaded!")));
                break;
            case "toggle":
                if (!sender.hasPermission("playermilk.toggle")) {
                    sender.sendMessage(PlayerMilk.colorize("&c没有权限使用此子命令。需要: playermilk.toggle"));
                    return true;
                }
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
            case "set":
                if (!sender.hasPermission("playermilk.set")) {
                    sender.sendMessage(PlayerMilk.colorize("&c没有权限使用此子命令。需要: playermilk.set"));
                    return true;
                }
                handleSet(sender, args, label);
                break;
            case "effects":
                if (!sender.hasPermission("playermilk.effects")) {
                    sender.sendMessage(PlayerMilk.colorize("&c没有权限使用此子命令。需要: playermilk.effects"));
                    return true;
                }
                handleEffects(sender, args, label);
                break;
            default:
                sendHelp(sender, label);
                break;
        }

        return true;
    }

    // ===========================
    //  /playermilk effects ...
    // ===========================
    private void handleEffects(CommandSender sender, String[] args, String label) {
        if (args.length < 2) {
            sendEffectsHelp(sender, label);
            return;
        }

        String sub = args[1].toLowerCase();
        switch (sub) {
            case "list":
                handleEffectsList(sender, label);
                break;
            case "add":
                handleEffectsAdd(sender, args, label);
                break;
            case "remove":
                handleEffectsRemove(sender, args, label);
                break;
            default:
                sender.sendMessage(PlayerMilk.colorize("&cUsage: /" + label + " effects <list|add|remove> ..."));
                break;
        }
    }

    private void sendEffectsHelp(CommandSender sender, String label) {
        sender.sendMessage(PlayerMilk.colorize("&7/" + label + " effects list &8- &fShow all effects"));
        sender.sendMessage(PlayerMilk.colorize("&7/" + label + " effects add <id> <duration(s)> <amplifier> [particles]"));
        sender.sendMessage(PlayerMilk.colorize("&cExample: /" + label + " effects add speed 60 0 true"));
        sender.sendMessage(PlayerMilk.colorize("&7/" + label + " effects remove <id>"));
        sender.sendMessage(PlayerMilk.colorize("&cExample: /" + label + " effects remove speed"));
    }

    private void handleEffectsList(CommandSender sender, String label) {
        FileConfiguration config = PlayerMilk.getInstance().getConfig();
        ConfigurationSection section = config.getConfigurationSection("Milk.Effects");

        if (section == null || section.getKeys(false).isEmpty()) {
            sender.sendMessage(PlayerMilk.colorize("&7No effects configured."));
            return;
        }

        sender.sendMessage(PlayerMilk.colorize("&8--- &fEffects &8---"));
        for (String key : section.getKeys(false)) {
            ConfigurationSection e = section.getConfigurationSection(key);
            if (e == null) continue;

            String id = e.getString("id", "&c?");
            int dur = e.getInt("duration", 0);
            int amp = e.getInt("amplifier", 0);
            Object p = e.get("particles");
            String pStr = (p instanceof Boolean) ? p.toString() : (p != null ? "\"" + p + "\"" : "true");

            sender.sendMessage(PlayerMilk.colorize(
                    "&7  &f" + key + " &8- &f" + id
                            + " &8| &7" + dur + "s &8| &7" + amp
                            + " &8| &7particles=&f" + pStr));
        }
    }

    private void handleEffectsAdd(CommandSender sender, String[] args, String label) {
        if (args.length < 5) {
            sender.sendMessage(PlayerMilk.colorize("&cUsage: /" + label + " effects add <id> <duration> <amplifier> [particles]"));
            sender.sendMessage(PlayerMilk.colorize("&7  id          Effect name or number (1-33), e.g. speed, 10"));
            sender.sendMessage(PlayerMilk.colorize("&7  duration    Seconds (e.g. 60)"));
            sender.sendMessage(PlayerMilk.colorize("&7  amplifier   0=I, 1=II, etc."));
            sender.sendMessage(PlayerMilk.colorize("&7  particles   true/false or particle type (e.g. HEART), optional, default: true"));
            sender.sendMessage(PlayerMilk.colorize("&cExample: &f/" + label + " effects add speed 60 0 true"));
            sender.sendMessage(PlayerMilk.colorize("&cExample: &f/" + label + " effects add speed 120 2 HEART"));
            return;
        }

        String id = args[2].trim();
        int duration, amplifier;

        try {
            duration = Integer.parseInt(args[3]);
            amplifier = Integer.parseInt(args[4]);
            if (duration < 1) duration = 1;
            if (amplifier < 0) amplifier = 0;
        } catch (NumberFormatException e) {
            sender.sendMessage(PlayerMilk.colorize("&cDuration and amplifier must be numbers!"));
            return;
        }

        String effectName = id.toLowerCase().replace(".", "_").replace(" ", "_");
        if (effectName.length() > 40) effectName = effectName.substring(0, 40);

        FileConfiguration config = PlayerMilk.getInstance().getConfig();
        String path = "Milk.Effects." + effectName;

        if (config.contains(path)) {
            sender.sendMessage(PlayerMilk.colorize("&eEffect '&f" + effectName + "&e' updated."));
        }

        config.set(path + ".id", id);
        config.set(path + ".duration", duration);
        config.set(path + ".amplifier", amplifier);
        config.set(path + ".show_icon", true);

        if (args.length >= 6) {
            String pVal = args[5];
            if (pVal.equalsIgnoreCase("true") || pVal.equalsIgnoreCase("false")) {
                config.set(path + ".particles", Boolean.parseBoolean(pVal.toLowerCase()));
            } else {
                config.set(path + ".particles", pVal.toUpperCase());
            }
        }

        PlayerMilk.getInstance().saveConfig();

        sender.sendMessage(PlayerMilk.colorize("&aEffect added: &f" + effectName));
        sender.sendMessage(PlayerMilk.colorize("&7  id=&f" + id + " &7duration=&f" + duration + "s &7amplifier=&f" + amplifier));
        sender.sendMessage(PlayerMilk.colorize("&7  /" + label + " effects remove " + effectName + " &8- &fto delete"));
    }

    private void handleEffectsRemove(CommandSender sender, String[] args, String label) {
        if (args.length < 3) {
            sender.sendMessage(PlayerMilk.colorize("&cUsage: /" + label + " effects remove <id>"));
            return;
        }

        String name = args[2].toLowerCase();
        FileConfiguration config = PlayerMilk.getInstance().getConfig();
        String path = "Milk.Effects." + name;

        if (!config.contains(path)) {
            sender.sendMessage(PlayerMilk.colorize("&cEffect '&f" + name + "&c' not found."));
            return;
        }

        config.set(path, null);
        PlayerMilk.getInstance().saveConfig();
        sender.sendMessage(PlayerMilk.colorize("&aEffect '&f" + name + "&a' removed."));
    }

    // ===========================
    //  /playermilk set ...
    // ===========================
    private void handleBucketLoreSet(CommandSender sender, String[] args, String configPath, String label) {
        FileConfiguration config = PlayerMilk.getInstance().getConfig();

        if (args.length < 4) {
            sender.sendMessage(PlayerMilk.colorize("&cUsage: /" + label + " set bucket-lore <line> <text>"));
            sender.sendMessage(PlayerMilk.colorize("&7  line can be:"));
            sender.sendMessage(PlayerMilk.colorize("&7    number (1, 2, 3...) — set that line"));
            sender.sendMessage(PlayerMilk.colorize("&7    add               — append a line"));
            sender.sendMessage(PlayerMilk.colorize("&7    remove <number>   — delete a line"));
            sender.sendMessage(PlayerMilk.colorize("&7    clear             — remove all lore"));
            List<String> current = config.getStringList(configPath);
            if (current.isEmpty()) {
                sender.sendMessage(PlayerMilk.colorize("&7Current lore: &8(empty)"));
            } else {
                sender.sendMessage(PlayerMilk.colorize("&7Current lore:"));
                for (int i = 0; i < current.size(); i++) {
                    sender.sendMessage(PlayerMilk.colorize("&7  " + (i + 1) + ". &f" + current.get(i)));
                }
            }
            return;
        }

        String lineArg = args[2];
        List<String> current = config.getStringList(configPath);

        if (lineArg.equalsIgnoreCase("add")) {
            String text = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
            current.add(text);
            config.set(configPath, current);
            PlayerMilk.getInstance().saveConfig();
            sender.sendMessage(PlayerMilk.colorize("&aLore line added: &f" + text));
            return;
        }

        if (lineArg.equalsIgnoreCase("remove")) {
            if (args.length < 4) {
                sender.sendMessage(PlayerMilk.colorize("&cUsage: /" + label + " set bucket-lore remove <line_number>"));
                return;
            }
            try {
                int lineNum = Integer.parseInt(args[3]);
                if (lineNum < 1 || lineNum > current.size()) {
                    sender.sendMessage(PlayerMilk.colorize("&cInvalid line number! Range: 1-" + current.size()));
                    return;
                }
                String removed = current.remove(lineNum - 1);
                config.set(configPath, current);
                PlayerMilk.getInstance().saveConfig();
                sender.sendMessage(PlayerMilk.colorize("&aRemoved lore line: &f" + removed));
            } catch (NumberFormatException e) {
                sender.sendMessage(PlayerMilk.colorize("&cInvalid line number!"));
            }
            return;
        }

        if (lineArg.equalsIgnoreCase("clear")) {
            config.set(configPath, new ArrayList<>());
            PlayerMilk.getInstance().saveConfig();
            sender.sendMessage(PlayerMilk.colorize("&aLore cleared."));
            return;
        }

        try {
            int lineNum = Integer.parseInt(lineArg);
            if (args.length < 4) {
                sender.sendMessage(PlayerMilk.colorize("&cUsage: /" + label + " set bucket-lore " + lineNum + " <text>"));
                return;
            }
            String text = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
            while (current.size() < lineNum) {
                current.add("");
            }
            current.set(lineNum - 1, text);
            config.set(configPath, current);
            PlayerMilk.getInstance().saveConfig();
            sender.sendMessage(PlayerMilk.colorize("&aSet lore line " + lineNum + " to: &f" + text));
        } catch (NumberFormatException e) {
            sender.sendMessage(PlayerMilk.colorize("&cInvalid line number or sub-command!"));
        }
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

        if (args.length < 3 && !"bucket-lore".equals(key)) {
            sender.sendMessage(PlayerMilk.colorize("&cUsage: /playermilk set " + key + " <value>"));
            return;
        }

        String value = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        String configPath = SETTINGS_MAP.get(key);

        if ("bucket-lore".equals(key)) {
            handleBucketLoreSet(sender, args, configPath, label);
            return;
        }

        try {
            Object current = config.get(configPath);
            if (current instanceof Boolean) {
                boolean boolVal = value.equalsIgnoreCase("true") || value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("1");
                if (!boolVal && !value.equalsIgnoreCase("false") && !value.equalsIgnoreCase("no") && !value.equalsIgnoreCase("0")) {
                    sender.sendMessage(PlayerMilk.colorize("&cInvalid value! Use true/false."));
                    return;
                }
                config.set(configPath, boolVal);
            } else if (current instanceof Number) {
                config.set(configPath, Double.parseDouble(value));
            } else {
                config.set(configPath, value);
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(PlayerMilk.colorize("&cInvalid number!"));
            return;
        }

        PlayerMilk.getInstance().saveConfig();
        sender.sendMessage(PlayerMilk.colorize("&f" + key + " &7set to: &f" + config.get(configPath)));
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(PlayerMilk.colorize("&7/" + label + " help &8- &fShow this help"));
        sender.sendMessage(PlayerMilk.colorize("&7/" + label + " set &8- &fList/set settings"));
        sender.sendMessage(PlayerMilk.colorize("&7/" + label + " toggle &8- &fEnable/disable"));
        sender.sendMessage(PlayerMilk.colorize("&7/" + label + " reload &8- &fReload config"));
        sender.sendMessage(PlayerMilk.colorize("&7/" + label + " about &8- &fPlugin info"));
        sender.sendMessage(PlayerMilk.colorize("&7/" + label + " effects list &8- &fList effects"));
        sender.sendMessage(PlayerMilk.colorize("&7/" + label + " effects add <id> <dur> <amp> [particles]"));
        sender.sendMessage(PlayerMilk.colorize("&7/" + label + " effects remove <id>"));
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
                if (cmd.startsWith(partial)) completions.add(cmd);
            }
        } else if (args[0].equalsIgnoreCase("set")) {
            if (args.length == 2) {
                String partial = args[1].toLowerCase();
                for (String k : SETTINGS_MAP.keySet()) {
                    if (k.startsWith(partial)) completions.add(k);
                }
            } else if (args.length >= 3) {
                String key = args[1].toLowerCase();
                if ("enabled".equals(key) || "allow-chestplate-milk".equals(key)) {
                    completions.addAll(Arrays.asList("true", "false"));
                } else if ("damage".equals(key)) {
                    completions.add("1.0");
                } else if ("cooldown".equals(key)) {
                    completions.add("5");
                } else if ("date-format".equals(key)) {
                    completions.add("yyyy-MM-dd HH:mm:ss");
                }
            }
        } else if (args[0].equalsIgnoreCase("effects")) {
                if (args.length == 2) {
                    String partial = args[1].toLowerCase();
                    for (String s : EFFECT_SUB_COMMANDS) {
                        if (s.startsWith(partial)) completions.add(s);
                    }
                } else if (args.length == 3) {
                    String sub = args[1].toLowerCase();
                    String partial = args[2].toLowerCase();
                    if ("remove".equals(sub)) {
                        FileConfiguration config = PlayerMilk.getInstance().getConfig();
                        ConfigurationSection sec = config.getConfigurationSection("Milk.Effects");
                        if (sec != null) {
                            for (String key : sec.getKeys(false)) {
                                if (key.startsWith(partial)) completions.add(key);
                            }
                        }
                    }
                } else if ("add".equals(args[1].toLowerCase())) {
                    if (args.length <= 3) {
                        String partial = (args.length >= 3 ? args[2].toLowerCase() : "");
                        for (String id : EFFECT_IDS) {
                            if (id.startsWith(partial)) completions.add(id);
                        }
                    } else if (args.length == 4) {
                        completions.add("60");
                    } else if (args.length == 5) {
                        completions.add("0");
                    } else if (args.length == 6) {
                        String partial = args[5].toLowerCase();
                        if ("t".startsWith(partial)) completions.add("true");
                        if ("f".startsWith(partial)) completions.add("false");
                    }
                }
            }

            return completions;
    }
}
