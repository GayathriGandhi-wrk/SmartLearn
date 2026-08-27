package com.student.performance.service.impl;

import com.student.performance.dto.AnalyticsDto;
import com.student.performance.entity.Badge;
import com.student.performance.entity.Reward;
import com.student.performance.entity.Student;
import com.student.performance.exception.ResourceNotFoundException;
import com.student.performance.repository.BadgeRepository;
import com.student.performance.repository.RewardRepository;
import com.student.performance.repository.StudentRepository;
import com.student.performance.service.GamificationService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class GamificationServiceImpl implements GamificationService {

    private static final int XP_PER_LEVEL = 100;

    private final StudentRepository studentRepository;
    private final RewardRepository rewardRepository;
    private final BadgeRepository badgeRepository;

    public GamificationServiceImpl(StudentRepository studentRepository,
                                   RewardRepository rewardRepository,
                                   BadgeRepository badgeRepository) {
        this.studentRepository = studentRepository;
        this.rewardRepository = rewardRepository;
        this.badgeRepository = badgeRepository;
    }

    @Override
    public AnalyticsDto.GamificationResponse getGamification(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));

        int level = student.getLevel();
        int xp = student.getXpPoints();
        int xpForLevel = level * XP_PER_LEVEL;
        int xpToNext = Math.max(0, xpForLevel - xp);

        Set<Long> earnedBadgeIds = rewardRepository.findByStudentOrderByEarnedAtDesc(student).stream()
                .map(r -> r.getBadge() == null ? null : r.getBadge().getBadgeId())
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        List<AnalyticsDto.BadgeDto> badges = badgeRepository.findAll().stream()
                .sorted(Comparator.comparingInt(Badge::getThreshold))
                .map(b -> new AnalyticsDto.BadgeDto(
                        b.getBadgeId(), b.getBadgeName(), b.getBadgeIcon(),
                        b.getBadgeDescription(), earnedBadgeIds.contains(b.getBadgeId())))
                .collect(Collectors.toList());

        List<AnalyticsDto.LeaderboardEntry> leaderboard = getLeaderboard(20);

        return new AnalyticsDto.GamificationResponse(
                level, xp, xpToNext, student.getStreakDays(), student.getStreakDays(),
                badges, leaderboard);
    }

    @Override
    public List<AnalyticsDto.LeaderboardEntry> getLeaderboard(int limit) {
        List<Student> students = studentRepository.findTopByOrderByXpPointsDesc();
        return students.stream().limit(limit)
                .map(s -> new AnalyticsDto.LeaderboardEntry(
                        s.getStudentId(), s.getUser().getFullName(), s.getDepartment(),
                        s.getSemester(), s.getXpPoints(), s.getLevel(), s.getStreakDays(), 0))
                .collect(Collectors.toList());
    }
}
