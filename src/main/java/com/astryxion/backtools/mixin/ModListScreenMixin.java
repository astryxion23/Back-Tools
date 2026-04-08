package com.astryxion.backtools.mixin;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ModListScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/** NeoForge 21.9.16-beta: {@code ModListScreen} sorts a list from {@code Stream.toList()} (immutable). */
@Mixin(ModListScreen.class)
public class ModListScreenMixin
{
    @Shadow
    private List<ModContainer> mods;

    @Inject(method = "reloadMods", at = @At("TAIL"))
    private void backtools$mutableFilteredMods(CallbackInfo ci)
    {
        this.mods = new ArrayList<>(this.mods);
    }
}
