import http from 'k6/http';
import { check } from 'k6';

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export function login(email, password) {
  const res = http.post(
    `${BASE_URL}/api/users/login`,
    JSON.stringify({ email, password }),
    { headers: { 'Content-Type': 'application/json' }, tags: { name: 'login' } }
  );
  check(res, { 'login: status 200': (r) => r.status === 200 });
  return res.json('result.accessToken');
}

export function authHeaders(token) {
  return {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`,
    },
  };
}
