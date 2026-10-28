package smart.fly.repository;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;

import smart.fly.airline.repository.impl.AirlineRepositoryImpl;
import smart.fly.airport.repository.impl.AirportRepositoryImpl;
import smart.fly.booking.repository.impl.BookingRepositoryImpl;
import smart.fly.booking.search.repository.BookingSearchRepository;
import smart.fly.businness.owner.repository.impl.BusinessOwnerRepositoryImpl;
import smart.fly.feedback.repository.impl.FeedbackRepositoryImpl;
import smart.fly.flight.add.repository.AddFlightRepository;
import smart.fly.flight.repository.impl.FlightRepositoryImpl;
import smart.fly.flight.search.repository.FlightSearchRepository;
import smart.fly.flight.seats.repository.impl.FlightSeatsRepositoryImpl;
import smart.fly.flightmanager.repository.impl.FlightManagerRepositoryImpl;
import smart.fly.passenger.repository.impl.PassengerRepositoryImpl;
import smart.fly.payment.repository.impl.PaymentRepositoryImpl;
import smart.fly.seatclass.repository.impl.SeatClassRepositoryImpl;
import smart.fly.user.repository.impl.UserRepositoryImpl;
import smart.fly.util.PasswordUtil;

/**
 * Runs repositories against a real MySQL 8 database in a Testcontainers container. The schema is built by the
 * Flyway migrations in db/migration, and data is re-seeded from db/seed.sql before every test.
 *
 * Tests are not wrapped in a rolled-back transaction because some repositories open raw DataSource
 * connections, which would not see uncommitted seed data; re-seeding keeps each test isolated instead.
 * All repositories share one Spring context and one container (started once per JVM, stopped by Ryuk).
 */
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.sql.init.mode=never" })
@Import({ PasswordUtil.class, AirlineRepositoryImpl.class, AirportRepositoryImpl.class, BookingRepositoryImpl.class,
        BookingSearchRepository.class, BusinessOwnerRepositoryImpl.class, FeedbackRepositoryImpl.class,
        AddFlightRepository.class, FlightRepositoryImpl.class, FlightSearchRepository.class,
        FlightSeatsRepositoryImpl.class, FlightManagerRepositoryImpl.class, PassengerRepositoryImpl.class,
        PaymentRepositoryImpl.class, SeatClassRepositoryImpl.class, UserRepositoryImpl.class })
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Sql("/db/seed.sql")
public abstract class RepositoryTestBase {

    // Production SQL mixes table-name case (e.g. "Airline", "Feedback"), so match a case-insensitive server.
    // Data lives on tmpfs with durability turned off: it is throwaway, and re-seeding before every test stays fast.
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withCommand("--lower-case-table-names=1", "--skip-log-bin", "--innodb-flush-log-at-trx-commit=0")
            .withTmpFs(Map.of("/var/lib/mysql", "rw"));

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    protected int count(String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }
}
