package com.example.extracttool.service;

import com.example.extracttool.model.dto.ExtractionJobDtos;
import com.example.extracttool.model.entity.UserAccount;
import java.util.List;

public interface ExtractionJobService {
    ExtractionJobDtos.Snapshot submit(UserAccount user, ExtractionJobDtos.Submit request);
    ExtractionJobDtos.Snapshot get(UserAccount user, String id);
    ExtractionJobDtos.Snapshot latest(UserAccount user, Long workspaceId);
    ExtractionJobDtos.JobPage list(UserAccount user, Long workspaceId, int page);
    void clearFinished(UserAccount user, Long workspaceId);
}
