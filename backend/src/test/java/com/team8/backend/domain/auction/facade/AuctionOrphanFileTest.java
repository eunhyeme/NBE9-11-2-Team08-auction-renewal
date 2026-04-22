package com.team8.backend.domain.auction.facade;

import com.team8.backend.domain.auction.dto.ImageFile;
import com.team8.backend.domain.auction.dto.request.AuctionCreateRequest;
import com.team8.backend.domain.auction.dto.request.AuctionUpdateRequest;
import com.team8.backend.domain.auction.entity.Auction;
import com.team8.backend.domain.auction.entity.AuctionStatus;
import com.team8.backend.domain.auction.repository.AuctionRepository;
import com.team8.backend.domain.auction.service.AuctionDomainService;
import com.team8.backend.domain.auction.service.ImageService;
import com.team8.backend.domain.user.entity.User;
import com.team8.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@ActiveProfiles("test")
class AuctionOrphanFileTest {
    @Autowired private AuctionFacade auctionFacade;
    @Autowired private AuctionRepository auctionRepository;
    @Autowired private UserRepository userRepository;
    @MockitoSpyBean private AuctionDomainService auctionDomainService;
    @MockitoSpyBean private ImageService imageService;
    @Value("${file.upload-dir}") private String uploadDir;

    @BeforeEach
    void cleanUp() throws IOException {
        // 💡 1. DB 데이터 정리 (FK 제약 조건 때문에 Auction부터 지워야 합니다)
        auctionRepository.deleteAll();
        userRepository.deleteAll();

        // 2. 기존 파일 정리 로직 (이미 있는 코드)
        Path path = Path.of(uploadDir);
        if (Files.exists(path)) {
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }

    @Test
    @DisplayName("경매 생성 중 DB 예외 발생 시 고아 파일이 삭제되어야 한다")
    void orphanFileTest() throws Exception {
        // 0. User.java의 create(email, password, nickname, address) 사용
        User testUser = User.create("test@test.com", "pw123", "고수", "서울");
        User savedUser = userRepository.save(testUser);

        MockMultipartFile mockFile = new MockMultipartFile("image", "test.jpg", "image/jpeg", "content".getBytes());
        ImageFile imageFile = new ImageFile(mockFile.getInputStream(), "test.jpg", "image/jpeg", mockFile.getSize());
        AuctionCreateRequest request = new AuctionCreateRequest("상품", "설명", 1000L, 100, LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(7));

        // DB 저장 시 에러 유도
        doThrow(new RuntimeException("DB 저장 펑!")).when(auctionDomainService).save(any());

        assertThrows(RuntimeException.class, () ->
                auctionFacade.createAuction(savedUser.getUserId(), request, imageFile)
        );

        // 💡 3-1. 실제 저장 위치인 'images' 폴더를 확인합니다. [cite: 2026-04-22]
        File storageDir = new File(uploadDir + "/images");
        File[] files = storageDir.listFiles();

        // 특정 파일을 지칭하지 않고 폴더가 비어있는지 확인
        assertThat(files).isNullOrEmpty();
    }

    @Test
    @DisplayName("경매 수정 중 DB 예외 발생 시, 새로 업로드된 파일만 삭제되고 기존 데이터는 유지되어야 한다")
    void updateOrphanFileTest() throws Exception {
        // 0. 초기 세팅 (기존 경매 및 이미지 A 생성)
        User seller = userRepository.save(User.create("seller@test.com", "pw", "판매자", "서울"));
        String oldImageUrl = "/images/old-image.jpg";
        File oldFile = new File(uploadDir + oldImageUrl);
        oldFile.getParentFile().mkdirs();
        oldFile.createNewFile();

        Auction auction = Auction.builder()
                .seller(seller).title("기존제목").imageUrl(oldImageUrl)
                .startPrice(1000L).currentPrice(1000L).bidUnit(100).status(AuctionStatus.ONGOING)
                .startedAt(LocalDateTime.now()).endedAt(LocalDateTime.now().plusDays(7)).build();
        Auction savedAuction = auctionRepository.save(auction);

        // 1. 새 이미지 B 준비
        MockMultipartFile newMockFile = new MockMultipartFile("image", "new-image.jpg", "image/jpeg", "content".getBytes());
        ImageFile newImageFile = new ImageFile(newMockFile.getInputStream(), "new-image.jpg", "image/jpeg", newMockFile.getSize());
        AuctionUpdateRequest updateRequest = new AuctionUpdateRequest("수정제목", "설명", null, 2000L, 200, 7L);

        // 💡 2. 폭탄 설치: imageService.delete가 호출될 때 예외 발생!
        // 새 이미지(B)는 이미 업로드됐고, 이벤트도 발행된 '후'에 기존 이미지(A)를 지우려다 터지는 시나리오입니다.
        doThrow(new RuntimeException("기존 이미지 삭제 중 장애 발생!"))
                .when(imageService).delete(oldImageUrl);

        // 3. 실행
        assertThrows(RuntimeException.class, () ->
                auctionFacade.updateAuction(seller.getUserId(), savedAuction.getAuctionId(), updateRequest, newImageFile)
        );

        // 4. 검증
        File storageDir = new File(uploadDir + "/images");

        // ✅ 결과: 새 파일(B)은 롤백되어 지워지고, 기존 파일(A)은 여전히 남아있어야 함!
        assertThat(storageDir.listFiles()).extracting(File::getName).contains("old-image.jpg");
        assertThat(storageDir.listFiles()).extracting(File::getName).noneMatch(name -> name.contains("new-image.jpg"));
    }

    @Test
    @DisplayName("비즈니스 로직(엔티티 수정) 중 예외 발생 시, 새 파일은 삭제되고 기존 파일은 유지되어야 한다")
    void updateBusinessLogicFailureTest() throws Exception {
        // 0. 초기 세팅 (기존 유저, 경매, 이미지 A 생성)
        User seller = userRepository.save(User.create("seller@test.com", "pw", "판매자", "서울")); //
        String oldImageUrl = "/images/old-image.jpg";
        File oldFile = new File(uploadDir + oldImageUrl);
        oldFile.getParentFile().mkdirs();
        oldFile.createNewFile();

        Auction auction = Auction.builder()
                .seller(seller).title("기존제목").imageUrl(oldImageUrl)
                .startPrice(1000L).currentPrice(1000L).bidUnit(100).status(AuctionStatus.ONGOING)
                .startedAt(LocalDateTime.now()).endedAt(LocalDateTime.now().plusDays(7)).build(); //
        Auction savedAuction = auctionRepository.save(auction);

        // 1. 새 이미지 B 및 수정 요청 DTO 준비
        MockMultipartFile newMockFile = new MockMultipartFile("image", "new-image.jpg", "image/jpeg", "content".getBytes());
        ImageFile newImageFile = new ImageFile(newMockFile.getInputStream(), "new-image.jpg", "image/jpeg", newMockFile.getSize());
        AuctionUpdateRequest updateRequest = new AuctionUpdateRequest("수정제목", "설명", null, 2000L, 200, 7L);

        // 💡 2. 진짜 엔티티를 '스파이'로 감싸서 특정 메서드만 폭파시킵니다. [cite: 2026-04-22]
        // 진짜 DB에서 가져온 auction 객체를 spy로 만듭니다.
        Auction spyAuction = Mockito.spy(savedAuction);

        // 💡 폭탄 설치: spyAuction.update()가 호출될 때 RuntimeException을 던지게 합니다.
        org.mockito.Mockito.doThrow(new RuntimeException("엔티티 수정 중 예기치 못한 에러!"))
                .when(spyAuction).update(ArgumentMatchers.any());

        // 💡 도메인 서비스가 '진짜' 대신 이 '스파이'를 반환하도록 조작합니다.
        org.mockito.Mockito.when(auctionDomainService.getById(savedAuction.getAuctionId()))
                .thenReturn(spyAuction);

        // 3. 실행
        assertThrows(RuntimeException.class, () -> {
            auctionFacade.updateAuction(seller.getUserId(), savedAuction.getAuctionId(), updateRequest, newImageFile);
        });

        // 4. 검증
        File storageDir = new File(uploadDir + "/images");

        // ✅ 결과 확인:
        // - 기존 파일(A)인 'old-image.jpg'는 살아남아야 함 (update가 터져서 delete가 호출 안 됨)
        // - 새 파일(B)은 롤백 핸들러에 의해 지워져야 함 [cite: 2026-04-22]
        assertThat(storageDir.listFiles()).extracting(File::getName).contains("old-image.jpg");
        assertThat(storageDir.listFiles()).extracting(File::getName).noneMatch(name -> name.contains("new-image.jpg"));
    }
}