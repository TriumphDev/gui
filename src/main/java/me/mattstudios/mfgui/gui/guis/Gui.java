package me.mattstudios.mfgui.gui.guis;

import org.bukkit.event.inventory.InventoryType;
import org.bukkit.plugin.Plugin;

public final class Gui extends BaseGui {

    public Gui(final Plugin plugin, final int rows, final String title) {
        super(plugin, rows, title);
    }

    public Gui(final Plugin plugin, final InventoryType type, final String title) { super(plugin, type, title); }

    public Gui(final Plugin plugin, final String title) {
        super(plugin, title);
    }

}
