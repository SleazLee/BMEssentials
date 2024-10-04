package at.sleazlee.bmessentials.trophyroom;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * The ItemBuilder class provides utility methods to serialize and deserialize ItemStack objects.
 * This ensures all item data, including NBT tags, are preserved when saving to or loading from a database.
 */
public class ItemBuilder {

    /**
     * Serializes an ItemStack to a Base64-encoded string.
     *
     * @param item The ItemStack to serialize.
     * @return A Base64-encoded string representing the serialized ItemStack.
     * @throws IOException If an I/O error occurs during serialization.
     */
    public static String itemStackToBase64(ItemStack item) throws IOException {
        // Initialize a byte array output stream to capture the serialized object
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        // Use BukkitObjectOutputStream to handle the serialization of Bukkit objects
        BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

        // Write the ItemStack object to the output stream
        dataOutput.writeObject(item);

        // Close the output stream to release resources
        dataOutput.close();

        // Encode the byte array to a Base64 string and return
        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }

    /**
     * Deserializes an ItemStack from a Base64-encoded string.
     *
     * @param data The Base64-encoded string representing the serialized ItemStack.
     * @return The deserialized ItemStack object.
     * @throws IOException            If an I/O error occurs during deserialization.
     * @throws ClassNotFoundException If the class of a serialized object cannot be found.
     */
    public static ItemStack itemStackFromBase64(String data) throws IOException, ClassNotFoundException {
        // Decode the Base64 string back into bytes
        ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data));
        // Use BukkitObjectInputStream to handle the deserialization of Bukkit objects
        BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);

        // Read the ItemStack object from the input stream
        ItemStack item = (ItemStack) dataInput.readObject();

        // Close the input stream to release resources
        dataInput.close();

        // Return the deserialized ItemStack
        return item;
    }
}
