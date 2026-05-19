package me.kwilson272.elementalmagic.core.command.commands;

import org.bukkit.command.CommandSender;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.core.command.SubCommand;

public class ReloadCommand implements SubCommand {

	@Override
	public void executeCommand(CommandSender sender, String[] args) {
        sender.sendMessage("Reloading ElementalMagic...");
        ElementalMagicApi.plugin().reload();
        sender.sendMessage("Reloaded ElementalMagic!");
	}

	@Override
	public String getName() {
        return "Reload";
	}

	@Override
	public int getMinArgs() {
        return 1;
	}

	@Override
	public boolean requiresPlayer() {
	    return false;
    }
}
