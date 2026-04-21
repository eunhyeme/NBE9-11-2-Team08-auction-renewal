-- k6 부하 테스트용 시드 데이터
-- prod 프로파일 기동 시 자동 실행 (ddl-auto: create 이후)
-- 비밀번호: Test1234! (BCrypt cost=10)

INSERT INTO users (email, role, password, nickname, created_at, address) VALUES
  ('seller@k6.test',  'ROLE_USER', '$2a$10$vxB9tcvPblBPMzZRGkm1KOA15ert4BVsVyWN4HMjVivuqV1sV26.O', 'K6Seller', NOW(), 'Seoul'),
  ('buyer1@k6.test',  'ROLE_USER', '$2a$10$vxB9tcvPblBPMzZRGkm1KOA15ert4BVsVyWN4HMjVivuqV1sV26.O', 'Buyer1',   NOW(), 'Seoul'),
  ('buyer2@k6.test',  'ROLE_USER', '$2a$10$vxB9tcvPblBPMzZRGkm1KOA15ert4BVsVyWN4HMjVivuqV1sV26.O', 'Buyer2',   NOW(), 'Seoul'),
  ('buyer3@k6.test',  'ROLE_USER', '$2a$10$vxB9tcvPblBPMzZRGkm1KOA15ert4BVsVyWN4HMjVivuqV1sV26.O', 'Buyer3',   NOW(), 'Seoul'),
  ('buyer4@k6.test',  'ROLE_USER', '$2a$10$vxB9tcvPblBPMzZRGkm1KOA15ert4BVsVyWN4HMjVivuqV1sV26.O', 'Buyer4',   NOW(), 'Seoul'),
  ('buyer5@k6.test',  'ROLE_USER', '$2a$10$vxB9tcvPblBPMzZRGkm1KOA15ert4BVsVyWN4HMjVivuqV1sV26.O', 'Buyer5',   NOW(), 'Seoul'),
  ('buyer6@k6.test',  'ROLE_USER', '$2a$10$vxB9tcvPblBPMzZRGkm1KOA15ert4BVsVyWN4HMjVivuqV1sV26.O', 'Buyer6',   NOW(), 'Seoul'),
  ('buyer7@k6.test',  'ROLE_USER', '$2a$10$vxB9tcvPblBPMzZRGkm1KOA15ert4BVsVyWN4HMjVivuqV1sV26.O', 'Buyer7',   NOW(), 'Seoul'),
  ('buyer8@k6.test',  'ROLE_USER', '$2a$10$vxB9tcvPblBPMzZRGkm1KOA15ert4BVsVyWN4HMjVivuqV1sV26.O', 'Buyer8',   NOW(), 'Seoul'),
  ('buyer9@k6.test',  'ROLE_USER', '$2a$10$vxB9tcvPblBPMzZRGkm1KOA15ert4BVsVyWN4HMjVivuqV1sV26.O', 'Buyer9',   NOW(), 'Seoul'),
  ('buyer10@k6.test', 'ROLE_USER', '$2a$10$vxB9tcvPblBPMzZRGkm1KOA15ert4BVsVyWN4HMjVivuqV1sV26.O', 'Buyer10',  NOW(), 'Seoul');

-- seller 권한 부여
INSERT INTO user_permissions (user_id, permissions)
  SELECT user_id, 'SELLER' FROM users WHERE email = 'seller@k6.test';

-- 경매 20개 생성 (seller_id는 seller@k6.test 기준 조회)
INSERT INTO auction (seller_id, title, description, start_price, current_price, bid_unit, status, started_at, ended_at, version)
SELECT
  u.user_id,
  CONCAT('K6 Test Auction ', nums.n),
  'load test',
  10000,
  10000,
  1000,
  'ONGOING',
  DATE_SUB(NOW(), INTERVAL 1 HOUR),
  DATE_ADD(NOW(), INTERVAL 48 HOUR),
  0
FROM users u
CROSS JOIN (
  SELECT 1 AS n UNION ALL SELECT 2  UNION ALL SELECT 3  UNION ALL SELECT 4  UNION ALL SELECT 5
  UNION ALL SELECT 6  UNION ALL SELECT 7  UNION ALL SELECT 8  UNION ALL SELECT 9  UNION ALL SELECT 10
  UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15
  UNION ALL SELECT 16 UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19 UNION ALL SELECT 20
) nums
WHERE u.email = 'seller@k6.test';
