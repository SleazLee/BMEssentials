package at.sleazlee.bmessentials.trophyroom.data;

import at.sleazlee.bmessentials.trophyroom.db.Database;
import at.sleazlee.bmessentials.trophyroom.menu.TrophyRoomMenu;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class Data {
    private static Data data;
    private TrophyRoomMenu menu;
    private Database db = Database.getDatabase();

    public Data() throws SQLException, JsonProcessingException {
        data = this;
    }

    public Trophy getTrophy(String id) {
        try {
            return this.db.getTrophy(id);
        } catch (JsonProcessingException | SQLException var3) {
            var3.printStackTrace();
            return null;
        }
    }

    public Trophy getTrophy(ItemStack item) {
        try {
            Map<String, Trophy> trophies = this.db.getTrophies();
            Iterator var3 = trophies.values().iterator();

            Trophy trophy;
            do {
                if (!var3.hasNext()) {
                    return null;
                }

                trophy = (Trophy)var3.next();
            } while(!trophy.getItem().isSimilar(item));

            return trophy;
        } catch (JsonProcessingException | SQLException var5) {
            var5.printStackTrace();
            return null;
        }
    }

    public Map<String, Integer> getTrophies(String uuid) {
        try {
            return this.db.getPlayerTrophies(uuid);
        } catch (SQLException var3) {
            var3.printStackTrace();
            return null;
        }
    }

    public String getSetting(String key) {
        try {
            return this.db.getSetting(key);
        } catch (SQLException var3) {
            var3.printStackTrace();
            return null;
        }
    }

    public void addTrophy(String id, @NotNull Trophy trophy) throws SQLException, JsonProcessingException {
        Database.getDatabase().insertTrophy(id, trophy);
    }

    public void addPlayerTrophy(String uuid, @NotNull Trophy trophy, int slot) {
        this.addPlayerTrophy(uuid, trophy.getId(), slot);
    }

    public void addPlayerTrophy(String uuid, String trophyId, int slot) {
        try {
            this.db.insertPlayerTrophy(uuid, trophyId, slot);
        } catch (SQLException var5) {
            var5.printStackTrace();
        }

    }

    public void removePlayerTrophy(String uuid, @NotNull Trophy trophy) {
        this.removePlayerTrophy(uuid, trophy.getId());
    }

    public void removePlayerTrophy(String uuid, String trophyId) {
        try {
            this.db.removePlayerTrophy(uuid, trophyId);
        } catch (SQLException var4) {
            var4.printStackTrace();
        }

    }

    public TrophyRoomMenu getMenu() {
        return this.menu;
    }

    public void setMenu(TrophyRoomMenu menu) {
        this.menu = menu;
    }

    public static Data getData() {
        return data;
    }
}
