package com.floogoobooq.blackomega.paperpersistence;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

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

            if (keepInventoryWorlds.size() == 0) {
                getLogger().log(Level.INFO, "KeepInventory is disabled, Persistence will work correctly.");
            } else if (keepInventoryWorlds.size() == 1) {
                final TextComponent message = Component.text("KeepInventory is enabled in world \"")
                        .color(NamedTextColor.YELLOW)
                        .append(Component.text(keepInventoryWorlds.get(0), NamedTextColor.BLUE))
                        .append(Component.text("\". For the Persistence plugin to work properly, this should be disabled."));
                getServer().broadcast(message, "OP");
                getLogger().log(Level.WARNING, "KeepInventory is enabled in world \"" + keepInventoryWorlds.get(0) + "\". For the Persistence plugin to work properly, this should be disabled.");
            } else {
                final TextComponent message = Component.text("KeepInventory is enabled in multiple worlds. For the Persistence plugin to work properly, this should be disabled. See the server console for a list of worlds with KeepInventory enabled.")
                        .color(NamedTextColor.YELLOW);
                getServer().broadcast(message, "OP");
                StringBuilder listString = new StringBuilder();
                for (String world : keepInventoryWorlds) {
                    listString.append(world);
                    listString.append("; ");
                }
                getLogger().log(Level.WARNING, "KeepInventory is enabled in multiple worlds. For the Persistence plugin to work properly, this should be disabled. See the server console for a list of worlds with KeepInventory enabled.");
                getLogger().log(Level.INFO, listString.toString());
            }
        });

        // Create the Reinforced Emerald
        ItemStack reinforcedEmerald = new ItemStack(Material.EMERALD);
        ItemMeta reMeta = reinforcedEmerald.getItemMeta();
        reMeta.displayName(Component.text("Reinforced Emerald"));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Persistent"));
        reMeta.lore(lore);
        reinforcedEmerald.setItemMeta(reMeta);

        // Set up shapeless crafting recipe for Reinforced Emerald
        NamespacedKey recipeKey = new NamespacedKey(this, "reRecipe");
        ShapelessRecipe reRecipe = new ShapelessRecipe(recipeKey, reinforcedEmerald);
        reRecipe.addIngredient(1, Material.EMERALD);
        reRecipe.addIngredient(8, Material.IRON_INGOT);

        getServer().addRecipe(reRecipe);

        // Register the command
        this.getCommand("makepersistent").setExecutor(new CommandMakePersistent());
        this.getCommand("makepersistent").setTabCompleter(new TabCompletion());

    }

    @Override
    public void onDisable() {

    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(final PlayerDeathEvent event) {
        final Player player = event.getEntity();

        final ItemStack[] inventory = player.getInventory().getContents();

        for(int i = 0; i < inventory.length; i++) { //Loop through inventory

            ItemStack is = inventory[i];
            if(is != null) {

                if(is.getItemMeta() instanceof BlockStateMeta) { //Special case for shulker boxes
                    BlockStateMeta im = (BlockStateMeta)is.getItemMeta();
                    if(im.getBlockState() instanceof ShulkerBox) {
                        ShulkerBox shulker = (ShulkerBox) im.getBlockState();

                        for (ItemStack s : shulker.getInventory().getContents()) {
                            if(s != null ) {
                                if (s.getItemMeta().hasLore()) {
                                    List<Component> lore = s.getItemMeta().lore();
                                    assert lore != null;
                                    // Check each lore component
                                    boolean containsPersistence = false;
                                    for (Component component: lore) {
                                        if (component.examinableName().equals("Persistent")) {
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
                        if (component.examinableName().equals("Persistent")) {
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
    public void onBlockBreak(final BlockBreakEvent event) {
        Block blockBroken = event.getBlock();
        if(Tag.SHULKER_BOXES.isTagged(blockBroken.getType())) {
            Collection<ItemStack> isCollection = blockBroken.getDrops();
            Collection<ItemStack> newCollection = new ArrayList<>();
            for (ItemStack item : isCollection) {
                ItemMeta meta = item.getItemMeta();
                if (meta.hasLore()) {
                    List<Component> lore = item.getItemMeta().lore();
                    assert lore != null;
                    // Check each lore component
                    boolean containsPersistence = false;
                    for (Component component: lore) {
                        if (component.examinableName().equals("Persistent")) {
                            containsPersistence = true;
                        }
                    }
                    if (!containsPersistence) {
                        Component persistenceText = Component.text("Persistent");
                        lore.add(persistenceText);
                        meta.lore(lore);
                    }

                } else {
                    ArrayList<Component> lore = new ArrayList<>();
                    lore.add(Component.text("Persistent"));
                    meta.lore(lore);
                }

                item.setItemMeta(meta);
                newCollection.add(item);
            }

            blockBroken.setType(Material.AIR);
            for (ItemStack itemStack : newCollection) {
                blockBroken.getWorld().dropItemNaturally(blockBroken.getLocation(), itemStack);
            }
        }
    }


}
