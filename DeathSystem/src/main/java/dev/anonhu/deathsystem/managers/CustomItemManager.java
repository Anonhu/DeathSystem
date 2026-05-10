package dev.anonhu.deathsystem.managers;

import dev.anonhu.deathsystem.DeathSystemPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * Создаёт кастомные предметы (бинт, дефибриллятор)
 * и регистрирует их крафты.
 *
 * Идентификация через PersistentDataContainer —
 * не зависит от CustomModelData и названия предмета.
 */
public class CustomItemManager {

    // Ключи PersistentData
    private final NamespacedKey keyBandage;           // маркер бинта
    private final NamespacedKey keyDefib;             // маркер дефибриллятора
    private final NamespacedKey keyDefibCharges;      // заряды дефибриллятора

    private final DeathSystemPlugin plugin;

    public CustomItemManager(DeathSystemPlugin plugin) {
        this.plugin         = plugin;
        this.keyBandage     = new NamespacedKey(plugin, "bandage");
        this.keyDefib       = new NamespacedKey(plugin, "defibrillator");
        this.keyDefibCharges = new NamespacedKey(plugin, "defib_charges");
        registerRecipes();
    }

    // =========================================================
    //  Проверки
    // =========================================================

    public boolean isBandage(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
            .has(keyBandage, PersistentDataType.BYTE);
    }

    public boolean isDefibrillator(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
            .has(keyDefib, PersistentDataType.BYTE);
    }

    public boolean hasCharge(ItemStack item) {
        if (!isDefibrillator(item)) return false;
        int charges = getCharges(item);
        return charges > 0;
    }

    public int getCharges(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        return pdc.getOrDefault(keyDefibCharges, PersistentDataType.INTEGER, 0);
    }

    // =========================================================
    //  Изменение заряда дефибриллятора
    // =========================================================

    /**
     * Уменьшает заряд дефибриллятора в руке игрока на 1.
     * Если заряд = 0 — удаляет предмет.
     */
    public void decrementDefibrillatorCharge(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (!isDefibrillator(item)) return;

        ItemMeta meta   = item.getItemMeta();
        int charges     = meta.getPersistentDataContainer()
            .getOrDefault(keyDefibCharges, PersistentDataType.INTEGER, 0);
        int newCharges  = charges - 1;

        if (newCharges <= 0) {
            // Заряды кончились — убираем предмет
            item.setAmount(0);
            player.sendMessage("\u00a77Дефибриллятор иссяк.");
            return;
        }

        meta.getPersistentDataContainer()
            .set(keyDefibCharges, PersistentDataType.INTEGER, newCharges);
        updateDefibLore(meta, newCharges);
        item.setItemMeta(meta);
        player.sendMessage("\u00a7eДефибриллятор: остало\u00a7f " + newCharges + " \u00a7eзаряд(a).");
    }

    // =========================================================
    //  Фабрика предметов
    // =========================================================

    /** Создаёт ItemStack бинта. */
    public ItemStack createBandage() {
        ItemStack item = new ItemStack(Material.STRING);
        ItemMeta  meta = item.getItemMeta();

        meta.displayName(Component.text("Бинт", NamedTextColor.WHITE));
        meta.lore(List.of(
            Component.text("Поднимает с 5% HP (а не стоито даже этого, ня~)", NamedTextColor.GRAY),
            Component.text("Правая кнопка по упавшему", NamedTextColor.DARK_GRAY)
        ));
        meta.setCustomModelData(plugin.getPluginConfig().bandageModelData);
        meta.getPersistentDataContainer().set(keyBandage, PersistentDataType.BYTE, (byte) 1);

        item.setItemMeta(meta);
        return item;
    }

    /** Создаёт ItemStack дефибриллятора с полным зарядом. */
    public ItemStack createDefibrillator() {
        int maxCharges = plugin.getPluginConfig().defibrillatorMaxUses;
        ItemStack item = new ItemStack(Material.CLOCK);
        ItemMeta  meta = item.getItemMeta();

        meta.displayName(Component.text("Дефибриллятор", NamedTextColor.GOLD));
        updateDefibLore(meta, maxCharges);
        meta.setCustomModelData(plugin.getPluginConfig().defibrillatorModelData);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(keyDefib,        PersistentDataType.BYTE,    (byte) 1);
        pdc.set(keyDefibCharges, PersistentDataType.INTEGER, maxCharges);

        item.setItemMeta(meta);
        return item;
    }

    private void updateDefibLore(ItemMeta meta, int charges) {
        meta.lore(List.of(
            Component.text("Мгновенный подъём с 30% HP", NamedTextColor.YELLOW),
            Component.text("Заряды: " + charges, NamedTextColor.AQUA),
            Component.text("Правая кнопка по упавшему", NamedTextColor.DARK_GRAY)
        ));
    }

    // =========================================================
    //  Рецепты
    // =========================================================

    private void registerRecipes() {
        registerBandageRecipe();
        registerDefibrillatorRecipe();
    }

    private void registerBandageRecipe() {
        ItemStack result = createBandage();
        result.setAmount(3);

        NamespacedKey key = new NamespacedKey(plugin, "bandage_recipe");
        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape("WWW", "   ", "   ");
        recipe.setIngredient('W', Material.WHITE_WOOL);

        plugin.getServer().addRecipe(recipe);
    }

    private void registerDefibrillatorRecipe() {
        ItemStack result = createDefibrillator();

        NamespacedKey key = new NamespacedKey(plugin, "defibrillator_recipe");
        ShapedRecipe recipe = new ShapedRecipe(key, result);
        recipe.shape("GRG", "INI", "GRG");
        recipe.setIngredient('G', Material.GOLD_INGOT);
        recipe.setIngredient('R', Material.REDSTONE_BLOCK);
        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('N', Material.NETHER_STAR);

        plugin.getServer().addRecipe(recipe);
    }
}
