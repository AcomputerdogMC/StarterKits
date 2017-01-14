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
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PluginStarterKits extends JavaPlugin implements Listener {

    private ItemStack[] kit;
    private boolean kitsEnabled;
    private File configFile;
    private File playerFile;
    private Set<String> kittedPlayers;

    //don't reset during onEnable or onDisable
    private boolean reloading = false;

    @Override
    public void onEnable() {
        try {
            kittedPlayers = new HashSet<>();
            if (!getDataFolder().isDirectory() && !getDataFolder().mkdirs()) {
                getLogger().warning("Unable to create data directory!");
            }
            configFile = new File(getDataFolder(), "config.yml");
            if (!configFile.isFile()) {
                copyConfig();
            }
            playerFile = new File(getDataFolder(), "players.lst");
            if (!playerFile.isFile()) {
                savePlayers();
            }
            readConfiguration();
            //don't register events or load players if they are not needed
            if (kitsEnabled) {
                loadPlayers();
                if (!reloading) { //don't register events twice
                    getServer().getPluginManager().registerEvents(this, this);
                }
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
        playerFile = null;
        kit = null;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        String uuid = p.getUniqueId().toString();
        if (!kittedPlayers.contains(uuid) && p.hasPermission("starterkits.receive")) {
            try {
                registerPlayer(uuid);
                p.getInventory().addItem(kit);
            } catch (IOException ex) {
                getLogger().warning("Exception giving kit to player: " + uuid);
                ex.printStackTrace();
                p.sendMessage(ChatColor.RED + "An exception occurred giving you a starter kit!  Please report this, and/or reconnect to try again.");
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (command.getName().toLowerCase()) {
            case "reload":
                reloading = true;
                onDisable();
                onEnable();
                reloading = false;
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Unknown command!");
        }
        return true;
    }

    private void registerPlayer(String uuid) throws IOException {
        kittedPlayers.add(uuid);
        savePlayers();
    }

    private void loadPlayers() throws IOException {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(playerFile));
            while (in.ready()) {
                kittedPlayers.add(in.readLine());
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {}
            }
        }
    }

    private void savePlayers() throws IOException {
        Writer out = null;
        try {
            out = new BufferedWriter(new FileWriter(playerFile));
            for (String uuid : kittedPlayers) {
                out.write(uuid);
                out.write("\n");
            }
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignored) {}
            }
        }
    }

    private void readConfiguration() throws IOException, InvalidConfigurationException {
        FileConfiguration conf = new YamlConfiguration();
        conf.load(configFile);
        kitsEnabled = conf.getBoolean("kits_enabled", false);
        if (kitsEnabled) {
            List<Map<?, ?>> map = conf.getMapList("kit");
            kit = new ItemStack[map.size()];
            for (int i = 0; i < map.size(); i++) {
                Map<?, ?> item = map.get(i);
                String name = (String) item.get("name");
                int count = (Integer) item.get("count");
                try {
                    Material mat = Material.valueOf(name);
                    kit[i] = new ItemStack(mat, count);
                } catch (IllegalArgumentException e) {
                    getLogger().warning("Invalid item name in kit: " + name);
                }
            }
        } else {
            kit = new ItemStack[0];
        }
    }

    private void copyConfig() throws IOException {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = getClass().getResourceAsStream("/default.yml");
            out = new FileOutputStream(configFile);
           while (in.available() > 0) {
               out.write(in.read());
           }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {}
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignored) {}
            }
        }
    }
}
