package fr.akkun.newmeriaservercore.admin;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The menu of {@code /invsee}: a vanilla 5-row chest screen (so players' clients need nothing to show it)
 * whose 45 slots are the inventory of the target player, live:
 * <pre>
 * row 1: head, chest, legs, feet, [barrier] x4, off hand
 * rows 2-4: main inventory
 * row 5: hotbar
 * </pre>
 * Below it, as in any chest, the inventory of the admin. Whatever the admin does here is done to the
 * target's inventory, and whatever happens to it shows up here right away.
 */
public class InvseeMenu extends AbstractContainerMenu {
    private static final int ROWS = 5;
    private static final int TARGET_SLOTS = ROWS * 9;
    /** The slots of the first row that hold no item of the target, shown as barriers. */
    private static final int FIRST_FILLER = 4;
    private static final int LAST_FILLER = 7;
    private static final int OFFHAND = 8;
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private final ServerPlayer target;

    public InvseeMenu(int containerId, Inventory adminInventory, ServerPlayer target) {
        super(MenuType.GENERIC_9x5, containerId);
        this.target = target;
        TargetInventory container = new TargetInventory(target);
        for (int i = 0; i < TARGET_SLOTS; i++) {
            int x = 8 + (i % 9) * 18;
            int y = 18 + (i / 9) * 18;
            if (i < ARMOR.length) {
                this.addSlot(new ArmorViewSlot(container, i, x, y, ARMOR[i], target));
            } else if (isFiller(i)) {
                this.addSlot(new FillerSlot(container, i, x, y));
            } else {
                this.addSlot(new Slot(container, i, x, y));
            }
        }
        // Same place as in a vanilla 5-row chest: 18 + rows * 18 + 13.
        this.addStandardInventorySlots(adminInventory, 8, 18 + ROWS * 18 + 13);
    }

    private static boolean isFiller(int slot) {
        return slot >= FIRST_FILLER && slot <= LAST_FILLER;
    }

    @Override
    public boolean stillValid(Player player) {
        // A player who leaves or dies is a removed entity (respawning makes a new one): the menu closes.
        return !this.target.isRemoved();
    }

    /** The barriers can never be clicked in any way (taken, swapped, cloned in creative...). */
    @Override
    public void clicked(int slotIndex, int buttonNum, ContainerInput containerInput, Player player) {
        if (slotIndex >= 0 && slotIndex < TARGET_SLOTS && isFiller(slotIndex)) {
            return;
        }
        super.clicked(slotIndex, buttonNum, containerInput, player);
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack carried, Slot target) {
        return !(target instanceof FillerSlot) && super.canTakeItemForPickAll(carried, target);
    }

    /** Shift-click: from the target to the admin; from the admin to the target's armor, then main inventory and hotbar. */
    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (slot == null || !slot.hasItem() || slot instanceof FillerSlot) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack moved = stack.copy();
        if (slotIndex < TARGET_SLOTS) {
            if (!this.moveItemStackTo(stack, TARGET_SLOTS, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, 0, ARMOR.length, false)
                && !this.moveItemStackTo(stack, 9, TARGET_SLOTS, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return moved;
    }

    /** The 45 slots of the menu, mapped onto the target's {@link Inventory}. */
    private static class TargetInventory implements Container {
        private final ServerPlayer target;

        TargetInventory(ServerPlayer target) {
            this.target = target;
        }

        /** @return the index in {@link Inventory} of a slot of the menu, or -1 for a barrier */
        private static int inventoryIndex(int slot) {
            if (slot < ARMOR.length) {
                // Inventory keeps feet, legs, chest, head at 36 to 39.
                return 39 - slot;
            }
            if (slot == OFFHAND) {
                return Inventory.SLOT_OFFHAND;
            }
            if (isFiller(slot)) {
                return -1;
            }
            // Rows 2 to 4 are the main inventory (9 to 35, same indexes); row 5 is the hotbar (0 to 8).
            return slot < 36 ? slot : slot - 36;
        }

        private Inventory inventory() {
            return this.target.getInventory();
        }

        @Override
        public int getContainerSize() {
            return TARGET_SLOTS;
        }

        @Override
        public boolean isEmpty() {
            for (int i = 0; i < TARGET_SLOTS; i++) {
                if (inventoryIndex(i) >= 0 && !getItem(i).isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            int index = inventoryIndex(slot);
            return index < 0 ? barrier() : inventory().getItem(index);
        }

        @Override
        public ItemStack removeItem(int slot, int count) {
            int index = inventoryIndex(slot);
            return index < 0 ? ItemStack.EMPTY : inventory().removeItem(index, count);
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            int index = inventoryIndex(slot);
            return index < 0 ? ItemStack.EMPTY : inventory().removeItemNoUpdate(index);
        }

        @Override
        public void setItem(int slot, ItemStack itemStack) {
            int index = inventoryIndex(slot);
            if (index >= 0) {
                inventory().setItem(index, itemStack);
            }
        }

        @Override
        public void setChanged() {
            inventory().setChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return !this.target.isRemoved();
        }

        @Override
        public void clearContent() {
            for (int i = 0; i < TARGET_SLOTS; i++) {
                setItem(i, ItemStack.EMPTY);
            }
        }

        private static ItemStack barrier() {
            ItemStack barrier = new ItemStack(Items.BARRIER);
            barrier.set(DataComponents.CUSTOM_NAME, Component.translatableWithFallback(
                    "container.newmeriaservercore.invsee.unused", "Emplacement inutilisé")
                    .withStyle(style -> style.withItalic(false).withColor(ChatFormatting.GRAY)));
            return barrier;
        }
    }

    /** An armor slot of the target: only takes what can be worn there, one at a time, like the player's own. */
    private static class ArmorViewSlot extends Slot {
        private final EquipmentSlot equipmentSlot;
        private final ServerPlayer owner;

        ArmorViewSlot(Container container, int slot, int x, int y, EquipmentSlot equipmentSlot, ServerPlayer owner) {
            super(container, slot, x, y);
            this.equipmentSlot = equipmentSlot;
            this.owner = owner;
        }

        @Override
        public boolean mayPlace(ItemStack itemStack) {
            return itemStack.canEquip(this.equipmentSlot, this.owner);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    /** A barrier: nothing can be put there or taken from there. */
    private static class FillerSlot extends Slot {
        FillerSlot(Container container, int slot, int x, int y) {
            super(container, slot, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack itemStack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }
}
