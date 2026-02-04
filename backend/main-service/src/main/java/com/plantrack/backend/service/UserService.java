package com.plantrack.backend.service;    
    
import com.plantrack.backend.model.User;    
import com.plantrack.backend.repository.UserRepository;
import com.plantrack.backend.repository.CommentRepository;
import com.plantrack.backend.repository.PlanRepository;    
import com.plantrack.backend.repository.MilestoneRepository;    
import com.plantrack.backend.repository.InitiativeRepository;    
import jakarta.persistence.EntityManager;    
import jakarta.transaction.Transactional;    
import org.springframework.beans.factory.annotation.Autowired;    
import org.springframework.security.crypto.password.PasswordEncoder;    
import org.springframework.stereotype.Service;    
    
import java.util.List;    
    
@Service    
public class UserService {    
    
    @Autowired    
    private UserRepository userRepository;    
    
    @Autowired    
    private PasswordEncoder passwordEncoder;    
    
    @Autowired    
    private AuditService auditService;    

    @Autowired    
    private CommentRepository commentRepository;    
    
    @Autowired    
    private PlanRepository planRepository;    
    
    @Autowired    
    private MilestoneRepository milestoneRepository;    
    
    @Autowired    
    private InitiativeRepository initiativeRepository;    
    
    @Autowired    
    private EntityManager entityManager;    

    public User createUser(User user) {  
        // Check if email already exists  
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {  
            throw new RuntimeException("A user with this email already exists. Please use a different email address.");  
        }  
          
        // Hash password if provided  
        if (user.getPassword() != null && !user.getPassword().isEmpty()) {  
            user.setPassword(passwordEncoder.encode(user.getPassword()));  
        }  
        User savedUser = userRepository.save(user);  
          
        // Audit Log  
        auditService.logCreate("USER", savedUser.getUserId(),  
            "Created user: " + savedUser.getName() + " (" + savedUser.getEmail() + ") with role: " + savedUser.getRole());  
          
        return savedUser;  
    }  
    
    public List<User> getAllUsers() {    
        return userRepository.findAll();    
    }    
    
    // UPDATED: Return User directly, not Optional    
    public User getUserById(Long id) {    
        return userRepository.findById(id)    
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));    
    }    
    
    public User updateUser(Long id, User userDetails) {    
        User user = userRepository.findById(id)    
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));    
            
        if (userDetails.getName() != null) {    
            user.setName(userDetails.getName());    
        }    
        if (userDetails.getEmail() != null) {    
            user.setEmail(userDetails.getEmail());    
        }    
        // Only update password if provided    
        if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {    
            user.setPassword(passwordEncoder.encode(userDetails.getPassword()));    
        }    
        if (userDetails.getDepartment() != null) {    
            user.setDepartment(userDetails.getDepartment());    
        }    
        if (userDetails.getRole() != null) {    
            user.setRole(userDetails.getRole());    
        }    
        if (userDetails.getStatus() != null) {    
            user.setStatus(userDetails.getStatus());    
        }    
            
        User savedUser = userRepository.save(user);    
            
        // Audit Log    
        auditService.logUpdate("USER", id, "Updated user: " + savedUser.getName() + " (" + savedUser.getEmail() + ")");    
            
        return savedUser;    
    }    
    
    // FIXED: Handle foreign key constraints properly    
    @Transactional    
    public void deleteUser(Long id) {    
        User user = userRepository.findById(id)    
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));    
            
        String userName = user.getName();    
        String userEmail = user.getEmail();    
            
        // Step 1: Delete notifications for this user    
        entityManager.createNativeQuery("DELETE FROM notifications WHERE user_id = :userId")    
            .setParameter("userId", id)    
            .executeUpdate();    
            
        // Step 2: Delete comment_mentions for comments authored by this user    
        entityManager.createNativeQuery("DELETE cm FROM comment_mentions cm " +    
            "INNER JOIN comments c ON cm.comment_id = c.comment_id " +    
            "WHERE c.author_id = :userId")    
            .setParameter("userId", id)    
            .executeUpdate();    
            
        // Step 3: Delete comments authored by this user    
        entityManager.createNativeQuery("DELETE FROM comments WHERE author_id = :userId")    
            .setParameter("userId", id)    
            .executeUpdate();    
            
        // Step 4: Remove user from initiative assignments    
        entityManager.createNativeQuery("DELETE FROM initiative_assignees WHERE user_id = :userId")    
            .setParameter("userId", id)    
            .executeUpdate();    
            
        // Step 5: Remove user from comment mentions (as a mentioned user)    
        entityManager.createNativeQuery("DELETE FROM comment_mentions WHERE user_id = :userId")    
            .setParameter("userId", id)    
            .executeUpdate();    
            
        // Step 6: Delete all plans owned by this user (including cascading milestones and initiatives)    
        List<Long> planIds = entityManager.createNativeQuery("SELECT plan_id FROM plans WHERE user_id = :userId", Long.class)    
            .setParameter("userId", id)    
            .getResultList();    
            
        for (Long planId : planIds) {    
            // Get all milestones for this plan    
            List<Long> milestoneIds = entityManager.createNativeQuery("SELECT milestone_id FROM milestones WHERE plan_id = :planId", Long.class)    
                .setParameter("planId", planId)    
                .getResultList();    
                
            // Delete all initiatives for each milestone    
            for (Long milestoneId : milestoneIds) {    
                // Clear initiative_assignees for initiatives in this milestone    
                entityManager.createNativeQuery("DELETE ia FROM initiative_assignees ia " +    
                    "INNER JOIN initiatives i ON ia.initiative_id = i.initiative_id " +    
                    "WHERE i.milestone_id = :milestoneId")    
                    .setParameter("milestoneId", milestoneId)    
                    .executeUpdate();    
                    
                // Delete comments for initiatives in this milestone    
                entityManager.createNativeQuery("DELETE c FROM comments c " +    
                    "INNER JOIN initiatives i ON c.initiative_id = i.initiative_id " +    
                    "WHERE i.milestone_id = :milestoneId")    
                    .setParameter("milestoneId", milestoneId)    
                    .executeUpdate();    
                    
                // Delete initiatives    
                entityManager.createNativeQuery("DELETE FROM initiatives WHERE milestone_id = :milestoneId")    
                    .setParameter("milestoneId", milestoneId)    
                    .executeUpdate();    
            }    
                
            // Delete milestones    
            entityManager.createNativeQuery("DELETE FROM milestones WHERE plan_id = :planId")    
                .setParameter("planId", planId)    
                .executeUpdate();    
                
            // Delete the plan    
            entityManager.createNativeQuery("DELETE FROM plans WHERE plan_id = :planId")    
                .setParameter("planId", planId)    
                .executeUpdate();    
        }    
            
        // Step 7: Finally delete the user    
        userRepository.deleteById(id);    
            
        // Audit Log    
        auditService.logDelete("USER", id, "Deleted user: " + userName + " (" + userEmail + ")");    
    }    
}