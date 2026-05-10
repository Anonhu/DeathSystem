package dev.yourname.deathsystem.managers;

import dev.yourname.deathsystem.DeathSystemPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.List;

public class CustomItemManager {

    private final DeathSystemPlugin plugin;

    public CustomItemManager(DeathSystemPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerRecipes() {
        registerBandageRecipe();
        registerDefibrillatorRecipe();
    }

    public ItemStack createBandage() {
        ItemStack item = new ItemStack(Material.WHITE_WOOL);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("§fБинт"));
        meta.lore(List.of(
            Component.text("§7Поднимает раненого с §c5% HP"),
            Component.text("§7Держите и нажмите ПКМ на лежащего"),
            Component.text("§8Время подъёма: 3 сек")
        ));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        var pdc = meta.getPersistentDataContainer();
        pdc.set(new NamespacedKey(plugin, "item_type"),
            PersistentDataType.STRING, "bandage");

        meta.setCustomModelData(plugin.getPluginConfig().bandageModelData);

        item.setItemMeta(meta);
        return item;
    }

    private void registerBandageRecipe() {
        NamespacedKey key = new NamespacedKey(plugin, "bandage");
        ShapedRecipe recipe = new ShapedRecipe(key, createBandage());
        recipe.shape("WWW", "   ", "   ");
        recipe.setIngredient('W', Material.WHITE_WOOL);
        recipe.setAmount(3);

        plugin.getServer().addRecipe(recipe);
    }

    public ItemStack createDefibrillator() {
        ItemStack item = new ItemStack(Material.CLOCK);
        updateDefibrillatorLore(item, 0);
        return item;
    }

    public void updateDefibrillatorLore(ItemStack item, int currentUses) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        int maxUses = plugin.getPluginConfig().defibrillatorMaxUses;
        int remaining = maxUses - currentUses;

        meta.displayName(Component.text("§e⚡ Дефибриллятор"));
        meta.lore(Arrays.asList(
            Component.text("§7Мгновенно поднимает раненого с §a30% HP"),
            Component.text("§7Применяется без задержки"),
            Component.text(""),
            Component.text("§8Зарядов: §e" + remaining + " §8/ §e" + maxUses),
            Component.text("§8Очень редкий предмет")
        ));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);

        var pdc = meta.getPersistentDataContainer();
        pdc.set(new NamespacedKey(plugin, "item_type"),
            PersistentDataType.STRING, "defibrillator");
        pdc.set(new NamespacedKey(plugin, "use_count"),
            PersistentDataType.INTEGER, currentUses);

        meta.setCustomModelData(plugin.getPluginConfig().defibrillatorModelData);
        item.setItemMeta(meta);
    }

    private void registerDefibrillatorRecipe() {
        NamespacedKey key = new NamespacedKey(plugin, "defibrillator");
        ShapedRecipe recipe = new ShapedRecipe(key, createDefibrillator());

        recipe.shape("GRG", "INI", "GRG");
        recipe.setIngredient('G', Material.GOLD_INGOT);
        recipe.setIngredient('R', Material.REDSTONE_BLOCK);
        recipe.setIngredient('I', Material.IRON_BLOCK);
        recipe.setIngredient('N', Material.NETHER_STAR);

        plugin.getServer().addRecipe(recipe);
    }
}
