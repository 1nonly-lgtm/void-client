package dev.oneonly.voidclient.yarn;

import net.minecraft.inventory.Inventories;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.collection.DefaultedList;

import java.util.Collections;
import java.util.List;

/** Zawartość shulkera dla 1.20.1 – 1.20.4 (dane w NBT). */
final class Contents {
    private Contents() {}

    static List<ItemStack> of(ItemStack stack) {
        NbtCompound tag = BlockItem.getBlockEntityNbt(stack);
        if (tag == null || !tag.contains("Items", 9)) return Collections.emptyList();
        DefaultedList<ItemStack> list = DefaultedList.ofSize(27, ItemStack.EMPTY);
        Inventories.readNbt(tag, list);
        return list;
    }
}
