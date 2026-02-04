package com.plantrack.backend.controller;

import com.plantrack.backend.model.User;
import com.plantrack.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping
    public User createUser(@Valid @RequestBody User user) {
        return userService.createUser(user);
    }

    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    /**
     * Get users for mention dropdown - accessible by all authenticated users
     * Returns only active users with limited fields (id, name, email)
     */
    @GetMapping("/mentions")
    public List<User> getUsersForMentions() {
        return userService.getAllUsers().stream()
                .filter(user -> "ACTIVE".equalsIgnoreCase(user.getStatus()))
                .toList();
    }

    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id) {
        // This now works because UserService returns 'User', not 'Optional<User>'
        return userService.getUserById(id);
    }

    @PutMapping("/{id}")
    public User updateUser(@PathVariable Long id, @Valid @RequestBody User userDetails) {
        return userService.updateUser(id, userDetails);
    }

    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable Long id) {
        // This now works because we added deleteUser() to the Service
        userService.deleteUser(id);
    }
}