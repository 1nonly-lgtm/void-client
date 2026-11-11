package dev.oneonly.voidclient.yarn;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

import java.util.Collections;
import java.util.List;

/** Zawartość shulkera dla 1.20.5+ (komponenty przedmiotów). */
final class Contents {
    private Contents() {}

    static List<ItemStack> of(ItemStack stack) {
        ContainerComponent c = stack.get(DataComponentTypes.CONTAINER);
        if (c == null) return Collections.emptyList();
        DefaultedList<ItemStack> list = DefaultedList.ofSize(27, ItemStack.EMPTY);
        c.copyTo(list);
        return list;
    }
}
