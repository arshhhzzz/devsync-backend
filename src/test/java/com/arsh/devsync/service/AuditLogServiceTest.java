package com.arsh.devsync.service;

import com.arsh.devsync.entity.AuditLog;
import com.arsh.devsync.repository.AuditLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    @Test
    void log_shouldSaveAuditLog() {
        auditLogService.log(
                1L,
                "arsh@test.com",
                "TASK_CREATED",
                "TASK",
                10L
        );

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);

        verify(auditLogRepository).save(captor.capture());

        AuditLog savedLog = captor.getValue();

        assertEquals(1L, savedLog.getWorkspaceId());
        assertEquals("arsh@test.com", savedLog.getActorEmail());
        assertEquals("TASK_CREATED", savedLog.getAction());
        assertEquals("TASK", savedLog.getResourceType());
        assertEquals(10L, savedLog.getResourceId());
    }

    @Test
    void getAuditLogsByWorkspace_shouldReturnLogs() {
        AuditLog auditLog = new AuditLog(
                1L,
                "arsh@test.com",
                "TASK_CREATED",
                "TASK",
                10L
        );

        when(auditLogRepository.findByWorkspaceIdOrderByTimestampDesc(1L))
                .thenReturn(List.of(auditLog));

        List result = auditLogService.getAuditLogsByWorkspace(1L);

        assertEquals(1, result.size());
        verify(auditLogRepository).findByWorkspaceIdOrderByTimestampDesc(1L);
    }
}