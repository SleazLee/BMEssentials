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
        TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {};
        HashMap<String, Object> o = mapper.readValue(json, typeRef);

        // Assuming a new method 'deserialize' or similar exists to replace 'from'
        return ItemStackUtil.deserialize(o).orElse(null);
    }

    public static String toJson(ItemStack stack) throws JsonProcessingException {
        // Assuming a new method 'serialize' or similar exists to replace 'to'
        return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(ItemStackUtil.serialize(stack));
    }
}
