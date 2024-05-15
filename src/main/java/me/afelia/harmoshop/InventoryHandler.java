package me.afelia.harmoshop;


import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public interface InventoryHandler {
    void onClick(InventoryClickEvent event);

    void onClose(InventoryCloseEvent event);
}
