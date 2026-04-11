package dev.melncat.identitytheft.command;

import com.destroystokyo.paper.profile.PlayerProfile;
import dev.melncat.identitytheft.IdentityManager;
import dev.melncat.identitytheft.IdentityTheft;
import dev.melncat.identitytheft.IdentityTheftConfig;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class IdentityTheftCommand implements CommandExecutor, TabCompleter {
	private final IdentityTheft plugin;
	
	public IdentityTheftCommand(IdentityTheft plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String name, String[] args) {
		if (args.length == 0) {
			sender.sendMessage(ChatColor.YELLOW + plugin.getName() + " " + ChatColor.GRAY + "v" + ChatColor.GREEN + plugin.getDescription().getVersion() + "\n"
				+ ChatColor.GRAY + "Made by " + ChatColor.GREEN + plugin.getDescription().getAuthors().get(0));
			return true;
		}
		if (args[0].equalsIgnoreCase("reload")) {
			if (!sender.hasPermission("identitytheft.command.identitytheft.reload")) {
				sendMissingPermission(sender, "identitytheft.command.identitytheft.reload");
				return true;
			}
			plugin.reloadConfig();
			plugin.getITConfig().setConfig(plugin.getConfig());
			sender.sendMessage(ChatColor.YELLOW + "IdentityTheft configuration successfully reloaded.");
		} else if (args[0].equalsIgnoreCase("become")) {
			if (!sender.hasPermission("identitytheft.command.identitytheft.become")) {
				sendMissingPermission(sender, "identitytheft.command.identitytheft.become");
				return true;
			}
			if (!(sender instanceof Player)) {
				sender.sendMessage(ChatColor.RED + "You must be a player to use this command.");
				return true;
			}
			if (args.length < 2) {
				sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cInsufficient arguments provided.\n&cSyntax: /it become <player>"));
				return true;
			}
			if (((Player) sender).getPlayerProfile().hasProperty("it_real")) {
				sender.sendMessage(ChatColor.RED + "You cannot use this while your identity is changed.\n" + ChatColor.RED + "Reset it first with /it reset.");
				return true;
			}
			Player player = (Player) sender;
			String targetName = args[1];
			resolveUUID(sender, targetName, target -> {
				if (plugin.getITConfig().opProtection()
					&& !sender.isOp()
					&& Bukkit.getOperators().stream().anyMatch(x -> x.getUniqueId().equals(target))
				) {
					sender.sendMessage(ChatColor.RED + "You cannot change your identity into an operator.");
					return;
				}
				IdentityManager.getInstance().setChangedIdentity(player.getUniqueId(), target);
				player.kickPlayer("Please rejoin for changes to apply.");
			});
		} else if (args[0].equalsIgnoreCase("set")) {
			if (!sender.hasPermission("identitytheft.command.identitytheft.set")) {
				sendMissingPermission(sender, "identitytheft.command.identitytheft.set");
				return true;
			}
			if (args.length < 3) {
				sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cInsufficient arguments provided.\n&cExample: /it set <from> <to>"));
				return true;
			}
			String fromName = args[1];
			String toName = args[2];
			resolveUUID(sender, fromName, from -> {
				resolveUUID(sender, toName, to -> {
					if (plugin.getITConfig().opProtection()
						&& !sender.isOp()
						&& Bukkit.getOperators().stream().anyMatch(x -> x.getUniqueId().equals(from) || x.getUniqueId().equals(to))
					) {
						sender.sendMessage(ChatColor.RED + "You cannot change the identities of operators.");
						return;
					}
					IdentityManager.getInstance().setChangedIdentity(from, to);
					sender.sendMessage(ChatColor.YELLOW
						+ "The player "
						+ ChatColor.WHITE
						+ fromName
						+ ChatColor.YELLOW
						+ " has successfully been changed to "
						+ ChatColor.WHITE
						+ toName
						+ ChatColor.YELLOW
						+ ".");
					Player fromPlayer = Bukkit.getPlayer(from);
					if (fromPlayer != null) fromPlayer.kickPlayer("Disconnected");
				});
			});
		} else if (args[0].equalsIgnoreCase("reset")) {
			if (!sender.hasPermission("identitytheft.command.identitytheft.reset")) {
				sendMissingPermission(sender, "identitytheft.command.identitytheft.reset");
				return true;
			}
			if (args.length < 2) {
				if (!(sender instanceof Player)) {
					
					sender.sendMessage(ChatColor.RED + "You must be a player to use /it reset without arguments.");
					return true;
				}
				;
				if (IdentityManager.getInstance().hasChangedIdentity(getRealPlayer(((Player) sender).getUniqueId()))) {
					IdentityManager.getInstance().removeChangedIdentity(getRealPlayer(((Player) sender).getUniqueId()));
					((Player) sender).kickPlayer("Please rejoin for changes to apply.");
				} else sender.sendMessage(ChatColor.RED + "Your identity is not altered.");
				return true;
			}
			if (!sender.hasPermission("identitytheft.command.identitytheft.reset.others")) {
				sendMissingPermission(sender, "identitytheft.command.identitytheft.reset.others");
				return true;
			}
			String targetName = args[1];
			resolveUUID(sender, targetName, target -> {
				if (IdentityManager.getInstance().hasChangedIdentity(target)) {
					IdentityManager.getInstance().removeChangedIdentity(target);
					sender.sendMessage(ChatColor.YELLOW + "The identity of " + targetName + " has been reset.");
				} else sender.sendMessage(ChatColor.RED + "The identity of " + targetName + " is not altered.");
			});
		}
		return true;
	}
	private UUID getRealPlayer(UUID uuid) {
		Player player = Bukkit.getPlayer(uuid);
		if (player == null) return uuid;
		if (player.getPlayerProfile().hasProperty("it_real"))
			return UUID.fromString(player.getPlayerProfile().getProperties().stream().filter(x -> x.getName().equals("it_real")).findFirst().get().getValue());
		return uuid;
	}
	
	private void sendMissingPermission(CommandSender sender, String permission) {
		sender.sendMessage(ChatColor.RED + "You do not have permission to perform this command.\n"
			+ ChatColor.RED + "Missing permission "
			+ ChatColor.DARK_RED + permission);
	}
	
	/**
	 * Resolves a player name or UUID string to a UUID.
	 * Tries direct UUID parsing and local server lookup first; falls back to
	 * an asynchronous Mojang profile lookup so that players who have never
	 * joined this server (including freshly-created accounts) can be targeted.
	 * The {@code onFound} callback is always invoked on the main thread.
	 */
	private void resolveUUID(CommandSender sender, String name, Consumer<UUID> onFound) {
		try {
			onFound.accept(UUID.fromString(name));
			return;
		} catch (IllegalArgumentException ignored) {}

		UUID local = Bukkit.getPlayerUniqueId(name);
		if (local != null) {
			onFound.accept(local);
			return;
		}

		sender.sendMessage(ChatColor.YELLOW + "Looking up player profile for " + name + "...");
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			PlayerProfile profile = Bukkit.createProfile(name);
			boolean found = profile.complete(false);
			UUID foundId = profile.getId();
			Bukkit.getScheduler().runTask(plugin, () -> {
				if (found && foundId != null) {
					onFound.accept(foundId);
				} else {
					sender.sendMessage(ChatColor.RED + name + " is not a valid player.");
				}
			});
		});
	}
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String name, String[] args) {
		List<String> suggestions = new ArrayList<>();
		if (args.length == 1) {
			if (sender.hasPermission("identitytheft.command.identitytheft.reload")) suggestions.add("reload");
			if (sender.hasPermission("identitytheft.command.identitytheft.become")) suggestions.add("become");
			if (sender.hasPermission("identitytheft.command.identitytheft.set")) suggestions.add("set");
			if (sender.hasPermission("identitytheft.command.identitytheft.reset")) suggestions.add("reset");
		} else if (args.length == 2) {
			if (args[0].equalsIgnoreCase("become") || args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("reset"))
				suggestions.addAll(listPlayerNames());
		} else if (args.length == 3) {
			if (args[0].equalsIgnoreCase("set")) suggestions.addAll(listPlayerNames());
		}
		return suggestions.stream().filter(x -> x.toLowerCase().startsWith(args[args.length - 1].toLowerCase())).collect(Collectors.toList());
	}
	
	private List<String> listPlayerNames() {
		return Bukkit.getOnlinePlayers().stream().map(HumanEntity::getName).collect(Collectors.toList());
	}
}
