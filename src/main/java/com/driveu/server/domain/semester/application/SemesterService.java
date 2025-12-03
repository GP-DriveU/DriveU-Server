package com.driveu.server.domain.semester.application;

import com.driveu.server.domain.directory.application.DirectoryService;
import com.driveu.server.domain.directory.dto.response.DirectoryTreeResponse;
import com.driveu.server.domain.resource.application.ResourceService;
import com.driveu.server.domain.semester.dao.SemesterRepository;
import com.driveu.server.domain.semester.dao.UserSemesterRepository;
import com.driveu.server.domain.semester.domain.Semester;
import com.driveu.server.domain.semester.domain.Term;
import com.driveu.server.domain.semester.domain.UserSemester;
import com.driveu.server.domain.semester.dto.request.UserSemesterRequest;
import com.driveu.server.domain.semester.dto.response.UserSemesterResponse;
import com.driveu.server.domain.user.domain.User;
import com.driveu.server.domain.user.dto.response.MainPageResponse;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SemesterService {

    private final SemesterRepository semesterRepository;
    private final UserSemesterRepository userSemesterRepository;
    private final DirectoryService directoryService;
    private final ResourceService resourceService;
    private final UserSemesterQueryService userSemesterQueryService;

    // user 최초 로그인 시 자동으로 생성되는 UserSemester
    @Transactional
    public UserSemester createUserSemesterFromNow(User user) {
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        Term term = Term.fromMonth(now.getMonthValue());

        // 현재 학기 Semester 가 존재하면 가져오고, 존재하지 않는다면 새롭게 생성
        Semester semester = semesterRepository.findByYearAndTerm(year, term)
                .orElseGet(() -> semesterRepository.save(Semester.of(year, term)));

        UserSemester userSemester = UserSemester.of(user, semester, true);
        UserSemester savedUserSemester = userSemesterRepository.save(userSemester);

        // semester 의 디폴트 디렉토리 생성
        directoryService.createDefaultDirectories(savedUserSemester);

        return savedUserSemester;
    }

    @Transactional
    public UserSemesterResponse createUserSemester(User user, UserSemesterRequest request) {
        Term term = Term.valueOf(request.getTerm().toUpperCase());

        Semester semester = semesterRepository.findByYearAndTerm(request.getYear(), term)
                .orElseGet(() -> semesterRepository.save(Semester.of(request.getYear(), term)));

        if (userSemesterRepository.findByUserAndSemesterAndIsDeletedFalse(user, semester).isPresent()) {
            throw new IllegalStateException("Cannot create to the same semester");
        }

        Optional<UserSemester> currentOpt = userSemesterRepository.findByUserAndIsCurrentTrue(user);

        boolean isCurrent = false;
        if (currentOpt.isEmpty()) {
            isCurrent = true;
        } else {
            UserSemester current = currentOpt.get();
            // 원래 current 였던 것이 더 과거라면
            if (!current.getSemester().isAfter(semester)) {
                current.setCurrent(false); // 기존 학기 비활성화
                isCurrent = true;
            }
        }

        UserSemester userSemester = UserSemester.of(user, semester, isCurrent);
        UserSemester savedUserSemester = userSemesterRepository.save(userSemester);

        // semester 의 디폴트 디렉토리 생성
        directoryService.createDefaultDirectories(savedUserSemester);

        return UserSemesterResponse.from(savedUserSemester);
    }

    @Transactional
    public UserSemesterResponse updateUserSemester(User user, Long userSemesterId, UserSemesterRequest request) {
        Term term = Term.valueOf(request.getTerm().toUpperCase());

        // 변경 대상 학기 조회 또는 생성
        Semester newSemester = semesterRepository.findByYearAndTerm(request.getYear(), term)
                .orElseGet(() -> semesterRepository.save(Semester.of(request.getYear(), term)));

        UserSemester userSemester = userSemesterQueryService.getUserSemester(userSemesterId);

        // 같은 학기로 업데이트 요청을 하였을때
        if (newSemester.getId().equals(userSemester.getSemester().getId())) {
            throw new IllegalStateException("Cannot update to the same semester");
        }

        Optional<UserSemester> currentOpt = userSemesterRepository.findByUserAndIsCurrentTrue(user);
        // update 하려는 userSemester가 isCurrent = true 라면,
        // 해당 user 의 모든 userSemester를 돌면서 가장 최신꺼를 찾기
        // update 하려는 내용과 비교해서 isCurrent 인거 둘 중에 결정하기

        boolean isCurrent = false;
        if (currentOpt.isEmpty()) {
            isCurrent = true;
        } else {
            UserSemester current = currentOpt.get();

            // 현재 학기의 내용을 수정하려는 경우
            if (current.getId().equals(userSemesterId)) {
                // 수정 결과가 더 최신이라면
                if (!current.getSemester().isAfter(newSemester)) {
                    current.setCurrent(false);
                    isCurrent = true;
                }
                // 수정 결과가 더 과거라면, 현재학기 중에 최신학기를 찾음
                else {
                    // 수정 대상 userSemester 를 제외하고, 해당 유저의 최신 학기를 찾음
                    List<UserSemester> latestYearSemesters = userSemesterRepository.findAllByUserAndLatestYear(user)
                            .stream()
                            .filter(us -> !us.getId().equals(userSemester.getId())) // 수정 대상 제외
                            .toList();

                    if (latestYearSemesters.isEmpty()) {
                        System.out.println("latestYearSemesters is empty");
                    }
                    for (UserSemester us : latestYearSemesters) {
                        System.out.println(us.getSemester().getYear() + " " + us.getSemester().getTerm());
                    }

                    // 기존 최신 중에서 term 기준으로 가장 최신 학기 찾기
                    Optional<UserSemester> latestFromOthers = latestYearSemesters.stream()
                            .max(Comparator.comparing(us -> us.getSemester().getTerm().ordinal()));

                    if (latestFromOthers.isEmpty()) {
                        System.out.println("latestFromOthers is empty");
                    } else {
                        System.out.println("latestFromOthers: " + latestFromOthers.get().getSemester().getYear() + " "
                                + latestFromOthers.get().getSemester().getTerm());
                    }

                    System.out.println("newSemester: " + newSemester.getYear() + " " + newSemester.getTerm());
                    // 수정 대상과 기존 최신 중 누가 더 최신인지 비교
                    if (latestFromOthers.isEmpty() || !latestFromOthers.get().getSemester().isAfter(newSemester)) {
                        // 수정 대상이 최신이면 이걸 current로
                        isCurrent = true;
                    } else {
                        // 그렇지 않으면 가장 최신인 나머지 학기를 current로
                        latestFromOthers.get().setCurrent(true);
                        isCurrent = false;
                    }
                }
            }
            // 현재 학기가 아닌 다른 학기의 내용을 수정하려는 경우
            else {
                if (!current.getSemester().isAfter(newSemester)) {
                    current.setCurrent(false);
                    isCurrent = true;
                }
            }
        }

        userSemester.updateSemester(newSemester, isCurrent);
        return UserSemesterResponse.from(userSemester);
    }

    @Transactional
    public void deleteUserSemester(User user, Long userSemesterId) {
        UserSemester userSemester = userSemesterQueryService.getUserSemester(userSemesterId);

        boolean CurrentDelete = userSemester.isCurrent();

        System.out.println("Before delete: " + userSemester.getIsDeleted());
        userSemester.softDelete();
        System.out.println("After delete: " + userSemester.getDeletedAt());

        userSemesterRepository.save(userSemester); // 명시적으로 변경사항 반영

        System.out.println(userSemesterRepository.findById(userSemesterId).get().getIsDeleted());

        // 하위 디렉토리 전부 soft delete
        directoryService.softDeleteDirectoryListNotHierarchyByUserSemester(userSemester);

        if (CurrentDelete) {
            updateCurrentSemester(user);
        }
    }

    private void updateCurrentSemester(User user) {
        List<UserSemester> latestYearSemesters = userSemesterRepository.findAllByUserAndLatestYear(user);

        Optional<UserSemester> latest = latestYearSemesters.stream()
                .max(Comparator.comparing(us -> us.getSemester().getTerm().ordinal()));

        latest.ifPresent(us -> {
            us.setCurrent(true);
            userSemesterRepository.save(us); // 명시적으로 save
        });
    }

    public Optional<UserSemester> getCurrentUserSemester(User user) {
        return userSemesterRepository.findByUserAndIsCurrentTrue(user);
    }

    @Transactional(readOnly = true)
    public MainPageResponse getMainPage(Long semesterId, User user) {

        List<DirectoryTreeResponse> directoryTreeResponses = directoryService.getDirectoryTree(semesterId);

        return MainPageResponse.builder()
                .directories(directoryTreeResponses)
                .recentFiles(resourceService.getTop3RecentFiles(semesterId))
                .favoriteFiles(resourceService.getTop3FavoriteFiles(semesterId))
                .remainingStorage(user.getRemainingStorage())
                .build();
    }
}
