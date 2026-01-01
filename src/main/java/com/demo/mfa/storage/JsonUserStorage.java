package com.demo.mfa.storage;

import com.demo.mfa.model.User;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JSON-based storage for user data.
 * Persists users to a JSON file.
 */
public class JsonUserStorage {

    private final Path filePath;
    private final Gson gson;
    private List<User> users;

    private static final Type USER_LIST_TYPE = new TypeToken<List<User>>() {}.getType();

    /**
     * Creates a new JsonUserStorage with the specified file path.
     *
     * @param filePath Path to the JSON file for storing users
     */
    public JsonUserStorage(String filePath) {
        this.filePath = Path.of(filePath);
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.users = load();
    }

    /**
     * Loads users from the JSON file.
     *
     * @return List of users, or empty list if file doesn't exist
     */
    private List<User> load() {
        if (!Files.exists(filePath)) {
            return new ArrayList<>();
        }

        try {
            String json = Files.readString(filePath);
            List<User> loadedUsers = gson.fromJson(json, USER_LIST_TYPE);
            return loadedUsers != null ? loadedUsers : new ArrayList<>();
        } catch (IOException e) {
            System.err.println("Warning: Could not read users file: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Persists all users to the JSON file.
     */
    private void persist() {
        try {
            Files.writeString(filePath, gson.toJson(users));
        } catch (IOException e) {
            throw new RuntimeException("Failed to save users to file", e);
        }
    }

    /**
     * Finds a user by email address.
     *
     * @param email The email address to search for
     * @return Optional containing the user if found
     */
    public Optional<User> findByEmail(String email) {
        return users.stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email))
                .findFirst();
    }

    /**
     * Checks if a user with the given email exists.
     *
     * @param email The email address to check
     * @return true if the user exists, false otherwise
     */
    public boolean exists(String email) {
        return findByEmail(email).isPresent();
    }

    /**
     * Saves a new user or updates an existing one.
     *
     * @param user The user to save
     */
    public void save(User user) {
        Optional<User> existing = findByEmail(user.getEmail());
        if (existing.isPresent()) {
            users.remove(existing.get());
        }
        users.add(user);
        persist();
    }

    /**
     * Deletes a user by email.
     *
     * @param email The email of the user to delete
     * @return true if the user was deleted, false if not found
     */
    public boolean delete(String email) {
        Optional<User> existing = findByEmail(email);
        if (existing.isPresent()) {
            users.remove(existing.get());
            persist();
            return true;
        }
        return false;
    }

    /**
     * Gets all users.
     *
     * @return List of all users
     */
    public List<User> findAll() {
        return new ArrayList<>(users);
    }

    /**
     * Gets the number of registered users.
     *
     * @return The user count
     */
    public int count() {
        return users.size();
    }
}
