package com.microservice_user.service;

import com.microservice_user.model.User;
import com.microservice_user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    // Método para buscar por username
    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    // Lógica de Login actualizada con username
    public User login(String username, String password) {
        // 1. Buscamos al usuario por username
        Optional<User> userOpt = userRepository.findByUsername(username);
        
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            // 2. Verificar que el usuario esté activo
            if (!user.getActivo()) {
                return null; // Usuario inactivo
            }
            // 3. Comparamos la contraseña (hashed con BCrypt en producción)
            if (user.getPassword().equals(password)) {
                // 4. Actualizar ultimo_acceso
                user.setUltimoAcceso(LocalDateTime.now());
                userRepository.save(user);
                return user; // Login exitoso
            }
        }
        return null; 
    }

    public void deleteUser(Long id){
        userRepository.deleteById(id);
    }
}