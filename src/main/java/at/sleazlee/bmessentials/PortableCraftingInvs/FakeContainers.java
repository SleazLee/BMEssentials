//package at.sleazlee.bmessentials.PortableCraftingInvs;
//
//import com.itsschatten.libs.Utils;
//import com.itsschatten.portablecrafting.events.*;
//import com.shanebeestudios.api.BrewingManager;
//import com.shanebeestudios.api.FurnaceManager;
//import com.shanebeestudios.api.VirtualFurnaceAPI;
//import net.minecraft.core.BlockPos;
//import net.minecraft.network.chat.Component;
//import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
//import net.minecraft.server.level.ServerPlayer;
//import net.minecraft.world.inventory.*;
//import org.bukkit.Bukkit;
//import org.bukkit.craftbukkit.v1_20_R3.CraftWorld;
//import org.bukkit.craftbukkit.v1_20_R3.entity.CraftPlayer;
//import org.bukkit.craftbukkit.v1_20_R3.event.CraftEventFactory;
//import org.bukkit.entity.Player;
//import org.bukkit.event.Listener;
//import org.bukkit.plugin.java.JavaPlugin;
//
//public class FakeContainers implements FakeContainers, Listener {
//    private final FurnaceManager manager;
//    private final BrewingManager brewingManager;
//    boolean debug, mysql;
//
//    MySqlI sql;
//
//    public FakeContainers_v1_20_R3(JavaPlugin plugin, MySqlI sql) {
//        VirtualFurnaceAPI furnaceAPI = new VirtualFurnaceAPI(plugin, true, true);
//        this.manager = furnaceAPI.getFurnaceManager();
//        this.brewingManager = furnaceAPI.getBrewingManager();
//        this.sql = sql;
//
//    }
//
//    @Override
//    public boolean openLoom(Player player) {
//        try {
//            ServerPlayer ePlayer = ((CraftPlayer) player).getHandle();
//            int containerID = ePlayer.nextContainerCounter();
//            final FakeLoom fakeLoom = new FakeLoom(containerID, player);
//
//            final LoomOpenEvent event = new LoomOpenEvent(player);
//            Bukkit.getPluginManager().callEvent(event);
//            if (!event.isCancelled()) {
//                ePlayer.containerMenu = ePlayer.inventoryMenu;
//                ePlayer.connection.send(new ClientboundOpenScreenPacket(containerID, MenuType.LOOM, fakeLoom.getTitle()));
//                ePlayer.containerMenu = fakeLoom;
//                return true;
//            }
//            return false;
//        } catch (UnsupportedOperationException ex) {
//            // Logging this error normally spams console
//            Utils.log("An error occurred while running the anvil command, make sure you have debug enabled to see this message.");
//            Utils.debugLog( ex.getMessage());
//            player.sendMessage("An error occurred, please contact an administrator.");
//            return false;
//        }
//    }
//
//    @Override
//    public boolean openAnvil(Player player) {
//        try {
//            CraftEventFactory.handleInventoryCloseEvent(((CraftPlayer) player).getHandle());
//            ServerPlayer ePlayer = ((CraftPlayer) player).getHandle();
//            int containerID = ePlayer.nextContainerCounter();
//            final FakeAnvil fakeAnvil = new FakeAnvil(containerID, player);
//
//            final AnvilOpenEvent event = new AnvilOpenEvent(player);
//            Bukkit.getPluginManager().callEvent(event);
//            if (!event.isCancelled()) {
//                player.openInventory(fakeAnvil.getBukkitView()); // Using this seemingly makes the Anvil listener far more consistent.
//                return true;
//            }
//            return false;
//        } catch (UnsupportedOperationException ex) {
//            // Logging this error normally spams console
//            Utils.log("An error occurred while running the anvil command, make sure you have debug enabled to see this message.");
//            Utils.debugLog( ex.getMessage());
//            player.sendMessage("An error occurred, please contact an administrator.");
//            return false;
//        }
//    }
//
//    @Override
//    public boolean openCartography(Player player) {
//        try {
//            ServerPlayer ePlayer = ((CraftPlayer) player).getHandle();
//            int containerID = ePlayer.nextContainerCounter();
//            FakeCartography fakeCartography = new FakeCartography(containerID, player);
//
//            final CartographyOpenEvent event = new CartographyOpenEvent(player);
//            Bukkit.getPluginManager().callEvent(event);
//            if (!event.isCancelled()) {
//                player.openInventory(fakeCartography.getBukkitView());
//                return true;
//            }
//            return false;
//        } catch (UnsupportedOperationException ex) {
//            // Logging this error normally spams console
//            Utils.log("An error occurred while running the anvil command, make sure you have debug enabled to see this message.");
//            Utils.debugLog( ex.getMessage());
//            player.sendMessage("An error occurred, please contact an administrator.");
//            return false;
//        }
//    }
//
//    @Override
//    public boolean openGrindStone(Player player) {
//        try {
//            ServerPlayer ePlayer = ((CraftPlayer) player).getHandle();
//            int containerId = ePlayer.nextContainerCounter();
//            FakeGrindstone fakeGrindstone = new FakeGrindstone(containerId, player);
//
//            final GrindStoneOpenEvent event = new GrindStoneOpenEvent(player);
//            Bukkit.getPluginManager().callEvent(event);
//            if (!event.isCancelled()) {
//                ePlayer.containerMenu = ePlayer.inventoryMenu;
//                ePlayer.connection.send(new ClientboundOpenScreenPacket(containerId, MenuType.GRINDSTONE, fakeGrindstone.getTitle()));
//                ePlayer.containerMenu = fakeGrindstone;
//                return true;
//            }
//            return false;
//        } catch (UnsupportedOperationException ex) {
//            Utils.debugLog( ex.getMessage());
//            Utils.log("An error occurred while running the grindstone command, make sure you have debug enabled to see this message.");
//            player.sendMessage("An error occurred, please contact an administrator.");
//            return false;
//        }
//    }
//
//    @Override
//    public boolean openStoneCutter(Player player) {
//        try {
//            ServerPlayer ePlayer = ((CraftPlayer) player).getHandle();
//            int containerID = ePlayer.nextContainerCounter();
//            FakeStoneCutter fakeStoneCutter = new FakeStoneCutter(containerID, player);
//
//            final StoneCutterOpenEvent event = new StoneCutterOpenEvent(player);
//            Bukkit.getPluginManager().callEvent(event);
//
//            if (!event.isCancelled()) {
//                ePlayer.containerMenu = ePlayer.inventoryMenu;
//                ePlayer.connection.send(new ClientboundOpenScreenPacket(containerID, MenuType.STONECUTTER, fakeStoneCutter.getTitle()));
//                ePlayer.containerMenu = fakeStoneCutter;
//
//                return true;
//            }
//            return false;
//        } catch (UnsupportedOperationException ex) {
//            // Logging this error normally spams console
//            Utils.log("An error occurred while running the anvil command, make sure you have debug enabled to see this message.");
//            Utils.debugLog( ex.getMessage());
//            player.sendMessage("An error occurred, please contact an administrator.");
//        }
//        return true;
//    }
//
//    @Override
//    public boolean openSmithing(Player player) {
//        try {
//            ServerPlayer ePlayer = ((CraftPlayer) player).getHandle();
//            int containerID = ePlayer.nextContainerCounter();
//            FakeSmithing fakeSmithing = new FakeSmithing(containerID, player);
//
//            final SmithingOpenEvent event = new SmithingOpenEvent(player);
//            Bukkit.getPluginManager().callEvent(event);
//            if (!event.isCancelled()) {
//                ePlayer.containerMenu = ePlayer.inventoryMenu;
//                ePlayer.connection.send(new ClientboundOpenScreenPacket(containerID, MenuType.SMITHING, fakeSmithing.getTitle()));
//                ePlayer.containerMenu = fakeSmithing;
//                return true;
//            }
//            return false;
//        } catch (UnsupportedOperationException ex) {
//            // Logging this error normally spams console
//            Utils.log("An error occurred while running the anvil command, make sure you have debug enabled to see this message.");
//            Utils.debugLog( ex.getMessage());
//            player.sendMessage("An error occurred, please contact an administrator.");
//            return false;
//        }
//    }
//
//    private static class FakeGrindstone extends GrindstoneMenu {
//
//        public FakeGrindstone(final int containerId, final Player player) {
//            super(containerId, ((CraftPlayer) player).getHandle().getInventory(), ContainerLevelAccess.create(((CraftWorld) player.getWorld()).getHandle(), new BlockPos(player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ())));
//            this.checkReachable = false;
//            this.setTitle(Component.literal("Repair & Disenchant"));
//        }
//    }
//
//    private static class FakeCartography extends CartographyTableMenu {
//
//        public FakeCartography(final int containerId, final Player player) {
//            super(containerId, ((CraftPlayer) player).getHandle().getInventory(), ContainerLevelAccess.create(((CraftWorld) player.getWorld()).getHandle(), new BlockPos(player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ())));
//            this.checkReachable = false;
//            this.setTitle(Component.literal("Cartography Table"));
//        }
//    }
//
//    private static class FakeLoom extends LoomMenu {
//
//        public FakeLoom(final int containerId, final Player player) {
//            super(containerId, ((CraftPlayer) player).getHandle().getInventory(),
//                    ContainerLevelAccess.create(((CraftWorld) player.getWorld()).getHandle(), new BlockPos(player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ())));
//            this.checkReachable = false;
//            this.setTitle(Component.literal("Loom"));
//        }
//    }
//
//    private static class FakeStoneCutter extends StonecutterMenu {
//
//        public FakeStoneCutter(final int containerId, final Player player) {
//            super(containerId, ((CraftPlayer) player).getHandle().getInventory(), ContainerLevelAccess.create(((CraftWorld) player.getWorld()).getHandle(), new BlockPos(player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ())));
//            this.checkReachable = false;
//            this.setTitle(Component.literal("Stonecutter"));
//        }
//    }
//
//    private static class FakeAnvil extends AnvilMenu {
//
//        public FakeAnvil(final int containerID, final Player player) {
//            super(containerID, ((CraftPlayer) player).getHandle().getInventory(),
//                    ContainerLevelAccess.create(((CraftWorld) player.getWorld()).getHandle(),
//                            new BlockPos(player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ())));
//            this.checkReachable = false; // ignore if the block is reachable, otherwise open regardless of distance.
//            this.setTitle(Component.literal("Repair & Name"));
//        }
//    }
//
//    private static class FakeSmithing extends SmithingMenu {
//
//        public FakeSmithing(final int containerID, final Player player) {
//            super(containerID, ((CraftPlayer) player).getHandle().getInventory(),
//                    ContainerLevelAccess.create(((CraftWorld) player.getWorld()).getHandle(),
//                            new BlockPos(player.getLocation().getBlockX(), player.getLocation().getBlockY(), player.getLocation().getBlockZ())));
//            this.checkReachable = false; // ignore if the block is reachable, otherwise open regardless of distance.
//            this.setTitle(Component.literal("Upgrade Gear"));
//        }
//    }
//}