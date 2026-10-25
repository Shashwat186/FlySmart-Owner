package smart.fly.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
 * Runs repositories against an in-memory H2 database in MySQL mode, seeded from db/seed.sql before every test.
 *
 * Tests are not wrapped in a rolled-back transaction because some repositories open raw DataSource
 * connections, which would not see uncommitted seed data; re-seeding keeps each test isolated instead.
 * All repositories share one Spring context so it is only started once.
 */
@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:repository-tests;MODE=MySQL;NON_KEYWORDS=USER;DATABASE_TO_LOWER=TRUE;IGNORECASE=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:db/schema.sql" })
@Import({ PasswordUtil.class, AirlineRepositoryImpl.class, AirportRepositoryImpl.class, BookingRepositoryImpl.class,
        BookingSearchRepository.class, BusinessOwnerRepositoryImpl.class, FeedbackRepositoryImpl.class,
        AddFlightRepository.class, FlightRepositoryImpl.class, FlightSearchRepository.class,
        FlightSeatsRepositoryImpl.class, FlightManagerRepositoryImpl.class, PassengerRepositoryImpl.class,
        PaymentRepositoryImpl.class, SeatClassRepositoryImpl.class, UserRepositoryImpl.class })
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Sql("/db/seed.sql")
public abstract class RepositoryTestBase {

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    protected int count(String table) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }
}
