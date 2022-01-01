package com.floogoobooq.blackomega.paperpersistence;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public class CommandMakePersistent implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            ItemStack[] inventory = player.getInventory().getContents();

            boolean error = false;
            outerloop: // Label to break out to
            for (int i = 0; i < inventory.length; i++) {
                ItemStack is = inventory[i];
                if (is != null) { // Skip if null to avoid NullPointerException
                    if (is.getType() ==  Material.EMERALD && is.getItemMeta().getDisplayName().equals("Reinforced Emerald") && is.getItemMeta().hasLore()) {
                        if (is.getItemMeta().getLore().get(0).equals("Persistent")) {


                            //Grab data of OffHand item
                            ItemStack ohItem = player.getInventory().getItemInOffHand();
                            ItemStack bigStack = ohItem.clone();
                            ItemStack bigEmerald = is.clone();
                            boolean giveBig = false;

                            if (ohItem.getAmount() > 1) {
                                bigStack.setAmount((ohItem.getAmount() - 1));
                                ohItem.setAmount(1);
                                giveBig = true;
                            }

                            //Add Persistence to OffHand item
                            ItemMeta ohItemMeta = ohItem.getItemMeta();

                            if (ohItemMeta == null) {
                                sender.sendMessage("Nothing in offhand to enchant");
                                error = true;
                                break outerloop;
                            }

                            if (ohItemMeta.hasLore()) {
                                List<String> lore = ohItemMeta.getLore();
                                if (lore.contains("Persistent")) {
                                    sender.sendMessage("This item is already persistent.");
                                    error = true;
                                    break;
                                }
                                // lore.add("Persistent"); //Not used because it's bypassed with the above error
                                // ohItemMeta.setLore(lore);
                            } else {
                                ArrayList<String> lore = new ArrayList<>();
                                lore.add("Persistent");
                                ohItemMeta.setLore(lore);
                            }

                            ohItem.setItemMeta(ohItemMeta);

                            player.getInventory().setItemInOffHand(ohItem);
                            sender.sendMessage("Enchanted!");
                            error = true; //Using error as a flag to stop the Emerald not found message
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

                            break;


                        }

                    }
                }
            }

            if (!error) {
                sender.sendMessage("No Reinforced Emerald found, could not enchant.");
            }
            return true;

        } else {
            sender.sendMessage("Only players should use this command");
            return false;
        }



    }

}
