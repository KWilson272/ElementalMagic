package me.kwilson272.elementalmagic.core.command.commands;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityStorage;
import me.kwilson272.elementalmagic.api.ability.Element;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.api.user.UserManager;
import me.kwilson272.elementalmagic.core.command.SubCommand;
import net.md_5.bungee.api.ChatColor;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ChooseCommand implements SubCommand {

    @Override
    public void executeCommand(CommandSender sender, String[] args) {
        AbilityStorage storage = ElementalMagicApi.abilityStorage();
        UserManager userManager = ElementalMagicApi.userManager();

        String elementName = args[1];
        Player player = (Player) sender;

        Element element = storage.getElement(elementName).orElse(null);
        if (element == null) {
            sender.sendMessage("Unknown Element: '" + elementName + "'.");
            return;
        }

        AbilityUser user = userManager.get(player).orElse(null);
        if (player == null || user == null) {
            sender.sendMessage("An internal error has occured and your user " +  
                    "object could not be loaded.");
            return;
        }

        String display = element.color() + element.name() + ChatColor.RESET;
        if (user.hasElement(element)) {
            sender.sendMessage("You already have the element '" + display + "'.");
        } else if (!user.addElement(element)) {
            sender.sendMessage("You can not choose the element '" + display + "' at this time.");
        } else {
            sender.sendMessage("You have chosen '" + display + "'!");
        }
    }

    @Override
    public String getName() {
        return "Choose";
    }

    @Override
    public int getMinArgs() {
        return 2;
    }

    @Override
    public boolean requiresPlayer() {
        return true;
    }
}

