
package me.zombie_striker.wishingwells;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class Main extends JavaPlugin implements Listener {

	private static String Delay_message;
	private static String ItemNotAccepted;
	private static String TooBadMessage;
	private static String CongratsMessage;
	private static int Delay;
	
	private static double chance = 0.5;

	private HashMap<UUID, Long> storedTime = new HashMap<UUID, Long>();
	private List<Material> acceptedItems = new ArrayList<Material>();
	private HashMap<ItemStack[], Double> returns = new HashMap<ItemStack[], Double>();
	private List<Location> wells = new ArrayList<Location>();

	@Override
	public void onEnable() {
		// Download the API dependancy
		try {
			if (Bukkit.getPluginManager().getPlugin("PluginConstructorAPI") == null)
				new DependencyDownloader(this, 276723);
		} catch (Exception e) {
			e.printStackTrace();
		}
		getServer().getPluginManager().registerEvents(this, this);

		if (!new File(getDataFolder(), "config.yml").exists()) {
			saveDefaultConfig();
		}
		Delay_message = ChatColor.translateAlternateColorCodes('&', getConfig()
				.getString("Delay message"));
		ItemNotAccepted = ChatColor.translateAlternateColorCodes('&',
				getConfig().getString("ItemNotAccepted"));
		TooBadMessage = ChatColor.translateAlternateColorCodes('&', getConfig()
				.getString("TooBadMessage"));
		CongratsMessage = ChatColor.translateAlternateColorCodes('&',
				getConfig().getString("CongratsMessage"));
		Delay = getConfig().getInt("Delay");
		chance = getConfig().getDouble("ChanceOfNoReward");

		for (Object s : getConfig().getList("Accepted")) {
			if(s instanceof Integer) {
				Bukkit.broadcastMessage("IDs are no longer supported. Please use the material names");
			}else {
			acceptedItems.add(Material.matchMaterial((String)s));
			}
		}
		if (getConfig().contains("wellLocs")) {
			for (String keys : getConfig().getConfigurationSection("wellLocs")
					.getKeys(false)) {
				wells.add((Location) getConfig().get("wellLocs." + keys));
			}
		}

		for (String keys : getConfig().getConfigurationSection("Returned")
				.getKeys(false)) {
			try {
				ItemStack[] ii = new ItemStack[getConfig().getStringList(
						"Returned." + keys).size()];
				int k = 0;
				for (String items : getConfig().getStringList(
						"Returned." + keys)) {
					ii[k] = itemStackParser(items);
					k++;
				}
				returns.put(ii, (getConfig()
						.getDouble("ReturnedChance." + keys)));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		new Updater(this, 281062, getConfig().getBoolean("auto-update"));
	}

	@EventHandler
	public void onDrop(PlayerDropItemEvent e) {
		// if(acceptedItems.contains(e.getItemDrop().getItemStack().getTypeId())){
		if (!e.getPlayer().hasPermission("well.use"))
			return;
		new BukkitRunnable() {
			@Override
			public void run() {
				if (e.getItemDrop() == null || !e.getItemDrop().isValid()) {
					cancel();
					return;
				}

				if (e.getItemDrop().isOnGround()) {
					boolean nearWell = false;
					for (Location well : Main.this.wells) {
						if(well.getWorld().equals(e.getItemDrop().getWorld()))
						if (well.distance(e.getItemDrop().getLocation()) < 2) {
							nearWell = true;
							break;
						}
					}
					if (nearWell) {
						if (!acceptedItems.contains(e.getItemDrop()
								.getItemStack().getType())) {
							e.getPlayer().sendMessage(ItemNotAccepted);
							e.getPlayer().getInventory()
									.addItem(e.getItemDrop().getItemStack());
							e.getItemDrop().remove();
							cancel();
							return;
						}
						if (storedTime.containsKey(e.getPlayer().getUniqueId())
								&& (System.currentTimeMillis() - storedTime
										.get(e.getPlayer().getUniqueId())) < (Delay * 1000)) {
							e.getPlayer()
									.sendMessage(
											Delay_message.replaceAll(
													"%s",
													""
															+ (((Delay * 1000) - (System
																	.currentTimeMillis() - storedTime
																	.get(e.getPlayer()
																			.getUniqueId())))/1000)));
							e.getPlayer().getInventory()
									.addItem(e.getItemDrop().getItemStack());
							e.getItemDrop().remove();
							cancel();
							return;
						}
						storedTime.put(e.getPlayer().getUniqueId(), System.currentTimeMillis());
						if(e.getItemDrop().getItemStack().getAmount()>1){
							ItemStack is = e.getItemDrop().getItemStack();
							is.setAmount(is.getAmount()-1);
							e.getPlayer().getInventory()
									.addItem(is);
						}

						boolean wonAnything = Math.random() > chance;
						if (wonAnything) {
							double total = 0;
							for(double d : returns.values()) {
								total+=d;
							}
							double chance = (Math.random()*total);
							for (Entry<ItemStack[], Double> ent : returns
									.entrySet()) {
								if (chance <= ent.getValue()) {
									e.getPlayer().sendMessage(CongratsMessage);
									e.getPlayer().getInventory()
											.addItem(ent.getKey());
									break;
								} else {
									chance -= ent.getValue();
								}
							}
						} else {
							e.getPlayer().sendMessage(TooBadMessage);
						}
						e.getItemDrop().remove();
					}
					cancel();
				}
			}
		}.runTaskTimer(this, 20, 10);
		// }
	}

	public void addApplicable(String testing, String arg, List<String> array) {
		if (testing.toLowerCase().startsWith(arg.toLowerCase()))
			array.add(testing);
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command,
			String alias, String[] args) {
		List<String> tabs = new ArrayList<String>();
		if (args.length == 1) {
			addApplicable("create", args[0], tabs);
			addApplicable("destroy", args[0], tabs);
			addApplicable("list", args[0], tabs);
		}
		if (args.length == 2)
			if (getConfig().contains("wellLocs"))
				for (String key : getConfig().getConfigurationSection(
						"wellLocs").getKeys(false))
					addApplicable(key, args[1], tabs);
		return tabs;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command,
			String label, String[] args) {
		if (args.length == 0) {
			sender.sendMessage("--==Accepted commands==--");
			sender.sendMessage("/well create <name>");
			sender.sendMessage("/well destroy <name>");
			sender.sendMessage("/well list");
			return true;
		}
		if (!sender.hasPermission("well.command")) {
			sender.sendMessage(org.bukkit.ChatColor.RED
					+ "You do not have permission to use this command");
			return true;
		}
		if (args[0].equalsIgnoreCase("create")) {
			if (args.length < 2) {
				sender.sendMessage("--==Accepted commands==--");
				sender.sendMessage("/well create <name>");
				return true;
			}
			getConfig().set("wellLocs." + args[1],
					((Player) sender).getLocation());
			saveConfig();
			wells.add(((Player) sender).getLocation());
			sender.sendMessage("Well created!");
		} else if (args[0].equalsIgnoreCase("destroy")) {
			if (args.length < 2) {
				sender.sendMessage("--==Accepted commands==--");
				sender.sendMessage("/well destroy <name>");
				return true;
			}
			Location temp = (Location) getConfig().get("wellLocs." + args[1]);
			getConfig().set("wellLocs." + args[1], null);
			saveConfig();
			wells.remove(temp);
			sender.sendMessage("Well destroyed!");
		} else if (args[0].equalsIgnoreCase("list")) {
			sender.sendMessage("--== Wells ==--");
			if (!getConfig().contains("wellLocs")) {
				sender.sendMessage("No wells have been created");
				return true;
			}
			for (String keys : getConfig().getConfigurationSection("wellLocs")
					.getKeys(false)) {
				Location ll = (Location) getConfig().get("wellLocs." + keys);
				sender.sendMessage(keys + ": X:" + ll.getBlockX() + " Y:"
						+ ll.getBlockY() + " Z:" + ll.getBlockZ());
			}
		} else {
			sender.sendMessage("--==Accepted commands==--");
			sender.sendMessage("/well create <name>");
			sender.sendMessage("/well destroy <name>");
			sender.sendMessage("/well list");
		}

		return true;
	}

	public static ItemStack itemStackParser(String message) {
		String[] parts = message.split(" ");
		ItemStack item = null;
		try {
			item = new ItemStack(Material.matchMaterial(parts[0]));
		} catch (Exception e) {
		//	item = new ItemStack(Integer.parseInt(parts[0]));
		}
		if (parts.length > 1)
			item.setAmount(Integer.parseInt(parts[1]));
		for (int i = 2; i < parts.length; i++) {
			String p = parts[i];
			if (p.startsWith("efficiency"))
				item.addEnchantment(Enchantment.DIG_SPEED,
						Integer.parseInt(p.split(":")[1]));
			if (p.startsWith("name")) {
				ItemMeta im = item.getItemMeta();
				String name = ChatColor.translateAlternateColorCodes('&',
						p.split(":")[1].replaceAll("_", " "));
				im.setDisplayName(name);
				item.setItemMeta(im);
				im = item.getItemMeta();
			}
			if (p.startsWith("lore")) {
				ItemMeta im = item.getItemMeta();
				String lore = ChatColor.translateAlternateColorCodes('&',
						p.split(":")[1]).replaceAll("_", " ");
				List<String> l = im.getLore();
				if (l == null)
					l = new ArrayList<String>();
				l.add(lore);
				im.setLore(l);
				item.setItemMeta(im);
			}
			if (p.startsWith("durability"))
				item.addEnchantment(Enchantment.DURABILITY,
						Integer.parseInt(p.split(":")[1]));
		}
		return item;
	}
}
