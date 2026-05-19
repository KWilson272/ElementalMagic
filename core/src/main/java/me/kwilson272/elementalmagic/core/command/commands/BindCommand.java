package me.kwilson272.elementalmagic.core.command.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.kwilson272.elementalmagic.api.ElementalMagicApi;
import me.kwilson272.elementalmagic.api.ability.AbilityController;
import me.kwilson272.elementalmagic.api.ability.AbilityStorage;
import me.kwilson272.elementalmagic.api.user.AbilityUser;
import me.kwilson272.elementalmagic.api.user.UserManager;
import me.kwilson272.elementalmagic.core.command.SubCommand;
import net.md_5.bungee.api.ChatColor;

public class BindCommand implements SubCommand {

	@Override
	public void executeCommand(CommandSender sender, String[] args) {
        String abilityName = args[1];
        Player player = (Player) sender;
        int slot = player.getInventory().getHeldItemSlot() + 1;

        if (args.length >= 3) {
            try {
                slot = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage("You must pass an integer between 1-9");
            }
        }

        AbilityStorage storage = ElementalMagicApi.abilityStorage();
        AbilityController controller = storage.getController(abilityName).orElse(null);
        ChatColor color = controller.element().color();
        String cDisplay = color + controller.name() + ChatColor.RESET;
        String eDisplay = color + controller.element().name() + ChatColor.RESET;

        if (controller == null || controller.isHidden()) {
            sender.sendMessage("Unknown Ability: '" + abilityName + "'.");
            return;
        } else if (!controller.isBindable()) {
            sender.sendMessage("The Ability: '" + cDisplay + "' Cannot be bound.");
            return;
        }

        UserManager userManager = ElementalMagicApi.userManager();
        AbilityUser user = userManager.get(player).orElse(null);
        if (user == null) {
            sender.sendMessage("An error has occurred internally and your user " +
                    "object could not be located.");
            return;
        }

        if (!user.hasElement(controller.element())) {
            sender.sendMessage("You do not have the required element '" +
                    eDisplay + "' needed to bind '" + cDisplay + ".");
        } else if (!user.canBind(controller) || !user.bindController(controller, slot)) {
            sender.sendMessage("You cannot bind '" + cDisplay + "' at this time.");
        } else {
            sender.sendMessage("You successfully bound " + cDisplay + ".");
        }
	}

	@Override
	public String getName() {
        return "Bind";
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
