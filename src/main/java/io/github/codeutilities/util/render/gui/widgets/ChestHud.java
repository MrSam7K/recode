package io.github.codeutilities.util.render.gui.widgets;

import com.google.common.collect.Lists;
import io.github.codeutilities.CodeUtilities;
import io.github.codeutilities.config.CodeUtilsConfig;
import io.github.codeutilities.util.networking.DFInfo;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.lwjgl.opengl.GL11;

import java.util.List;

public class ChestHud {
    public static void register() {
        ScreenEvents.AFTER_INIT.register(ChestHud::afterInit);
    }

    private static void afterInit(MinecraftClient client, Screen screen, int windowWidth, int windowHeight) {
        if (screen instanceof GenericContainerScreen) {
            ScreenEvents.afterRender(screen).register(ChestHud::afterContainerRender);
        }
    }


    private static void afterContainerRender(Screen screen, MatrixStack matrices, int mouseX, int mouseY, float tickDelta) {

        if(DFInfo.currentState == DFInfo.State.DEV && CodeUtilsConfig.getBoolean("chestToolTip")) {
            ItemStack item = CodeUtilities.MC.player.inventory.getStack(17);

            int i = ((screen.width) / 2) + 85;
            int j = (screen.height) / 2 - 68;

            // check if block in dev area later.
            if(CodeUtilities.MC.getWindow().getScaledWidth() >= 600) {
                List<Text> lines = item.getTooltip(CodeUtilities.MC.player, TooltipContext.Default.NORMAL);
                GL11.glTranslatef(0f, 0f, -1f);
                screen.renderOrderedTooltip(matrices, Lists.transform(lines, Text::asOrderedText), i, j);
                GL11.glTranslatef(0f, 0f, 1f);
            }
        }


    }
}