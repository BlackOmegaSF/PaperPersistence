package com.floogoobooq.blackomega.paperpersistence;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
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
import java.util.logging.Level;

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
                    String itemLore = PlainTextComponentSerializer.plainText().serialize(Objects.requireNonNull(is.getItemMeta().lore()).get(0));
                    String itemDisplayName = PlainTextComponentSerializer.plainText().serialize(Objects.requireNonNull(is.getItemMeta().displayName()));
                    if (itemLore.equals("Persistent") && itemDisplayName.equals("Reinforced Emerald")) {

                        //Grab data of target item
                        ItemStack targetItem;
                        if (mainHand) {
                            targetItem = player.getInventory().getItemInMainHand();
                        } else {
                            targetItem = player.getInventory().getItemInOffHand();
                        }
                        if (is.equals(targetItem)) { //Special case for trying to enchant a reinforced emerald
                            Component message = Component.text()
                                    .append(targetItem.displayName().hoverEvent(targetItem.asHoverEvent()))
                                    .append(Component.text(" is already persistent."))
                                    .build();
                            sender.sendMessage(message);
                            return true;
                        }
                        ItemStack bigStack = targetItem.clone();
                        boolean giveBig = false;

                        if (targetItem.getAmount() > 1) {
                            bigStack.setAmount((targetItem.getAmount() - 1));
                            targetItem.setAmount(1);
                            giveBig = true;
                        }

                        //Add Persistence to specified item
                        ItemMeta targetItemMeta = targetItem.getItemMeta();
                        String handString;
                        if (mainHand) {
                            handString = "main hand";
                        } else {
                            handString = "offhand";
                        }
                        if (targetItemMeta == null) {
                            sender.sendMessage("Nothing in " + handString + "...");
                            return true;
                        }

                        if (targetItemMeta.hasLore()) {
                            List<Component> lore = targetItemMeta.lore();
                            // Check each lore component
                            boolean containsPersistence = false;
                            assert lore != null;
                            for (Component component: lore) {
                                if (PlainTextComponentSerializer.plainText().serialize(component).equals("Persistent")) {
                                    containsPersistence = true;
                                }
                            }
                            if (containsPersistence) {
                                TextComponent message = Component.text()
                                        .append(targetItem.displayName().hoverEvent(targetItem.asHoverEvent()))
                                        .append(Component.text(" is already persistent."))
                                        .build();
                                sender.sendMessage(message);
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
                        TextComponent successMessage = Component.text()
                                .append(targetItem.displayName().hoverEvent(targetItem.asHoverEvent()))
                                .append(Component.text(" is now persistent!"))
                                .build();
                        sender.sendMessage(successMessage);
                        if (giveBig) {
                            player.getWorld().dropItemNaturally(player.getLocation(), bigStack);
                        }
                        //Take away one Reinforced Emerald
                        if (is.getAmount() == 1) {
                            player.getInventory().setItem(i,  new ItemStack(Material.AIR));
                        } else if (is.getAmount() > 1) {
                            ItemStack remainder = is.clone();
                            remainder.setAmount(is.getAmount() - 1);
                            player.getInventory().setItem(i, remainder);
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

