package me.kwilson272.elementalmagic.core.display;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import net.md_5.bungee.api.ChatColor;

public class AbilityBoard {
   
    private static final String PREFIX_UNSELECTED = ChatColor.DARK_GRAY + "> ";
    private static final String PREFIX_SELECTED = ChatColor.WHITE + "> ";
    private static final String EMPTY_SLOT = ChatColor.DARK_GRAY + "" + ChatColor.ITALIC + "-- Slot ? --";

    private final Scoreboard scoreboard;
    private final Objective header;
    private final BoardSlot[] slots;
    private int selected;
    
    private BoardSlot miscHeader;
    private int openNumber;
    private final Map<String, BoardSlot> miscSlots;

    public AbilityBoard() {
        scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        header = scoreboard.registerNewObjective("Abilities", Criteria.DUMMY, ChatColor.BOLD + "Abilities");
        header.setDisplaySlot(DisplaySlot.SIDEBAR);
        slots = new BoardSlot[9];
        selected = 0;
        
        openNumber = 11;
        miscSlots = new HashMap<>();

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

    public void addMiscCooldown(String cooldownId, String display) {
        // Cannot display more than 16 entries
        // Don't overwrite old slots or we will lose reference to them
        if (miscSlots.size() >= 5 || miscSlots.containsKey(cooldownId)) {
            return;
        }

        if (miscSlots.isEmpty()) {
            miscHeader = new BoardSlot(10); 
            header.getScore(miscHeader.entry).setScore(-10);
            miscHeader.team.setPrefix("");
            miscHeader.team.setSuffix(" -----");
        }
        
        createMiscSlot(cooldownId, display);
    }

    private void createMiscSlot(String cooldownId, String display) {
        BoardSlot miscSlot = new BoardSlot(openNumber);
        header.getScore(miscSlot.entry).setScore(-openNumber);
        miscSlot.team.setPrefix("  ");
        miscSlot.team.setSuffix(display);
        miscSlots.put(cooldownId, miscSlot);   
        openNumber++;
    }

    public void removeMiscCooldown(String cooldownId) {
        BoardSlot removal = miscSlots.remove(cooldownId);
        if (removal == null) {
            return;
        }

        scoreboard.resetScores(removal.entry);
        removal.team.unregister();

        // To avoid errors, redraw the misc portion of the board
        Map<String, String> keep = new HashMap<>();
        for (String id : miscSlots.keySet()) {
            BoardSlot slot = miscSlots.get(id);
            keep.put(id, slot.team.getSuffix());
            scoreboard.resetScores(slot.entry);
            slot.team.unregister();
        }
    
        openNumber = 11;
        for (String id : keep.keySet()) {
            createMiscSlot(id, keep.get(id));
        }

        if (miscSlots.isEmpty() && miscHeader != null) {
            scoreboard.resetScores(miscHeader.entry);
            miscHeader.team.unregister();
            miscHeader = null;
        }
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
            String hex = "#";
            if (slotNumber < 10) {
                hex += slotNumber + "00000";
            } else {
                hex += slotNumber + "0000";
            }
            
            return ChatColor.of(hex).toString();
        }
    }
}
