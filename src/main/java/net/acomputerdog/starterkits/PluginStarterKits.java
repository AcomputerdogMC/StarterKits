package net.acomputerdog.starterkits;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Plugin main class
 * TODO support multiple kits
 */
public class PluginStarterKits extends JavaPlugin implements Listener {

    /**
     * The kit to provide to players
     * TODO store in a dedicated data structure
     */
    private ItemStack[] kit;

    /**
     * If kits are not enabled, then most plugin code will be skipped for performance
     */
    private boolean kitsEnabled;

    // TODO look for dedicated bukkit functions
    private File configFile;

    private File playersFile;

    /**
     * UUIDs of players who have already received kits
     * TODO store in database or player data of some kind, this is very inefficient
     */
    private Set<String> kittedPlayers;

    @Override
    public void onEnable() {
        try {
            kittedPlayers = new HashSet<>();
            if (!getDataFolder().isDirectory() && !getDataFolder().mkdirs()) {
                getLogger().warning("Unable to create data directory!");
            }

            // create configuration file
            configFile = new File(getDataFolder(), "config.yml");
            if (!configFile.isFile()) {
                copyConfig();
            }
            // create database of kitted players
            playersFile = new File(getDataFolder(), "players.lst");
            if (!playersFile.isFile()) {
                savePlayers();
            }

            // read plugin config
            readConfiguration();

            //don't register events or load players if they are not needed
            if (kitsEnabled) {
                // load list of kitted players
                loadPlayers();

                // register event handlers
                getServer().getPluginManager().registerEvents(this, this);
            } else {
                getLogger().info("Kits disabled; not loading players.");
            }
        } catch (Exception e) {
            super.setEnabled(false);
            throw new RuntimeException("Exception starting up!", e);
        }
    }

    @Override
    public void onDisable() {
        kittedPlayers = null;
        configFile = null;
        playersFile = null;
        kit = null;

        HandlerList.unregisterAll((JavaPlugin) this);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        String uuid = p.getUniqueId().toString();
        if (!kittedPlayers.contains(uuid) && p.hasPermission("starterkits.receive")) {
            try {
                p.getInventory().addItem(kit);
                registerPlayer(uuid);
            } catch (IOException ex) {
                getLogger().log(Level.WARNING, "Exception giving kit to player: " + uuid, ex);
                p.sendMessage(ChatColor.RED + "An exception occurred giving you a starter kit!  Please report this, and/or reconnect to try again.");
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String cmd = command.getName().toLowerCase();
        if ("reloadkits".equals(cmd)) {
            if (sender.hasPermission("starterkits.reload")) {
                onDisable();
                onEnable();
                sender.sendMessage(ChatColor.AQUA + "Reload complete.");
                getLogger().info("Reloaded.");
            } else {
                sender.sendMessage(ChatColor.RED + "You do not have permission.");
            }
        } else {
            sender.sendMessage(ChatColor.RED + "Unknown command!  Please report this error!");
        }
        return true;
    }

    /**
     * Registers a player as having received a kit
     */
    private void registerPlayer(String uuid) throws IOException {
        kittedPlayers.add(uuid);
        savePlayers();
    }

    private void loadPlayers() throws IOException {
        try (Stream<String> lines = Files.lines(playersFile.toPath())) {
            kittedPlayers.addAll(lines.collect(Collectors.toList()));
        }
    }

    /**
     * Saves the kitted players database
     */
    private void savePlayers() throws IOException {
        Files.write(playersFile.toPath(), (Iterable<String>) kittedPlayers.stream()::iterator);
    }

    /**
     * Reads the plugin configuration
     */
    private void readConfiguration() throws IOException, InvalidConfigurationException {
        // parse config file
        FileConfiguration conf = new YamlConfiguration();
        conf.load(configFile);

        // if kits are disabled then don't bother with anything
        kitsEnabled = conf.getBoolean("kits_enabled", false);
        if (kitsEnabled) {

            // get kit section
            List<Map<?, ?>> map = conf.getMapList("kit");

            // create item array of correct size
            kit = new ItemStack[map.size()];

            // fill in array from kit section
            for (int i = 0; i < map.size(); i++) {
                // get each item and its data
                Map<?, ?> item = map.get(i);
                String name = (String) item.get("name");
                int count = (Integer) item.get("count");

                // parse item info into an actual item
                kit[i] = parseItem(name, count);

                // make sure that it was valid
                if (kit[i] == null) {
                    getLogger().warning("Invalid item name in kit: " + name);
                }
            }
        } else {
            kit = new ItemStack[0];
        }
    }

    /**
     * Parses an item name and count into an ItemStack
     * TODO support more than just item type and count
     */
    private ItemStack parseItem(String name, int count) {
        try {
            Material mat = Material.valueOf(name);
            return new ItemStack(mat, count);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void copyConfig() throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/default.yml")) {
            try (OutputStream out = new FileOutputStream(configFile)) {

                byte[] buffer = new byte[1024];
                int count;

                // copy config file
                while ((count = in.read(buffer)) > 0) {
                    out.write(buffer, 0, count);
                }
            }
        }
    }
}
