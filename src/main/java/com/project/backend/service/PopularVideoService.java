package com.project.backend.service;

import com.project.backend.dto.PopularVideoDTO;
import com.project.backend.dto.VideoResponse;
import com.project.backend.model.PopularVideo;
import com.project.backend.repository.PopularVideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PopularVideoService {

    private final PopularVideoRepository popularVideoRepository;
    private final VideoService videoService;

    /**
     * Get the top 3 popular videos from the latest ETL run
     */
    @Transactional(readOnly = true)
    public List<PopularVideoDTO> getTop3PopularVideos(Long currentUserId) {
        List<PopularVideoDTO> result = new ArrayList<>();

        PopularVideo latestResult = popularVideoRepository
                .findFirstByIsLatestTrueOrderByPipelineRunAtDesc()
                .orElse(null);

        if (latestResult == null) {
            return result; // No ETL results yet
        }

        // Add first place
        if (latestResult.getFirstPlaceVideo() != null) {
            VideoResponse videoResponse = videoService.getVideoById(
                    latestResult.getFirstPlaceVideo().getId(),
                    currentUserId
            );
            result.add(new PopularVideoDTO(
                    1,
                    videoResponse,
                    latestResult.getFirstPlaceScore(),
                    latestResult.getPipelineRunAt()
            ));
        }

        // Add second place
        if (latestResult.getSecondPlaceVideo() != null) {
            VideoResponse videoResponse = videoService.getVideoById(
                    latestResult.getSecondPlaceVideo().getId(),
                    currentUserId
            );
            result.add(new PopularVideoDTO(
                    2,
                    videoResponse,
                    latestResult.getSecondPlaceScore(),
                    latestResult.getPipelineRunAt()
            ));
        }

        // Add third place
        if (latestResult.getThirdPlaceVideo() != null) {
            VideoResponse videoResponse = videoService.getVideoById(
                    latestResult.getThirdPlaceVideo().getId(),
                    currentUserId
            );
            result.add(new PopularVideoDTO(
                    3,
                    videoResponse,
                    latestResult.getThirdPlaceScore(),
                    latestResult.getPipelineRunAt()
            ));
        }

        return result;
    }
}
