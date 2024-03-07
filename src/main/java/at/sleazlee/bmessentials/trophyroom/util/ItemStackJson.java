package at.sleazlee.bmessentials.trophyroom.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.portlek.bukkititembuilder.util.ItemStackUtil;
import java.util.HashMap;
import org.bukkit.inventory.ItemStack;

public class ItemStackJson {
    public ItemStackJson() {
    }

    public static ItemStack fromJSON(String json) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {
        };
        HashMap<String, Object> o = (HashMap)mapper.readValue(json, typeRef);
        return ItemStackUtil.from(o).isPresent() ? (ItemStack)ItemStackUtil.from(o).get() : null;
    }

    public static String toJson(ItemStack stack) throws JsonProcessingException {
        return (new ObjectMapper()).writerWithDefaultPrettyPrinter().writeValueAsString(ItemStackUtil.to(stack));
    }
}
