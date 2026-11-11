package dev.oneonly.voidclient.yarn.mixin;

import dev.oneonly.voidclient.core.ChatTweaks;
import dev.oneonly.voidclient.yarn.ChatLines;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Czat: godzina, łączenie powtórzeń (×N) i dłuższa historia. */
@Mixin(ChatHud.class)
public abstract class ChatMixin {
    @ModifyVariable(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private Text voidclient$tweak(Text message) {
        try {
            int n = ChatTweaks.onMessage(message.getString());
            Text out = message;
            if (n > 1 && ChatLines.removeNewest(this)) {
                out = message.copy().append(Text.literal(" ×" + n).formatted(Formatting.GRAY));
            }
            if (ChatTweaks.timestamps()) {
                MutableText t = Text.literal(ChatTweaks.timestamp()).formatted(Formatting.DARK_GRAY);
                out = t.append(out);
            }
            return out;
        } catch (Throwable t) {
            return message;
        }
    }

    @ModifyConstant(method = {
            "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;ILnet/minecraft/client/gui/hud/MessageIndicator;Z)V", // 1.20.1
            "addMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V",                 // 1.20.2+
            "addVisibleMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V"},, constant = @Constant(intValue = 100), require = 0)
    private int voidclient$history(int vanilla) {
        return ChatTweaks.historyLimit(vanilla);
    }
}
