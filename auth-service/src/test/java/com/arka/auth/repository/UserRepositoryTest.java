package com.arka.auth.repository;

import com.arka.auth.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
    "spring.cloud.config.enabled=false",
    "spring.cloud.discovery.enabled=false",
    "eureka.client.enabled=false"
})
class UserRepositoryTest {

  @Autowired
  private TestEntityManager entityManager;

  @Autowired
  private UserRepository userRepository;

  private User testUser;

  @BeforeEach
  void setUp() {
    testUser = new User();
    testUser.setUsername("testuser");
    testUser.setEmail("test@example.com");
    testUser.setPassword("hashedPassword");
  }

  @Test
  void findByUsername_ShouldReturnUser_WhenExists() {
    entityManager.persist(testUser);
    entityManager.flush();

    Optional<User> found = userRepository.findByUsername("testuser");

    assertTrue(found.isPresent());
    assertEquals("testuser", found.get().getUsername());
    assertEquals("test@example.com", found.get().getEmail());
  }

  @Test
  void findByUsername_ShouldReturnEmpty_WhenNotExists() {
    Optional<User> found = userRepository.findByUsername("nonexistent");

    assertFalse(found.isPresent());
  }

  @Test
  void findByEmail_ShouldReturnUser_WhenExists() {
    entityManager.persist(testUser);
    entityManager.flush();

    Optional<User> found = userRepository.findByEmail("test@example.com");

    assertTrue(found.isPresent());
    assertEquals("testuser", found.get().getUsername());
  }

  @Test
  void save_ShouldPersistUser() {
    User savedUser = userRepository.save(testUser);

    assertNotNull(savedUser.getId());
    assertEquals("testuser", savedUser.getUsername());
    assertEquals("test@example.com", savedUser.getEmail());
  }
}