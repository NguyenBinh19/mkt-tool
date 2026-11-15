package com.example.extracttool.service;

import com.example.extracttool.config.FacebookProperties;
import com.example.extracttool.model.dto.ExtractResponseDTO;
import com.example.extracttool.model.dto.ExtractionJobDtos;
import com.example.extracttool.model.entity.ExtractionJob;
import com.example.extracttool.model.entity.UserAccount;
import com.example.extracttool.model.entity.Workspace;
import com.example.extracttool.repository.ExtractionJobRepository;
import com.example.extracttool.repository.UserAccountRepository;
import com.example.extracttool.repository.WorkspaceRepository;
import com.example.extracttool.service.impl.ExtractionJobServiceImpl;
import com.example.extracttool.validation.FacebookPostUrlValidator;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ExtractionJobServiceImplTest {
    @Test
    void jobListIsScopedAndPagedWithoutLoadingResults() {
        ExtractionJobRepository jobs = mock(ExtractionJobRepository.class);
        WorkspaceRepository workspaces = mock(WorkspaceRepository.class);
        UserAccount user = mock(UserAccount.class);
        when(user.getId()).thenReturn(7L);
        when(workspaces.findByIdAndOwnerId(8L, 7L)).thenReturn(Optional.of(mock(Workspace.class)));
        ExtractionJob job = new ExtractionJob(7L, 8L, "https://www.facebook.com/example/posts/pfbid123", 1, false);
        job.setResultJson("large result that must not be returned in the list");
        when(jobs.findByWorkspaceIdAndOwnerId(eq(8L), eq(7L), any(Pageable.class)))
                .thenReturn(new SliceImpl<>(List.of(job)));
        ExtractionJobServiceImpl service = new ExtractionJobServiceImpl(jobs, workspaces,
                mock(UserAccountRepository.class), mock(FacebookCommentService.class),
                mock(WorkspaceDataService.class), mock(FacebookPostUrlValidator.class),
                new FacebookProperties(), new ObjectMapper());
        try {
            var page = service.list(user, 8L, 0);
            assertEquals(job.getId(), page.items().getFirst().id());
            assertFalse(page.hasMore());
            var pageable = org.mockito.ArgumentCaptor.forClass(Pageable.class);
            verify(jobs).findByWorkspaceIdAndOwnerId(eq(8L), eq(7L), pageable.capture());
            assertEquals(20, pageable.getValue().getPageSize());
        } finally {
            service.stopWorkers();
        }
    }

    @Test
    void secondJobWaitsWithDefaultSingleTab() throws Exception {
        verifyQueueAtLimit(1);
    }

    @Test
    void configuredConcurrencyIsUsedByTheWorkerPool() throws Exception {
        verifyQueueAtLimit(3);
    }

    private void verifyQueueAtLimit(int concurrency) throws Exception {
        Map<String, ExtractionJob> records = new ConcurrentHashMap<>();
        ExtractionJobRepository jobs = mock(ExtractionJobRepository.class);
        WorkspaceRepository workspaces = mock(WorkspaceRepository.class);
        UserAccountRepository users = mock(UserAccountRepository.class);
        FacebookCommentService facebook = mock(FacebookCommentService.class);
        WorkspaceDataService workspaceData = mock(WorkspaceDataService.class);
        FacebookPostUrlValidator validator = mock(FacebookPostUrlValidator.class);
        UserAccount user = mock(UserAccount.class);
        when(user.getId()).thenReturn(7L);
        when(workspaces.findByIdAndOwnerId(8L, 7L)).thenReturn(Optional.of(mock(Workspace.class)));
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(jobs.save(any(ExtractionJob.class))).thenAnswer(invocation -> {
            ExtractionJob job = invocation.getArgument(0);
            records.put(job.getId(), job);
            return job;
        });
        when(jobs.findById(anyString())).thenAnswer(invocation -> Optional.ofNullable(records.get(invocation.getArgument(0))));
        when(jobs.findByIdAndOwnerId(anyString(), eq(7L))).thenAnswer(invocation -> Optional.ofNullable(records.get(invocation.getArgument(0))));

        CountDownLatch started = new CountDownLatch(concurrency);
        CountDownLatch release = new CountDownLatch(1);
        when(facebook.extractComments(any())).thenAnswer(invocation -> {
            started.countDown();
            if (!release.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("test timed out");
            return ExtractResponseDTO.builder().status("ok").comments(List.of()).build();
        });

        FacebookProperties properties = new FacebookProperties();
        properties.setMaxConcurrentTabs(concurrency);
        ExtractionJobServiceImpl service = new ExtractionJobServiceImpl(jobs, workspaces, users, facebook,
                workspaceData, validator, properties, new ObjectMapper());
        try {
            List<String> ids = new ArrayList<>();
            for (int index = 0; index <= concurrency; index++) {
                ids.add(service.submit(user, new ExtractionJobDtos.Submit(8L,
                        "https://www.facebook.com/example/posts/pfbid123", 1, false)).id());
            }
            assertTrue(started.await(3, TimeUnit.SECONDS));
            assertEquals(ExtractionJob.Status.QUEUED, service.get(user, ids.get(concurrency)).status());
            assertEquals(concurrency, ids.stream().filter(id -> service.get(user, id).status() == ExtractionJob.Status.RUNNING).count());
        } finally {
            release.countDown();
            service.stopWorkers();
        }
    }
}
