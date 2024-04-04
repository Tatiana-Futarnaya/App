package org.example.repository.impl;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import org.example.db.PropertiesManager;
import org.example.model.Position;
import org.example.repository.PositionRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.ext.ScriptUtils;
import org.testcontainers.jdbc.JdbcDatabaseDelegate;
import org.testcontainers.junit.jupiter.Container;


import java.util.Optional;


class PositionRepositoryImplTest {
    private static final String INIT_SQL = "sql/schema.sql";
    private static final int containerPort = 5432;
    private static final int localPort = 5432;
    @Container
    public static PostgreSQLContainer<?> container = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("postgres")
            .withUsername(PropertiesManager.getProperties("db.username"))
            .withPassword(PropertiesManager.getProperties("db.password"))
            .withExposedPorts(containerPort)
            .withCreateContainerCmdModifier(cmd -> cmd.withHostConfig(
                    new HostConfig().withPortBindings(new PortBinding(Ports.Binding.bindPort(localPort), new ExposedPort(containerPort)))
            ))
            .withInitScript(INIT_SQL);
    public static PositionRepository positionRepository;
    private static JdbcDatabaseDelegate jdbcDatabaseDelegate;

    @BeforeAll
    static void beforeAll() {
        container.start();
        positionRepository = PositionRepositoryImpl.getInstance();
        jdbcDatabaseDelegate = new JdbcDatabaseDelegate(container, "");
    }

    @AfterAll
    static void afterAll() {
        container.stop();
    }

    @BeforeEach
    void setUp() {
        ScriptUtils.runInitScript(jdbcDatabaseDelegate, INIT_SQL);
    }

    @Test
    void save() {
        String expectedName = "new Position Name";
        Position position = new Position(null, expectedName);
        position = positionRepository.save(position);
        Optional<Position> resultPosition = positionRepository.findById(position.getId());

        Assertions.assertTrue(resultPosition.isPresent());
        Assertions.assertEquals(expectedName, resultPosition.get().getName());
    }

    @Test
    void update() {
        String expectedName = "UPDATE Position Name";

        Position positionForUpdate = positionRepository.findById(3L).get();
        String oldPositionName = positionForUpdate.getName();

        positionForUpdate.setName(expectedName);
        positionRepository.update(positionForUpdate);

        Position position = positionRepository.findById(3L).get();

        Assertions.assertNotEquals(expectedName, oldPositionName);
        Assertions.assertEquals(expectedName, position.getName());
    }

    @DisplayName("Delete by ID")
    @Test
    void deleteById() {
        Boolean expectedValue = true;
        int expectedSize = positionRepository.findAll().size();

        Position tempPosition = new Position(null, "Position for delete.");
        tempPosition = positionRepository.save(tempPosition);

        boolean resultDelete = positionRepository.deleteById(tempPosition.getId());
        int positionListAfterSize = positionRepository.findAll().size();

        Assertions.assertEquals(expectedValue, resultDelete);
        Assertions.assertEquals(expectedSize, positionListAfterSize);
    }


    @DisplayName("Find by ID")
    @ParameterizedTest
    @CsvSource(value = {
            "1; true",
            "4; true",
            "100; false"
    }, delimiter = ';')
    void findById(Long expectedId, Boolean expectedValue) {
        Optional<Position> position = positionRepository.findById(expectedId);
        Assertions.assertEquals(expectedValue, position.isPresent());
        position.ifPresent(value -> Assertions.assertEquals(expectedId, value.getId()));
    }

    @Test
    void findAll() {
        int expectedSize = 5;
        int resultSize = positionRepository.findAll().size();

        Assertions.assertEquals(expectedSize, resultSize);
    }

    @DisplayName("Exist by ID")
    @ParameterizedTest
    @CsvSource(value = {
            "1; true",
            "4; true",
            "100; false"
    }, delimiter = ';')
    void exitsById(Long positionId, Boolean expectedValue) {
        boolean isPositionExist = positionRepository.exitsById(positionId);

        Assertions.assertEquals(expectedValue, isPositionExist);
    }
}