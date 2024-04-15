/*
 * MIT License
 *
 * Copyright (c) 2021 Hasan Demirtaş
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package at.sleazlee.bmessentials.trophyroom.smartinventory;

import at.sleazlee.bmessentials.BMEssentials;
import at.sleazlee.bmessentials.Scheduler;
import at.sleazlee.bmessentials.trophyroom.smartinventory.event.PgTickEvent;
import at.sleazlee.bmessentials.trophyroom.smartinventory.listener.*;
import at.sleazlee.bmessentials.trophyroom.smartinventory.opener.ChestInventoryOpener;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * a class that manages all smart inventories.
 */
public interface SmartInventory {

  /**
   * default inventory openers.
   */
  List<at.sleazlee.bmessentials.trophyroom.smartinventory.InventoryOpener> DEFAULT_OPENERS = Collections.singletonList(
    new ChestInventoryOpener());

  /**
   * all listener to register.
   */
  Function<Consumer<UUID>, List<Listener>> LISTENERS = function -> Arrays.asList(
    new InventoryClickListener(),
    new InventoryOpenListener(),
    new InventoryCloseListener(function),
    new PlayerQuitListener(function),
    new PluginDisableListener(),
    new InventoryDragListener());

  /**
   * obtains the given {@code uniqueId}'s smart holder.
   *
   * @param uniqueId the unique id to obtain.
   *
   * @return smart holder.
   */
  @NotNull
  static Optional<at.sleazlee.bmessentials.trophyroom.smartinventory.SmartHolder> getHolder(@NotNull final UUID uniqueId) {
    return Optional.ofNullable(Bukkit.getPlayer(uniqueId))
      .flatMap(SmartInventory::getHolder);
  }

  /**
   * obtains the given {@code player}'s smart holder.
   *
   * @param player the player to obtain.
   *
   * @return smart holder.
   */
  @NotNull
  static Optional<at.sleazlee.bmessentials.trophyroom.smartinventory.SmartHolder> getHolder(@NotNull final Player player) {
    final InventoryHolder holder = player.getOpenInventory().getTopInventory().getHolder();
    if (!(holder instanceof at.sleazlee.bmessentials.trophyroom.smartinventory.SmartHolder)) {
      return Optional.empty();
    }
    return Optional.of((at.sleazlee.bmessentials.trophyroom.smartinventory.SmartHolder) holder)
      .filter(at.sleazlee.bmessentials.trophyroom.smartinventory.SmartHolder::isActive);
  }

  /**
   * obtains the smart holders of all the online players.
   *
   * @return smart holders of online players.
   */
  @NotNull
  static List<at.sleazlee.bmessentials.trophyroom.smartinventory.SmartHolder> getHolders() {
    return Bukkit.getOnlinePlayers().stream()
      .map(SmartInventory::getHolder)
      .filter(Optional::isPresent)
      .map(Optional::get)
      .collect(Collectors.toList());
  }

  /**
   * obtains the players that see the given page.
   *
   * @param page the page to obtain.
   *
   * @return a player list.
   */
  @NotNull
  static List<Player> getOpenedPlayers(@NotNull final at.sleazlee.bmessentials.trophyroom.smartinventory.Page page) {
    return SmartInventory.getHolders().stream()
      .filter(holder -> page.id().equals(holder.getPage().id()))
      .map(at.sleazlee.bmessentials.trophyroom.smartinventory.SmartHolder::getPlayer)
      .collect(Collectors.toList());
  }

  /**
   * runs {@link at.sleazlee.bmessentials.trophyroom.smartinventory.InventoryProvider#update(at.sleazlee.bmessentials.trophyroom.smartinventory.InventoryContents)} method of the player's page.
   *
   * @param player the player to notify.
   */
  static void notifyUpdate(@NotNull final Player player) {
    SmartInventory.getHolder(player).ifPresent(smartHolder ->
      smartHolder.getContents().notifyUpdate());
  }

  /**
   * runs {@link at.sleazlee.bmessentials.trophyroom.smartinventory.InventoryProvider#update(at.sleazlee.bmessentials.trophyroom.smartinventory.InventoryContents)} method of the given provider's class.
   *
   * @param provider the provider to notify.
   * @param <T> type of the class.
   */
  static <T extends at.sleazlee.bmessentials.trophyroom.smartinventory.InventoryProvider> void notifyUpdateForAll(@NotNull final Class<T> provider) {
    SmartInventory.getHolders().stream()
      .map(at.sleazlee.bmessentials.trophyroom.smartinventory.SmartHolder::getContents)
      .filter(contents -> provider.equals(contents.page().provider().getClass()))
      .forEach(at.sleazlee.bmessentials.trophyroom.smartinventory.InventoryContents::notifyUpdate);
  }

  /**
   * runs {@link InventoryProvider#update(InventoryContents)} method of the page called the given id.
   *
   * @param id the id to find and run the update method.
   */
  static void notifyUpdateForAllById(@NotNull final String id) {
    SmartInventory.getHolders().stream()
      .map(at.sleazlee.bmessentials.trophyroom.smartinventory.SmartHolder::getPage)
      .filter(page -> page.id().equals(id))
      .forEach(at.sleazlee.bmessentials.trophyroom.smartinventory.Page::notifyUpdateForAll);
  }

  /**
   * finds a {@link at.sleazlee.bmessentials.trophyroom.smartinventory.InventoryOpener} from the given {@link InventoryType}.
   *
   * @param type the type to find.
   *
   * @return the inventory opener from the given type.
   */
  @NotNull
  default Optional<at.sleazlee.bmessentials.trophyroom.smartinventory.InventoryOpener> findOpener(@NotNull final InventoryType type) {
    return Stream.of(this.getOpeners(), SmartInventory.DEFAULT_OPENERS)
      .flatMap(Collection::stream)
      .filter(opener -> opener.supports(type))
      .findFirst();
  }

  /**
   * obtains inventory openers.
   *
   * @return inventory openers.
   */
  @NotNull
  Collection<at.sleazlee.bmessentials.trophyroom.smartinventory.InventoryOpener> getOpeners();

  /**
   * obtains the plugin.
   *
   * @return the plugin.
   */
  @NotNull
  Plugin getPlugin();

  /**
   * obtains the given uniqueId's task.
   *
   * @param uniqueId the uniqueId to obtain.
   *
   * @return a {@link BukkitRunnable} instance.
   */
  @NotNull
  default Optional<BukkitRunnable> getTask(@NotNull final UUID uniqueId) {
    return Optional.ofNullable(this.getTasks().get(uniqueId));
  }

  /**
   * obtains the tasks.
   *
   * @return tasks.
   */
  @NotNull
  Map<UUID, BukkitRunnable> getTasks();

  /**
   * initiates the manager.
   */
  default void init() {
    SmartInventory.LISTENERS.apply(this::stopTick).forEach(listener ->
      Bukkit.getPluginManager().registerEvents(listener, this.getPlugin()));
  }

  /**
   * registers the given inventory openers.
   *
   * @param openers the openers to register.
   */
  default void registerOpeners(@NotNull final at.sleazlee.bmessentials.trophyroom.smartinventory.InventoryOpener... openers) {
    this.getOpeners().addAll(Arrays.asList(openers));
  }

  /**
   * removes given uniqueId of the ticking task.
   *
   * @param uniqueId the uniqueId to set.
   */
  default void removeTask(@NotNull final UUID uniqueId) {
    this.getTasks().remove(uniqueId);
  }

  /**
   * sets the given player of the ticking task to the given task.
   *
   * @param uniqueId the unique id to set.
   * @param task the task to set.
   */
  default void setTask(@NotNull final UUID uniqueId, @NotNull final BukkitRunnable task) {
    this.getTasks().put(uniqueId, task);
  }

  /**
   * stops the ticking of the given uniqueId.
   *
   * @param uniqueId the uniqueId to stop.
   */
  default void stopTick(@NotNull final UUID uniqueId) {
    this.getTask(uniqueId).ifPresent(runnable -> {
      Bukkit.getGlobalRegionScheduler().cancelTasks(BMEssentials.getInstance());
      this.removeTask(uniqueId);
    });
  }

  /**
   * starts the ticking of the given player with the given page.
   *
   * @param uniqueId the unique id to start.
   * @param page the page to start.
   */
  default void tick(@NotNull final UUID uniqueId, @NotNull final Page page) {
    final BukkitRunnable task = new BukkitRunnable() {
      @Override
      public void run() {
        SmartInventory.getHolder(uniqueId)
          .map(SmartHolder::getContents)
          .ifPresent(contents -> {
            page.accept(new PgTickEvent(contents));
            page.provider().tick(contents);
          });
      }
    };
    this.setTask(uniqueId, task);
      Scheduler.runTimer(task, page.startDelay(), page.tick());
  }

  /**
   * unregisters the given inventory openers.
   *
   * @param openers the openers to unregister.
   */
  default void unregisterOpeners(@NotNull final InventoryOpener... openers) {
    this.getOpeners().removeAll(Arrays.asList(openers));
  }
}