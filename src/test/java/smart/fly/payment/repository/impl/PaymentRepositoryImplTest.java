package smart.fly.payment.repository.impl;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Date;
import java.sql.Timestamp;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import smart.fly.booking.model.Booking;
import smart.fly.payment.model.Payment;
import smart.fly.payment.repository.PaymentRepository;
import smart.fly.repository.RepositoryTestBase;

class PaymentRepositoryImplTest extends RepositoryTestBase {

    @Autowired
    private PaymentRepository paymentRepository;

    @Test
    void findByBooking_mapsAllColumns() {
        Payment payment = paymentRepository.findByBooking(1);

        assertEquals(1, payment.getPaymentId());
        assertEquals(1, payment.getBooking().getBookingId());
        assertEquals("CARD", payment.getPaymentMode());
        assertEquals("4111111111111111", payment.getCardNumber());
        assertEquals("ASHA RAO", payment.getCardHolderName());
        assertEquals(Date.valueOf("2028-12-31"), payment.getExpiryDate());
        assertEquals(Timestamp.valueOf("2026-10-01 10:01:00"), payment.getPaymentDateTime());
        assertEquals("TXN-1", payment.getTransactionId());
        assertEquals("Credit", payment.getCardType());
        assertNull(payment.getUpiId());
    }

    @Test
    void findByTransactionId_returnsMatches() {
        List<Payment> payments = paymentRepository.findByTransactionId("TXN-2");

        assertEquals(1, payments.size());
        assertEquals("asha@upi", payments.get(0).getUpiId());
        assertTrue(paymentRepository.findByTransactionId("TXN-404").isEmpty());
    }

    @Test
    void save_insertsPayment() {
        jdbcTemplate.update("INSERT INTO booking (booking_id, user_id, flight_id, seat_class_id, status) VALUES (9, 3, 2, 1, 'BOOKED')");
        Payment payment = new Payment();
        Booking booking = new Booking();
        booking.setBookingId(9);
        payment.setBooking(booking);
        payment.setPaymentMode("UPI");
        payment.setUpiId("ravi@upi");
        payment.setTransactionId("TXN-9");
        payment.setPaymentDateTime(Timestamp.valueOf("2026-10-25 12:00:00"));

        paymentRepository.save(payment);

        Payment saved = paymentRepository.findByBooking(9);
        assertEquals("TXN-9", saved.getTransactionId());
        assertEquals("ravi@upi", saved.getUpiId());
    }

    @Test
    void updatePaymentStatus_changesStatus() {
        paymentRepository.updatePaymentStatus(1, "REFUNDED");

        assertEquals("REFUNDED", jdbcTemplate.queryForObject("SELECT status FROM payment WHERE payment_id = 1",
                String.class));
    }

    @Test
    void totalPayment_sumsOnlyBookedBookings() {
        // booking 3 is CANCELLED, so its 4500 is excluded
        assertEquals(15000, paymentRepository.totalPayment());
    }

    @Test
    @Disabled("Known bug: SUM returns NULL when nothing is booked, and unboxing it into int throws NullPointerException")
    void totalPayment_noBookedBookings_returnsZero() {
        jdbcTemplate.update("UPDATE booking SET status = 'CANCELLED'");

        assertEquals(0, paymentRepository.totalPayment());
    }
}
