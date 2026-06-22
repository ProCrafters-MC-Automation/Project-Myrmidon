package org.pmca.project_myrmidon.client.mixin.client;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ChatInterceptMixin {

    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void onSendChatMessage(String content, CallbackInfo ci) {
        // Check if the message starts with our prefix
        if (content.startsWith("@")) {
            // Print the intercepted message to the system console
            System.out.println("[Chat Intercept] " + content);

            /* * Optional: If you also want to print it to the player's
             * in-game chat HUD so they know it worked, you can use:
             * MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("Intercepted: " + content));
             */

            // Cancel the mixin callback to prevent the packet from sending to the server
            ci.cancel();
        }
    }
}