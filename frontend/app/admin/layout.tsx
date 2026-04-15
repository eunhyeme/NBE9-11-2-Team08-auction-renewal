// TODO: ROLE_ADMIN 접근 제어
// - useAuthStore에서 role 확인
// - ROLE_USER → / 리다이렉트
export default function AdminLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div className="mx-auto max-w-5xl px-4 py-6">
      <h1 className="mb-6 text-xl font-bold">관리자 페이지</h1>
      {children}
    </div>
  );
}
