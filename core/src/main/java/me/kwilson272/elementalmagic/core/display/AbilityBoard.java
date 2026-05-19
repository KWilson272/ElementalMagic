package me.kwilson272.elementalmagic.core.display;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class AbilityBoard {
   
    private static final String PREFIX_UNSELECTED = ChatColor.DARK_GRAY + "> ";
    private static final String PREFIX_SELECTED = ChatColor.WHITE + "> ";
    private static final String EMPTY_SLOT = ChatColor.DARK_GRAY + "" + ChatColor.ITALIC + "-- Slot ? --";

    private final Scoreboard scoreboard;
    private final Objective header;
    private final BoardSlot[] slots;
    private int selected;

    public AbilityBoard() {
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        header = scoreboard.registerNewObjective("Abilities", Criteria.DUMMY, ChatColor.BOLD + "Abilities");
        header.setDisplaySlot(DisplaySlot.SIDEBAR);
        slots = new BoardSlot[9];
        selected = 0;

        createEmptySlots();
    }   

    private void createEmptySlots() {
        for (int i = 0; i < 9; ++i) {
            slots[i] = new BoardSlot(i);
            clearSlot(i);
            // - (i+1) because PK does that and I like how it looks
            header.getScore(slots[i].entry).setScore(-(i+1));
        }
    }

    public void clearSlot(int slotNumber) {
        String s = EMPTY_SLOT.replace("?", Integer.toString(slotNumber+1));
        display(slotNumber, s);
    }

    public void display(int slotNumber, String s) {
        slots[slotNumber].team.setSuffix(s);
    }

    public void setSelected(int slotNumber) {
        BoardSlot prevSelected = slots[selected];
        BoardSlot curSelected = slots[slotNumber];
        prevSelected.team.setPrefix(PREFIX_UNSELECTED);
        curSelected.team.setPrefix(PREFIX_SELECTED);
        selected = slotNumber;
    }

    public Scoreboard getScoreboard() {
        return scoreboard; 
    }

    private class BoardSlot {
        
        private final int slotNumber;
        private String entry;
        private String name;
        private Team team;

        BoardSlot(int slotNumber) {
            this.slotNumber = slotNumber;
            // These need to be invisible & unique
            this.entry = hashEntry();
            this.name = "Slot " + (slotNumber + 1);
            this.team = scoreboard.registerNewTeam(name);
            this.team.addEntry(entry);
            this.team.setPrefix(PREFIX_UNSELECTED);
        }

        private String hashEntry() {
            return ChatColor.values()[slotNumber].toString(); 
        }
    }
}
