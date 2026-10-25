package smart.fly.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import smart.fly.action.log.ActionLog;

@ExtendWith(MockitoExtension.class)
class LoggingServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private LoggingService loggingService;

    @Test
    void logAction_insertsRowWithCurrentTimestamp() {
        LocalDateTime before = LocalDateTime.now();

        loggingService.logAction(3, "Added flight", "AdminController");

        ArgumentCaptor<Timestamp> timestamp = ArgumentCaptor.forClass(Timestamp.class);
        verify(jdbcTemplate).update(startsWith("INSERT INTO action_log"), eq(3), eq("Added flight"),
                eq("AdminController"), timestamp.capture());
        LocalDateTime logged = timestamp.getValue().toLocalDateTime();
        assertFalse(logged.isBefore(before));
        assertFalse(logged.isAfter(LocalDateTime.now()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void getActionLogForOwner_queriesByUserAndMapsRows() throws Exception {
        ActionLog expected = new ActionLog();
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(3))).thenReturn(List.of(expected));

        List<ActionLog> result = loggingService.getActionLogForOwner(3);

        assertEquals(List.of(expected), result);
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<RowMapper<ActionLog>> mapper = ArgumentCaptor.forClass(RowMapper.class);
        verify(jdbcTemplate).query(sql.capture(), mapper.capture(), eq(3));
        assertTrue(sql.getValue().contains("ORDER BY timestamp DESC"));

        ResultSet rs = mock(ResultSet.class);
        Timestamp ts = Timestamp.valueOf("2026-10-25 09:30:00");
        when(rs.getInt("log_id")).thenReturn(11);
        when(rs.getInt("user_id")).thenReturn(3);
        when(rs.getString("action_performed")).thenReturn("Added flight");
        when(rs.getString("controller_name")).thenReturn("AdminController");
        when(rs.getTimestamp("timestamp")).thenReturn(ts);

        ActionLog mapped = mapper.getValue().mapRow(rs, 0);

        assertEquals(11, mapped.getLogId());
        assertEquals(3, mapped.getUserId());
        assertEquals("Added flight", mapped.getActionPerformed());
        assertEquals("AdminController", mapped.getControllerName());
        assertEquals(ts, mapped.getTimestamp());
    }
}
