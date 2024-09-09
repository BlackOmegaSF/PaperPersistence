package com.kleinercode.plugins.paperpersistence;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.*;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.view.AnvilView;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.units.qual.A;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

import static com.kleinercode.plugins.paperpersistence.Utils.*;

public class PaperPersistence extends JavaPlugin implements Listener {


    public void onEnable() {

        getServer().getPluginManager().registerEvents(this, this);

        // Check if KeepInventory is enabled, recommend disabling it
        getServer().getScheduler().scheduleSyncDelayedTask(this, () -> {
            List<String> keepInventoryWorlds = new ArrayList<>();
            for (World world : getServer().getWorlds()) {
                Boolean keepInventoryEnabled = world.getGameRuleValue(GameRule.KEEP_INVENTORY);
                if (keepInventoryEnabled != null && keepInventoryEnabled) {
                    keepInventoryWorlds.add(world.getName());
                }
            }

            if (keepInventoryWorlds.isEmpty()) {
                getLogger().log(Level.INFO, "KeepInventory is disabled, Persistence will work correctly.");
            } else if (keepInventoryWorlds.size() == 1) {
                final TextComponent message = Component.text("KeepInventory is enabled in world \"")
                        .color(NamedTextColor.YELLOW)
                        .append(Component.text(keepInventoryWorlds.getFirst(), NamedTextColor.BLUE))
                        .append(Component.text("\". For the Persistence plugin to work properly, this should be disabled."));
                getServer().broadcast(message, "OP");
                getLogger().log(Level.WARNING, PlainTextComponentSerializer.plainText().serialize(message));
            } else {
                final TextComponent message = Component.text("KeepInventory is enabled in multiple worlds. For the Persistence plugin to work properly, this should be disabled. See the server console for a list of worlds with KeepInventory enabled.")
                        .color(NamedTextColor.YELLOW);
                getServer().broadcast(message, "OP");
                StringBuilder listString = new StringBuilder();
                for (String world : keepInventoryWorlds) {
                    listString.append(world);
                    listString.append("; ");
                }
                getLogger().log(Level.WARNING, PlainTextComponentSerializer.plainText().serialize(message));
                getLogger().log(Level.INFO, listString.toString());
            }
        });

        // Create the Reinforced Emerald
        ItemStack reinforcedEmerald = getReinforcedEmerald();

        // Set up shapeless crafting recipe for Reinforced Emerald
        NamespacedKey recipeKey = new NamespacedKey(this, "reRecipe");
        ShapelessRecipe reRecipe = new ShapelessRecipe(recipeKey, reinforcedEmerald);
        reRecipe.addIngredient(1, Material.EMERALD);
        reRecipe.addIngredient(8, Material.IRON_INGOT);

        getServer().addRecipe(reRecipe);

        // Register the command
        Objects.requireNonNull(this.getCommand("makepersistent")).setExecutor(new CommandMakePersistent());
        Objects.requireNonNull(this.getCommand("makepersistent")).setTabCompleter(new TabCompletion());

    }

    @Override
    public void onDisable() {
        // TODO Figure out if we need to do anything here
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(final PlayerDeathEvent event) {
        final Player player = event.getEntity();

        final ItemStack[] inventory = player.getInventory().getContents();

        for(int i = 0; i < inventory.length; i++) { //Loop through inventory

            ItemStack is = inventory[i];
            if(is != null) {

                if(is.getItemMeta() instanceof BlockStateMeta im) { //Special case for shulker boxes
                    if(im.getBlockState() instanceof ShulkerBox shulker) {

                        for (ItemStack s : shulker.getInventory().getContents()) {
                            if(s != null ) {
                                if (s.getItemMeta().hasLore()) {
                                    List<Component> lore = s.getItemMeta().lore();
                                    assert lore != null;
                                    // Check each lore component
                                    boolean containsPersistence = false;
                                    for (Component component: lore) {
                                        if (PlainTextComponentSerializer.plainText().serialize(component).equals("Persistent")) {
                                            containsPersistence = true;
                                        }
                                    }
                                    if (!containsPersistence) {
                                        player.getWorld().dropItemNaturally(player.getLocation(), s);
                                        s.setAmount(0);
                                    } else {
                                        event.getDrops().remove(s);
                                    }
                                } else {
                                    player.getWorld().dropItemNaturally(player.getLocation(), s);
                                    s.setAmount(0);
                                }
                            }
                        }

                        im.setBlockState(shulker);
                        is.setItemMeta(im);

                    }
                }


                if (is.getItemMeta().hasLore()) {
                    List<Component> lore = is.getItemMeta().lore();
                    assert lore != null;
                    // Check each lore component
                    boolean containsPersistence = false;
                    for (Component component: lore) {
                        if (PlainTextComponentSerializer.plainText().serialize(component).equals("Persistent")) {
                            containsPersistence = true;
                        }
                    }
                    if (!containsPersistence) {
                        inventory[i] = null;
                    } else {
                        event.getDrops().remove(is);
                    }
                } else {
                    inventory[i] = null;
                }
            }
        }

        Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> player.getInventory().setContents(inventory));

    }


    @EventHandler
    public void onBlockDropItem(final BlockDropItemEvent event) {
        Material blockType = event.getBlockState().getType(); //Using getBlockState because getBlock will usually be air for this event type
        //Exit if item isn't shulker box or ender chest
        if (!Tag.SHULKER_BOXES.isTagged(blockType) && !(blockType.equals(Material.ENDER_CHEST))) {
            return;
        }

        //Loop through list of dropping items
        for (Item item : event.getItems()) {
            ItemStack itemStack = item.getItemStack();

            //Skip if item isn't shulker box or ender chest
            if (!Tag.SHULKER_BOXES.isTagged(itemStack.getType()) && !(itemStack.getType().equals(Material.ENDER_CHEST))) {
                continue;
            }

            //Add Persistence if item isn't already
            if (!Utils.checkIfPersistent(itemStack)) {
                ItemMeta meta = itemStack.getItemMeta();
                ArrayList<Component> lore;
                if (meta.hasLore()) {
                    lore = (ArrayList<Component>) meta.lore();
                } else {
                    lore = new ArrayList<>();
                }
                assert lore != null;
                lore.add(Component.text("Persistent"));
                meta.lore(lore);
                itemStack.setItemMeta(meta);
                item.setItemStack(itemStack);
            }
        }
    }

    @EventHandler
    public void prepareAnvilEvent(final PrepareAnvilEvent event) {

        AnvilInventory inventory = event.getInventory();
        ItemStack firstItemStack = inventory.getFirstItem();
        if (firstItemStack == null) return; // Not handling null event
        //if (firstItemStack.getAmount() > 1) return; // Not handling events with multiple input
        ItemStack secondItemStack = inventory.getSecondItem();
        if (checkIfReinforcedEmerald(secondItemStack)) {
            // Second item is a reinforced emerald, check if first is persistent
            if (checkIfPersistent(inventory.getFirstItem())) {
                // First item is persistent, set result to null
                event.setResult(null);
            } else {
                // First item is ready for Persistence, so add it
                ItemMeta meta = firstItemStack.getItemMeta();
                ArrayList<Component> lore;
                if (meta.hasLore()) {
                    lore = (ArrayList<Component>) meta.lore();
                } else {
                    lore = new ArrayList<>();
                }
                assert lore != null;
                lore.add(Component.text("Persistent"));
                meta.lore(lore);
                ItemStack resultStack = firstItemStack.clone();
                resultStack.setItemMeta(meta);
                event.setResult(resultStack);

                // Need to define repair cost to make recipe work
                AnvilView view = event.getView();
                view.setRepairCost(1);
                view.setRepairItemCountCost(firstItemStack.getAmount());
            }
        }

    }
}
