package team.creative.creativecore.common.gui.integration;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import team.creative.creativecore.common.gui.IScaleableGuiScreen;

public class GuiEventHandler {
    
    @Environment(EnvType.CLIENT)
    public static int defaultScale;
    @Environment(EnvType.CLIENT)
    public static boolean changed;
    
    @Environment(EnvType.CLIENT)
    private static Screen displayScreen;
    
    @Environment(EnvType.CLIENT)
    public static void queueScreen(Screen displayScreen) {
        GuiEventHandler.displayScreen = displayScreen;
    }
    
    @Environment(EnvType.CLIENT)
    public static void onTick(Minecraft mc) {
        if (displayScreen != null) {
            mc.setScreen(displayScreen);
            displayScreen = null;
        }
        if (mc.screen instanceof IScaleableGuiScreen) {
            IScaleableGuiScreen gui = (IScaleableGuiScreen) mc.screen;
            
            if (!changed)
                defaultScale = mc.options.guiScale().get();
            int maxScale = gui.getMaxScale(mc.getWindow().getWidth(), mc.getWindow().getHeight());
            int scale = Math.min(defaultScale, maxScale);
            if (defaultScale == 0)
                scale = maxScale;
            if (scale != mc.options.guiScale().get()) {
                changed = true;
                mc.options.guiScale().set(scale);
                mc.getWindow().setGuiScale(scale);
                mc.screen.resize(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
            }
        } else if (changed) {
            changed = false;
            mc.options.guiScale().set(defaultScale);
            mc.getWindow().setGuiScale(mc.getWindow().calculateScale(mc.options.guiScale().get(), mc.isEnforceUnicode()));
            if (mc.screen != null)
                mc.screen.resize(mc, mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
        }
    }
    
}
