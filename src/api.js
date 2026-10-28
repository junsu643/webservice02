let session = { accessToken: "", user: null };
export const getSession = () => session;
export function clearSession() {
  session = { accessToken: "", user: null };
}

const MESSAGES = {
  INVALID_CREDENTIALS: "이메일 또는 비밀번호가 올바르지 않습니다.",
  TOKEN_REQUIRED: "로그인이 필요합니다. 먼저 로그인하세요.",
  INVALID_TOKEN: "로그인 정보가 올바르지 않습니다. 다시 로그인하세요.",
  TOKEN_EXPIRED: "로그인이 만료되었습니다. 다시 로그인하세요.",
  FORBIDDEN: "작성자만 수정하거나 삭제할 수 있습니다.",
  NOT_FOUND: "해당 게시글이 없습니다.",
};


function authorizationHeaders() {
  return session.accessToken
      ? { Authorization: `Bearer ${session.accessToken}` }
      : {};
}

export async function request(path, options = {}) {
  let response;
  try {
    response = await fetch(`/api${path}`, {
      ...options,
      headers: {
        "Content-Type": "application/json",
        ...authorizationHeaders(),
        ...options.headers,
      },
    });
  } catch {
    throw new Error("서버에 연결할 수 없습니다. 백엔드가 실행 중인지 확인하세요.");
  }


  let data = null;
  if (response.status !== 204) {
    try {
      data = await response.json();
    } catch {
      data = null;
    }
  }

  if (!response.ok) {

    if (response.status === 401) clearSession();
    const code = data?.code;
    const error = new Error(
        MESSAGES[code] || data?.message || code || `HTTP ${response.status}`,
    );
    error.status = response.status;
    error.code = code;
    throw error;
  }
  return data;
}


export async function login(email, password) {
  const data = await request("/auth/login", {
    method: "POST",
    body: JSON.stringify({ email, password }),
  });
  session = { accessToken: data.accessToken, user: data.user };
  return data.user;
}


export function canEdit(post) {
  return !!session.user && session.user.id === post.authorId;
}

export const api = {
  list: () => request("/posts"),
  detail: (id) => request(`/posts/${id}`),
  create: (input) =>
      request("/posts", { method: "POST", body: JSON.stringify(input) }),
  update: (id, input) =>
      request(`/posts/${id}`, { method: "PUT", body: JSON.stringify(input) }),
  remove: (id) => request(`/posts/${id}`, { method: "DELETE" }),
  me: () => request("/me"),
};