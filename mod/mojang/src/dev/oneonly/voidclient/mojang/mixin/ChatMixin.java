package dev.oneonly.voidclient.mojang.mixin;

import dev.oneonly.voidclient.core.ChatTweaks;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.lang.reflect.Field;
import java.util.List;

/** Czat: godzina, łączenie powtórzeń (×N) i dłuższa historia (26.1+). */
@Mixin(ChatComponent.class)
public abstract class ChatMixin {
    @ModifyVariable(method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/GuiMessageTag;)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private Component voidclient$tweak(Component message) {
        try {
            int n = ChatTweaks.onMessage(message.getString());
            Component out = message;
            if (n > 1 && removeNewest(this)) out = message.copy().append(Component.literal(" ×" + n).withStyle(ChatFormatting.GRAY));
            if (ChatTweaks.timestamps()) out = Component.literal(ChatTweaks.timestamp()).withStyle(ChatFormatting.DARK_GRAY).append(out);
            return out;
        } catch (Throwable t) {
            return message;
        }
    }

    @ModifyConstant(method = {"addMessageToQueue", "addMessageToDisplayQueue"}, constant = @Constant(intValue = 100), require = 0)
    private int voidclient$history(int vanilla) {
        return ChatTweaks.historyLimit(vanilla);
    }

    @SuppressWarnings("unchecked")
    private static boolean removeNewest(Object chat) {
        try {
            List<Object> all = (List<Object>) field(chat, "allMessages").get(chat);
            List<Object> vis = (List<Object>) field(chat, "trimmedMessages").get(chat);
            if (all.isEmpty() || vis.isEmpty()) return false;
            all.remove(0);
            vis.remove(0);
            java.lang.reflect.Method end = vis.isEmpty() ? null : vis.get(0).getClass().getMethod("endOfEntry");
            while (!vis.isEmpty() && !(Boolean) end.invoke(vis.get(0))) vis.remove(0);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static Field field(Object o, String name) throws NoSuchFieldException {
        Field f = ChatComponent.class.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }
}
