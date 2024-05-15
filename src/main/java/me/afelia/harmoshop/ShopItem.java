package me.afelia.harmoshop;


import org.bukkit.Material;

import java.util.List;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;



public class ShopItem {
    private Material icon;
    private int customModelData;
    private int price;
    private String command;
    private String displayName;
    private List<String> lore;

    public ShopItem(Material icon, int customModelData, int price, String command, String displayName, List<String> lore) {
        this.icon = icon;
        this.customModelData = customModelData;
        this.price = price;
        this.command = command;
        this.displayName = displayName;
        this.lore = lore;
    }

    public Material getIcon() {
        return icon;
    }

    public int getCustomModelData() {
        return customModelData;
    }

    public int getPrice() {
        return price;
    }

    public String getCommand() {
        return command;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getLore() {
        return lore;
    }

    public ItemStack toItemStack() {
        ItemStack itemStack = new ItemStack(icon);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            if (customModelData != 0) {
                meta.setCustomModelData(customModelData);
            }
            if (displayName != null) {
                meta.setDisplayName(displayName);
            }
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(lore);
            }
            itemStack.setItemMeta(meta);
        }
        return itemStack;
    }
}
