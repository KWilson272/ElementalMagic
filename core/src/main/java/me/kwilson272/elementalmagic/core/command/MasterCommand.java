package me.kwilson272.elementalmagic.core.command;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.kwilson272.elementalmagic.core.command.commands.BindCommand;
import me.kwilson272.elementalmagic.core.command.commands.ChooseCommand;
import me.kwilson272.elementalmagic.core.command.commands.ReloadCommand;

/**
 * Small & Probably temporary command framework.
 */
public class MasterCommand implements CommandExecutor {

    private final Map<String, SubCommand> commandsByAlias;

    public MasterCommand() {
        commandsByAlias = new HashMap<>();

        registerCommand(new BindCommand(), "B", "Bind");
        registerCommand(new ChooseCommand(), "Ch", "Choose");
        registerCommand(new ReloadCommand(), "R", "Reload");

    }

    private void registerCommand(SubCommand subCommand, String... aliases) {
        for (String alias : aliases) { 
            commandsByAlias.put(alias.toUpperCase(), subCommand);
        }
    }

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Help command not implemented.");
            return true;
        }

        SubCommand cmd = commandsByAlias.get(args[0].toUpperCase());
        if (cmd == null) {
            sender.sendMessage("Unrecognized command.");
            return true;
        } else if (cmd.requiresPlayer() && !(sender instanceof Player)) {
            sender.sendMessage("You need to be an in-game player to execute this.");
            return true;
        } else if (cmd.getMinArgs() > args.length) {
            sender.sendMessage("Not enough arguments.");
            return true;
        }

        String permission = "elementalmagic.command." + cmd.getName().toLowerCase();
        if (!sender.hasPermission(permission)) {
            sender.sendMessage("You do not have permission to execute this command.");
        } else {
            cmd.executeCommand(sender, args);
        }
        return true;
	}
}
