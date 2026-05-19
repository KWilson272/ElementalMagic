package me.kwilson272.elementalmagic.core.command;

import org.bukkit.command.CommandSender;

public interface SubCommand {

    void executeCommand(CommandSender sender, String[] args);

    String getName();

    int getMinArgs(); 

    boolean requiresPlayer();
}

