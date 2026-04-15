/**
 * Access token을 메모리에 저장한다.
 * localStorage/sessionStorage 대신 모듈 변수를 사용해 XSS 탈취를 방어한다.
 * 단, 페이지 새로고침 시 토큰이 사라지므로 AuthProvider에서 /api/users/refresh로 복원한다.
 */
let _accessToken: string | null = null;

export const tokenManager = {
  get: () => _accessToken,
  set: (token: string) => {
    _accessToken = token;
  },
  clear: () => {
    _accessToken = null;
  },
};
