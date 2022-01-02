package com.floogoobooq.blackomega.paperpersistence;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CommandMakePersistent implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        //Check args length
        if (args.length < 2) {
            sender.sendMessage("Missing arguments");
            return false;
        }

        Player player = sender.getServer().getPlayerExact(args[0]);
        if (player == null) {
            sender.sendMessage("Player not found");
            return false;
        }

        boolean mainHand;
        if (args[1].equalsIgnoreCase("main")) {
            mainHand = true;
        } else if (args[1].equalsIgnoreCase("offhand")) {
            mainHand = false;
        } else {
            sender.sendMessage("Invalid hand parameter: must be main or offhand");
            return false;
        }

        ItemStack[] inventory = player.getInventory().getContents();

        for (int i = 0; i < inventory.length; i++) {
            ItemStack is = inventory[i];
            if (is != null) { // Skip if null to avoid NullPointerException
                if (is.getType() ==  Material.EMERALD && is.getItemMeta().hasDisplayName() && is.getItemMeta().hasLore()) {
                    if (Objects.requireNonNull(is.getItemMeta().lore()).get(0).examinableName().equals("Persistent")
                            && Objects.requireNonNull(is.getItemMeta().displayName()).examinableName().equals("Reinforced Emerald")) {

                        //Grab data of target item
                        ItemStack targetItem = null;
                        if (mainHand) {
                            targetItem = player.getInventory().getItemInMainHand();
                        } else {
                            targetItem = player.getInventory().getItemInOffHand();
                        }
                        ItemStack bigStack = targetItem.clone();
                        ItemStack bigEmerald = is.clone();
                        boolean giveBig = false;

                        if (targetItem.getAmount() > 1) {
                            bigStack.setAmount((targetItem.getAmount() - 1));
                            targetItem.setAmount(1);
                            giveBig = true;
                        }

                        //Add Persistence to OffHand item
                        ItemMeta targetItemMeta = targetItem.getItemMeta();
                        String handString;
                        if (mainHand) {
                            handString = "main hand";
                        } else {
                            handString = "offhand";
                        }
                        if (targetItemMeta == null) {
                            sender.sendMessage("Nothing in " + handString + " to enchant");
                            return true;
                        }

                        if (targetItemMeta.hasLore()) {
                            List<Component> lore = targetItemMeta.lore();
                            // Check each lore component
                            boolean containsPersistence = false;
                            assert lore != null;
                            for (Component component: lore) {
                                if (component.examinableName().equals("Persistent")) {
                                    containsPersistence = true;
                                }
                            }
                            if (containsPersistence) {
                                sender.sendMessage("This item is already persistent.");
                                return true;
                            }

                        } else {
                            ArrayList<Component> lore = new ArrayList<>();
                            lore.add(Component.text("Persistent"));
                            targetItemMeta.lore(lore);
                        }

                        targetItem.setItemMeta(targetItemMeta);

                        if (mainHand) {
                            player.getInventory().setItemInMainHand(targetItem);
                        } else {
                            player.getInventory().setItemInOffHand(targetItem);
                        }
                        sender.sendMessage("Enchanted!");
                        if (giveBig) {
                            player.getWorld().dropItemNaturally(player.getLocation(), bigStack);
                        }
                        //Take away the Reinforced Emerald
                        player.getInventory().setItem(i,  new ItemStack(Material.AIR));
                        boolean giveBigEmerald = false;

                        if (is.getAmount() > 1) {
                            bigEmerald.setAmount((is.getAmount() - 1));
                            giveBigEmerald = true;
                        }

                        if (giveBigEmerald) {
                            player.getWorld().dropItemNaturally(player.getLocation(), bigEmerald);
                        }

                        return true;


                    }

                }
            }
        }

        //This only happens if no emerald is found, other cases return before this
        sender.sendMessage("No Reinforced Emerald found, could not enchant.");

        return true;
    }

}

