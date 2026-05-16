package me.kwilson272.elementalmagic.core.ability;

import me.kwilson272.elementalmagic.api.ability.Element;

import net.md_5.bungee.api.ChatColor;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class CoreElement implements Element {

    public static final Element AIR = new CoreElement("Air", ChatColor.GRAY, List.of("A"), Set.of(), ParentMode.NONE);
    public static final Element SOUND = new CoreElement("Sound", ChatColor.DARK_GRAY, List.of(), Set.of(AIR), ParentMode.ALL);
    public static final Element FLIGHT = new CoreElement("Flight", ChatColor.DARK_GRAY, List.of(), Set.of(AIR), ParentMode.ALL);

    public static final Element AVATAR = new CoreElement("Avatar", ChatColor.DARK_PURPLE, List.of("Av"), Set.of(), ParentMode.NONE);

    public static final Element CHI = new CoreElement("Chi", ChatColor.GOLD, List.of("Ch", "C"), Set.of(), ParentMode.NONE);
    public static final Element WEAPONRY = new CoreElement("Weaponry", ChatColor.DARK_GRAY, List.of(), Set.of(CHI), ParentMode.ALL);

    public static final Element EARTH = new CoreElement("Earth", ChatColor.GREEN, List.of("E"), Set.of(), ParentMode.NONE);
    public static final Element SAND = new CoreElement("Sand", ChatColor.DARK_GREEN, List.of(), Set.of(EARTH), ParentMode.ALL);
    public static final Element MUD = new CoreElement("Mud", ChatColor.DARK_GREEN, List.of(), Set.of(EARTH), ParentMode.ANY);
    public static final Element METAL = new CoreElement("Metal", ChatColor.DARK_GREEN, List.of(), Set.of(EARTH), ParentMode.ALL);
    public static final Element LAVA = new CoreElement("Lava", ChatColor.RED, List.of(), Set.of(EARTH), ParentMode.ALL);

    public static final Element FIRE = new CoreElement("Fire", ChatColor.RED, List.of("F"), Set.of(), ParentMode.NONE);
    public static final Element LIGHTNING = new CoreElement("Lightning", ChatColor.AQUA, List.of(), Set.of(FIRE), ParentMode.ALL);
    public static final Element COMBUSTION = new CoreElement("Combustion", ChatColor.DARK_RED, List.of(), Set.of(FIRE), ParentMode.ALL);

    public static final Element WATER = new CoreElement("Water", ChatColor.AQUA, List.of("W"), Set.of(), ParentMode.NONE);
    public static final Element ICE = new CoreElement("Ice", ChatColor.DARK_AQUA, List.of(), Set.of(WATER), ParentMode.ALL);
    public static final Element PLANT = new CoreElement("Plant", ChatColor.DARK_GREEN, List.of(), Set.of(WATER), ParentMode.ALL);
    public static final Element HEALING = new CoreElement("Healing", ChatColor.DARK_AQUA, List.of(), Set.of(WATER), ParentMode.ALL);
    public static final Element BLOOD = new CoreElement("Blood", ChatColor.DARK_RED, List.of(), Set.of(WATER), ParentMode.ALL);

    private final String name;
    private final ChatColor displayColor;
    private final List<String> aliases;
    private final Set<Element> parents;
    private final ParentMode parentMode;

    CoreElement(String name, ChatColor displayColor, List<String> aliases,
                Set<Element> parents, ParentMode parentResolutionMode) {
        this.name = name;
        this.displayColor = displayColor;
        this.aliases = aliases;
        this.parents = parents;
        this.parentMode = parentResolutionMode;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public ChatColor color() {
        return displayColor;
    }

    @Override
    public Collection<String> aliases() {
        return List.copyOf(aliases);
    }

    @Override
    public Set<Element> parents() {
        return Set.copyOf(parents);
    }

    @Override
    public ParentMode parentMode() {
        return parentMode;
    }

    @Override
    public String permission() {
        return  "elementalmagic." + name.toLowerCase();
    }
}
