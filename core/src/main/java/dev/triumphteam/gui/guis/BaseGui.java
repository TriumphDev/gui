package dev.triumphteam.gui.guis;

import dev.triumphteam.gui.components.GuiAction;
import dev.triumphteam.gui.components.GuiType;
import dev.triumphteam.gui.components.exception.GuiException;
import dev.triumphteam.gui.components.util.GuiFiller;
import org.bukkit.Bukkit;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class that every GUI extends
 * Contains all the basics for the GUI to work
 * Main and simplest implementation of this is {@link Gui}
 */
@SuppressWarnings("unused")
public abstract class BaseGui {

    // The plugin instance for registering the event and for the close delay
    @NotNull
    private static final Plugin PLUGIN = JavaPlugin.getProvidingPlugin(BaseGui.class);

    /**
     * For managing the instances of BaseGui that are currently opened
     * This is to change away from implementing {@link InventoryHolder}
     */
    @NotNull
    private static final GuiManager GUI_MANAGER = new GuiManager(PLUGIN);

    // Main inventory
    private Inventory inventory;

    // Gui filler
    private final GuiFiller filler = new GuiFiller(this);

    // Inventory attributes
    private String title;
    private int rows;

    // Gui type, defaults to chest
    private GuiType guiType = GuiType.CHEST;

    // Contains all items the GUI will have
    private final Map<Integer, GuiItem> guiItems = new LinkedHashMap<>();

    // Actions for specific slots
    private final Map<Integer, GuiAction<InventoryClickEvent>> slotActions = new LinkedHashMap<>();
    // Action to execute when clicking on any item
    private GuiAction<InventoryClickEvent> defaultClickAction;
    // Action to execute when clicking on the top part of the GUI only
    private GuiAction<InventoryClickEvent> defaultTopClickAction;
    // Action to execute when clicking on the player Inventory
    private GuiAction<InventoryClickEvent> playerInventoryAction;
    // Action to execute when dragging the item on the GUI
    private GuiAction<InventoryDragEvent> dragAction;
    // Action to execute when GUI closes
    private GuiAction<InventoryCloseEvent> closeGuiAction;
    // Action to execute when GUI opens
    private GuiAction<InventoryOpenEvent> openGuiAction;
    // Action to execute when clicked outside the GUI
    private GuiAction<InventoryClickEvent> outsideClickAction;

    // Whether or not the GUI is updating
    private boolean updating;

    // Whether or not should run the actions from the close and open methods
    private boolean runCloseAction = true;
    private boolean runOpenAction = true;

    /**
     * Main constructor that takes rows
     *
     * @param rows  The amount of rows the GUI should have
     * @param title The GUI title
     */
    public BaseGui(final int rows, @NotNull final String title) {
        int finalRows = rows;
        if (!(rows >= 1 && rows <= 6)) finalRows = 1;

        this.rows = finalRows;
        this.title = title;

        // TODO remove some of this inventory creations
        inventory = Bukkit.createInventory(null, this.rows * 9, title);
    }

    /**
     * Alternative constructor that takes {@link GuiType} instead of rows number
     *
     * @param guiType The {@link GuiType} to use
     * @param title   The GUI title
     */
    public BaseGui(@NotNull final GuiType guiType, @NotNull final String title) {
        this.title = title;
        this.guiType = guiType;

        inventory = Bukkit.createInventory(null, guiType.getInventoryType(), title);
    }

    /**
     * Sets the number of rows the GUI should have
     *
     * @param rows The number of rows to set
     * @return The GUI for easier use when declaring, works like a builder
     */
    @SuppressWarnings("UnusedReturnValue")
    public BaseGui setRows(final int rows) {
        if (guiType != GuiType.CHEST) throw new GuiException("Cannot set rows of non chest GUI!");

        int finalRows = rows;
        if (!(rows >= 1 && rows <= 6)) finalRows = 1;
        this.rows = finalRows;

        updating = true;

        final List<HumanEntity> viewers = new ArrayList<>(inventory.getViewers());

        inventory = Bukkit.createInventory(null, this.rows * 9, this.title);

        for (HumanEntity player : viewers) {
            //open(player);
        }

        updating = false;

        return this;
    }

    /**
     * Sets the {@link GuiItem} to a specific slot on the GUI
     *
     * @param slot    The GUI slot
     * @param guiItem The {@link GuiItem} to add to the slot
     */
    public void setItem(final int slot, @NotNull final GuiItem guiItem) {
        validateSlot(slot);
        guiItems.put(slot, guiItem);
    }

    /**
     * Removes the {@link GuiItem} in the specific slot
     *
     * @param slot The GUI slot
     */
    public void removeItem(final int slot) {
        validateSlot(slot);
        guiItems.remove(slot);
    }

    /**
     * Alternative {@link #removeItem(int)} with cols and rows
     *
     * @param row The row
     * @param col The column
     */
    public void removeItem(final int row, final int col) {
        removeItem(getSlotFromRowCol(row, col));
    }

    /**
     * Alternative {@link #setItem(int, GuiItem)} to set item that takes a {@link List} of slots instead
     *
     * @param slots   The slots in which the item should go
     * @param guiItem The {@link GuiItem} to add to the slots
     */
    public void setItem(@NotNull final List<Integer> slots, @NotNull final GuiItem guiItem) {
        for (final int slot : slots) {
            setItem(slot, guiItem);
        }
    }

    /**
     * Alternative {@link #setItem(int, GuiItem)} to set item that uses <i>ROWS</i> and <i>COLUMNS</i> instead of slots
     *
     * @param row     The GUI row number
     * @param col     The GUI column number
     * @param guiItem The {@link GuiItem} to add to the slot
     */
    public void setItem(final int row, final int col, @NotNull final GuiItem guiItem) {
        setItem(getSlotFromRowCol(row, col), guiItem);
    }

    /**
     * Adds {@link GuiItem}s to the GUI without specific slot
     * It'll set the item to the next empty slot available
     *
     * @param items Varargs for specifying the {@link GuiItem}s
     */
    public void addItem(@NotNull final GuiItem... items) {
        for (final GuiItem guiItem : items) {
            for (int slot = 0; slot < rows * 9; slot++) {
                if (guiItems.get(slot) != null) continue;

                guiItems.put(slot, guiItem);
                break;
            }
        }
    }

    /**
     * Sets the {@link GuiAction} of a default click on any item
     * See {@link InventoryClickEvent}
     *
     * @param defaultClickAction {@link GuiAction} to resolve when any item is clicked
     */
    public void setDefaultClickAction(@Nullable final GuiAction<InventoryClickEvent> defaultClickAction) {
        this.defaultClickAction = defaultClickAction;
    }

    /**
     * Sets the {@link GuiAction} of a default click on any item on the top part of the GUI
     * Top inventory being for example chests etc, instead of the {@link Player} inventory
     * See {@link InventoryClickEvent}
     *
     * @param defaultTopClickAction {@link GuiAction} to resolve when clicking on the top inventory
     */
    public void setDefaultTopClickAction(@Nullable final GuiAction<InventoryClickEvent> defaultTopClickAction) {
        this.defaultTopClickAction = defaultTopClickAction;
    }

    public void setPlayerInventoryAction(@Nullable final GuiAction<InventoryClickEvent> playerInventoryAction) {
        this.playerInventoryAction = playerInventoryAction;
    }

    /**
     * Sets the {@link GuiAction} to run when clicking on the outside of the inventory
     * See {@link InventoryClickEvent}
     *
     * @param outsideClickAction {@link GuiAction} to resolve when clicking outside of the inventory
     */
    public void setOutsideClickAction(@Nullable final GuiAction<InventoryClickEvent> outsideClickAction) {
        this.outsideClickAction = outsideClickAction;
    }

    /**
     * Sets the {@link GuiAction} of a default drag action
     * See {@link InventoryDragEvent}
     *
     * @param dragAction {@link GuiAction} to resolve
     */
    public void setDragAction(@Nullable final GuiAction<InventoryDragEvent> dragAction) {
        this.dragAction = dragAction;
    }

    /**
     * Sets the {@link GuiAction} to run once the inventory is closed
     * See {@link InventoryCloseEvent}
     *
     * @param closeGuiAction {@link GuiAction} to resolve when the inventory is closed
     */
    public void setCloseGuiAction(@Nullable final GuiAction<InventoryCloseEvent> closeGuiAction) {
        this.closeGuiAction = closeGuiAction;
    }

    /**
     * Sets the {@link GuiAction} to run when the GUI opens
     * See {@link InventoryOpenEvent}
     *
     * @param openGuiAction {@link GuiAction} to resolve when opening the inventory
     */
    public void setOpenGuiAction(@Nullable final GuiAction<InventoryOpenEvent> openGuiAction) {
        this.openGuiAction = openGuiAction;
    }

    /**
     * Adds a {@link GuiAction} for when clicking on a specific slot
     * See {@link InventoryClickEvent}
     *
     * @param slot       The slot that will trigger the {@link GuiAction}
     * @param slotAction {@link GuiAction} to resolve when clicking on specific slots
     */
    public void addSlotAction(final int slot, @Nullable final GuiAction<InventoryClickEvent> slotAction) {
        validateSlot(slot);
        slotActions.put(slot, slotAction);
    }

    /**
     * Alternative method for {@link #addSlotAction(int, GuiAction)} to add a {@link GuiAction} to a specific slot using <i>ROWS</i> and <i>COLUMNS</i> instead of slots
     * See {@link InventoryClickEvent}
     *
     * @param row        The row of the slot
     * @param col        The column of the slot
     * @param slotAction {@link GuiAction} to resolve when clicking on the slot
     */
    public void addSlotAction(final int row, final int col, @Nullable final GuiAction<InventoryClickEvent> slotAction) {
        addSlotAction(getSlotFromRowCol(row, col), slotAction);
    }

    /**
     * Gets a specific {@link GuiItem} on the slot
     *
     * @param slot The slot of the item
     * @return The {@link GuiItem} on the introduced slot or {@code null} if doesn't exist
     */
    @Nullable
    public GuiItem getGuiItem(final int slot) {
        return guiItems.get(slot);
    }

    /**
     * Checks whether or not the GUI is updating
     *
     * @return Whether the GUI is updating or not
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isUpdating() {
        return updating;
    }

    /**
     * Sets the updating status of the GUI
     *
     * @param updating Sets the GUI to the updating status
     */
    public void setUpdating(final boolean updating) {
        this.updating = updating;
    }

    /**
     * Opens the GUI for a {@link HumanEntity}
     * Should be overridden by all children
     *
     * @param player The {@link HumanEntity} to open the GUI to
     * @return True if the inventory opened successfully and false if it doesn't, should only be false when player is sleeping
     */
    public boolean openCheck(@NotNull final HumanEntity player) {
        if (player.isSleeping()) return false;
        GUI_MANAGER.addGui(inventory, this);
        return true;
    }

    /**
     * Closes the GUI with a {@code 2 tick} delay (to prevent items from being taken from the {@link Inventory})
     *
     * @param player The {@link HumanEntity} to close the GUI to
     */
    public void close(@NotNull final HumanEntity player) {
        close(player, true);
    }

    /**
     * Closes the GUI with a {@code 2 tick} delay (to prevent items from being taken from the {@link Inventory})
     *
     * @param player         The {@link HumanEntity} to close the GUI to
     * @param runCloseAction If should or not run the close action
     */
    public void close(@NotNull final HumanEntity player, final boolean runCloseAction) {
        Bukkit.getScheduler().runTaskLater(PLUGIN, () -> {
            this.runCloseAction = runCloseAction;
            player.closeInventory();
            this.runCloseAction = true;
        }, 2L);
    }

    /**
     * Updates the GUI for all the {@link Inventory} views
     */
    public void update() {
        inventory.clear();
        populateGui();
        for (HumanEntity viewer : new ArrayList<>(inventory.getViewers())) ((Player) viewer).updateInventory();
    }

    /**
     * Updates the specified item in the GUI at runtime, without creating a new {@link GuiItem}
     *
     * @param slot      The slot of the item to update
     * @param itemStack The {@link ItemStack} to replace in the original one in the {@link GuiItem}
     */
    public void updateItem(final int slot, @NotNull final ItemStack itemStack) {
        if (!guiItems.containsKey(slot)) return;
        final GuiItem guiItem = guiItems.get(slot);
        guiItem.setItemStack(itemStack);
        inventory.setItem(slot, guiItem.getItemStack());
    }

    /**
     * Alternative {@link #updateItem(int, ItemStack)} that takes <i>ROWS</i> and <i>COLUMNS</i> instead of slots
     *
     * @param row       The row of the slot
     * @param col       The columns of the slot
     * @param itemStack The {@link ItemStack} to replace in the original one in the {@link GuiItem}
     */
    public void updateItem(final int row, final int col, @NotNull final ItemStack itemStack) {
        updateItem(getSlotFromRowCol(row, col), itemStack);
    }

    /**
     * Alternative {@link #updateItem(int, ItemStack)} but creates a new {@link GuiItem}
     *
     * @param slot The slot of the item to update
     * @param item The {@link GuiItem} to replace in the original
     */
    public void updateItem(final int slot, @NotNull final GuiItem item) {
        if (!guiItems.containsKey(slot)) return;
        guiItems.put(slot, item);
        inventory.setItem(slot, item.getItemStack());
    }

    /**
     * Alternative {@link #updateItem(int, GuiItem)} that takes <i>ROWS</i> and <i>COLUMNS</i> instead of slots
     *
     * @param row  The row of the slot
     * @param col  The columns of the slot
     * @param item The {@link GuiItem} to replace in the original
     */
    public void updateItem(final int row, final int col, @NotNull final GuiItem item) {
        updateItem(getSlotFromRowCol(row, col), item);
    }

    /**
     * Updates the title of the GUI
     * <i>This method may cause LAG if used on a loop</i>
     *
     * @param title The title to set
     * @return The GUI for easier use when declaring, works like a builder
     */
    @Deprecated
    public BaseGui updateTitle(@NotNull final String title) {
        this.title = title;

        updating = true;

        final List<HumanEntity> viewers = new ArrayList<>(inventory.getViewers());

        inventory = Bukkit.createInventory(null, inventory.getSize(), this.title);

        for (final HumanEntity player : viewers) {
            //open(player);
        }

        updating = false;

        return this;
    }

    /**
     * Gets the {@link GuiFiller} that it's used for filling up the GUI in specific ways
     *
     * @return The {@link GuiFiller}
     */
    @NotNull
    public GuiFiller getFiller() {
        return filler;
    }

    /**
     * Gets an immutable {@link Map} with all the GUI items
     *
     * @return The {@link Map} with all the {@link #guiItems}
     */
    public Map<Integer, GuiItem> getGuiItems() {
        return guiItems;
    }

    /**
     * Gets the {@link Inventory} of this GUI
     */
    @NotNull
    public Inventory getInventory() {
        return inventory;
    }

    /**
     * Gets the amount of {@link #rows}
     *
     * @return The {@link #rows} of the GUI
     */
    public int getRows() {
        return rows;
    }

    /**
     * Gets the default click resolver
     */
    @Nullable
    GuiAction<InventoryClickEvent> getDefaultClickAction() {
        return defaultClickAction;
    }

    /**
     * Gets the default top click resolver
     */
    @Nullable
    GuiAction<InventoryClickEvent> getDefaultTopClickAction() {
        return defaultTopClickAction;
    }

    /**
     * Gets the player inventory action
     */
    @Nullable
    GuiAction<InventoryClickEvent> getPlayerInventoryAction() {
        return playerInventoryAction;
    }

    /**
     * Gets the default drag resolver
     */
    @Nullable
    GuiAction<InventoryDragEvent> getDragAction() {
        return dragAction;
    }

    /**
     * Gets the close gui resolver
     */
    @Nullable
    GuiAction<InventoryCloseEvent> getCloseGuiAction() {
        return closeGuiAction;
    }

    /**
     * Gets the open gui resolver
     */
    @Nullable
    GuiAction<InventoryOpenEvent> getOpenGuiAction() {
        return openGuiAction;
    }

    /**
     * Gets the resolver for the outside click
     */
    @Nullable
    GuiAction<InventoryClickEvent> getOutsideClickAction() {
        return outsideClickAction;
    }

    /**
     * Gets the action for the specified slot
     *
     * @param slot The slot clicked
     */
    @Nullable
    GuiAction<InventoryClickEvent> getSlotAction(final int slot) {
        return slotActions.get(slot);
    }

    /**
     * Populates the GUI with it's items
     */
    void populateGui() {
        for (final Map.Entry<Integer, GuiItem> entry : guiItems.entrySet()) {
            inventory.setItem(entry.getKey(), entry.getValue().getItemStack());
        }
    }

    boolean shouldRunCloseAction() {
        return runCloseAction;
    }

    boolean shouldRunOpenAction() {
        return runOpenAction;
    }

    /**
     * Gets the slot from the row and column passed
     *
     * @param row The row
     * @param col The column
     * @return The slot needed
     */
    int getSlotFromRowCol(final int row, final int col) {
        return (col + (row - 1) * 9) - 1;
    }

    /**
     * Sets the new title of the inventory
     *
     * @param title The new title
     */
    void setTitle(@NotNull final String title) {
        this.title = title;
    }

    /**
     * Sets the new inventory of the GUI
     *
     * @param inventory The new inventory
     */
    public void setInventory(@NotNull final Inventory inventory) {
        this.inventory = inventory;
    }

    /**
     * Checks if the slot introduces is a valid slot
     *
     * @param slot The slot to check
     */
    private void validateSlot(final int slot) {
        final int limit = guiType.getLimit();

        if (guiType == GuiType.CHEST) {
            if (slot < 0 || slot >= rows * limit) throwInvalidSlot(slot);
            return;
        }

        if (slot < 0 || slot > limit) throwInvalidSlot(slot);
    }

    /**
     * Throws an exception if the slot is invalid
     *
     * @param slot The specific slot to display in the error message
     */
    private void throwInvalidSlot(final int slot) {
        if (guiType == GuiType.CHEST) {
            throw new GuiException("Slot " + slot + " is not valid for the gui type - " + guiType.name() + " and rows - " + rows + "!");
        }

        throw new GuiException("Slot " + slot + " is not valid for the gui type - " + guiType.name() + "!");
    }

}
