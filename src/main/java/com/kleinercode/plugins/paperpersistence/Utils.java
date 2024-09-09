package com.kleinercode.plugins.paperpersistence;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Utils {

    public static ItemStack getReinforcedEmerald() {
        ItemStack reinforcedEmerald = new ItemStack(Material.EMERALD);
        ItemMeta reMeta = reinforcedEmerald.getItemMeta();
        reMeta.displayName(Component.text("Reinforced Emerald"));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Persistent"));
        reMeta.lore(lore);
        reinforcedEmerald.setItemMeta(reMeta);
        return reinforcedEmerald;
    }

    public static Boolean checkIfReinforcedEmerald(ItemStack itemStack) {
        if (itemStack == null) return false;
        if (itemStack.getType() != Material.EMERALD) return false;
        if (!itemStack.getItemMeta().hasDisplayName()) return false;
        if (!itemStack.getItemMeta().hasLore()) return false;
        String itemDisplayName = PlainTextComponentSerializer.plainText().serialize(Objects.requireNonNull(itemStack.getItemMeta().displayName()));
        return checkIfPersistent(itemStack) && itemDisplayName.equals("Reinforced Emerald");
    }

    public static Boolean checkIfPersistent(ItemStack itemStack) {
        if (itemStack == null) return false;
        if (!itemStack.getItemMeta().hasLore()) return false;
        List<Component> loreComponents = Objects.requireNonNull(itemStack.getItemMeta().lore());
        for (Component loreComponent : loreComponents) {
            if (PlainTextComponentSerializer.plainText().serialize(loreComponent).equals("Persistent")) return true;
        }
        return false;
    }

}
