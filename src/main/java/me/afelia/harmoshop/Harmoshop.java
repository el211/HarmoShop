package me.afelia.harmoshop;

import me.afelia.afeliarpg.AfeliaRPG;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.inventory.InventoryType;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Harmoshop extends JavaPlugin implements Listener, InventoryHandler {

    private FileConfiguration shopConfig;
    private final Map<Inventory, InventoryHandler> activeInventories = new HashMap<>();
    private final Map<Integer, ShopItem> shopItems = new HashMap<>();

    @Override
    public void onEnable() {
        // Load or create shop.yml file
        File shopFile = new File(getDataFolder(), "shop.yml");
        if (!shopFile.exists()) {
            saveResource("shop.yml", false);
        }
        shopConfig = YamlConfiguration.loadConfiguration(shopFile);

        loadShopItems();

        // Register events
        getServer().getPluginManager().registerEvents(this, this);

        // Get AfeliaRPG plugin instance
        AfeliaRPG afeliaRPG = (AfeliaRPG) Bukkit.getPluginManager().getPlugin("AfeliaRPG");
    }

    private void loadShopItems() {
        shopItems.clear(); // Clear the shopItems map before reloading items
        ConfigurationSection shopSection = shopConfig.getConfigurationSection("shop");
        if (shopSection != null) {
            for (String key : shopSection.getKeys(false)) {
                ConfigurationSection itemSection = shopSection.getConfigurationSection(key);
                if (itemSection != null) {
                    int slot = itemSection.getInt("SLOT", 0);
                    Material icon = Material.matchMaterial(itemSection.getString("ICON", "STONE"));
                    int customModelData = itemSection.getInt("CUSTOM_MODEL_DATA", 0);
                    int price = itemSection.getInt("PRICE_IN_HARMONIES", 0);
                    String command = itemSection.getString("COMMAND_ISSUED_ON_BOUGHT", "");
                    String displayName = itemSection.getString("DISPLAY_NAME");
                    List<String> lore = itemSection.getStringList("LORE");

                    if (icon != null && displayName != null) {
                        displayName = ChatColor.translateAlternateColorCodes('&', displayName);
                        List<String> coloredLore = new ArrayList<>();
                        for (String line : lore) {
                            if (line != null) {
                                coloredLore.add(ChatColor.translateAlternateColorCodes('&', line));
                            }
                        }
                        // Check if the slot is already occupied
                        while (shopItems.containsKey(slot)) {
                            slot++; // Increment the slot until it's unique
                        }
                        shopItems.put(slot, new ShopItem(icon, customModelData, price, command, displayName, coloredLore));
                    }
                }
            }
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;
        if (command.getName().equalsIgnoreCase("hshop") || command.getName().equalsIgnoreCase("harmoshop")) {
            openShop(player);
            return true;
        } else if (command.getName().equalsIgnoreCase("hshopreload")) {
            if (player.hasPermission("harmoshop.reload")) {
                reloadShopConfig(player);
            } else {
                player.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            }
            return true;
        }
        return false;
    }

    private void reloadShopConfig(Player player) {
        File shopFile = new File(getDataFolder(), "shop.yml");
        if (shopFile.exists()) {
            shopConfig = YamlConfiguration.loadConfiguration(shopFile);
            loadShopItems();
            player.sendMessage(ChatColor.GREEN + "Shop configuration reloaded successfully.");
        } else {
            player.sendMessage(ChatColor.RED + "Shop configuration file not found.");
        }
    }

    private void openShop(Player player) {
        String title = shopConfig.getString("title", "Shop");
        title = ChatColor.translateAlternateColorCodes('&', title); // Translate color codes
        Inventory shopInventory = Bukkit.createInventory(player, 54, title);

        for (Map.Entry<Integer, ShopItem> entry : shopItems.entrySet()) {
            int slot = entry.getKey();
            ShopItem item = entry.getValue();
            // Check if the slot index is within the bounds of the inventory size
            if (slot >= 0 && slot < shopInventory.getSize()) {
                shopInventory.setItem(slot, item.toItemStack());
            } else {
                getLogger().warning("Slot index " + slot + " is out of bounds for shop inventory size.");
            }
        }

        // Register the shop inventory with the GUIManager
        activeInventories.put(shopInventory, this);

        player.openInventory(shopInventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory != null && clickedInventory.getType() == InventoryType.CHEST) {
            if (activeInventories.containsKey(clickedInventory)) {
                activeInventories.get(clickedInventory).onClick(event);
            }
        }
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        event.setCancelled(true); // Prevent players from taking items from the shop inventory
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem != null && clickedItem.getType() != Material.AIR) {
            int slot = event.getRawSlot();
            if (shopItems.containsKey(slot)) {
                ShopItem item = shopItems.get(slot);
                int harmoniePrice = item.getPrice();
                int playerHarmonies = AfeliaRPG.getHarmonie(player);
                if (playerHarmonies >= harmoniePrice) {
                    AfeliaRPG.takeHarmonie(player, harmoniePrice);
                    String command = item.getCommand();
                    if (!command.isEmpty()) {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", player.getName()));
                    }
                    player.sendMessage("Merci pour votre achat !");
                } else {
                    player.sendMessage("Vous n'avez pas assez d'harmonies à dépenser.");
                }
            }
        }
    }

    @Override
    public void onClose(InventoryCloseEvent event) {
        // Remove the inventory from activeInventories when it's closed
        activeInventories.remove(event.getInventory());
    }
}
