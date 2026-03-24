package it.hurts.sskirillss.yagm.client.utils;

import com.mojang.blaze3d.platform.NativeImage;
import lombok.extern.slf4j.Slf4j;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class EmissiveTextureManager {

    private static final Map<ResourceLocation, ResourceLocation> EMISSIVE_CACHE = new HashMap<>();

    private static final int BRIGHTNESS_THRESHOLD = 80;

    public static ResourceLocation getOrCreate(ResourceLocation originalTexture) {
        return EMISSIVE_CACHE.computeIfAbsent(originalTexture, EmissiveTextureManager::generate);
    }

    private static ResourceLocation generate(ResourceLocation original) {
        try {
            Minecraft mc = Minecraft.getInstance();

            Optional<Resource> resourceOpt = mc.getResourceManager().getResource(original);
            if (resourceOpt.isEmpty()) {
                return original;
            }

            NativeImage originalImage;
            try (InputStream stream = resourceOpt.get().open()) {
                originalImage = NativeImage.read(stream);
            }

            int width = originalImage.getWidth();
            int height = originalImage.getHeight();
            NativeImage emissiveImage = new NativeImage(width, height, true);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    int pixel = originalImage.getPixelRGBA(x, y);

                    int alpha  = (pixel >> 24) & 0xFF;
                    int blue   = (pixel >> 16) & 0xFF;
                    int green  = (pixel >> 8)  & 0xFF;
                    int red    =  pixel        & 0xFF;

                    int brightness = (red + green + blue) / 3;

                    emissiveImage.setPixelRGBA(x, y,
                            alpha > 0 && brightness > BRIGHTNESS_THRESHOLD ? pixel : 0x00000000);
                }
            }

            ResourceLocation emissiveLocation = ResourceLocation.fromNamespaceAndPath(
                    original.getNamespace(),
                    "dynamic/emissive/" + original.getPath().replace("/", "_").replace(".png", "")
            );

            mc.getTextureManager().register(emissiveLocation, new DynamicTexture(emissiveImage));
            originalImage.close();

            return emissiveLocation;

        } catch (IOException e) {
            log.error("[YAGM] Failed to generate emissive texture for {}: {}", original, e.getMessage(), e);
            return original;
        }
    }

    public static void clearCache() {
        EMISSIVE_CACHE.clear();
    }
}
