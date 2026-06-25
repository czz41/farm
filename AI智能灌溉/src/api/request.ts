import axios, { AxiosError } from "axios";

const request = axios.create({
  baseURL: "/api",
  timeout: 30000,
  headers: { "Content-Type": "application/json" },
});

// 响应拦截：统一解包 BaseResponse，出错抛出
request.interceptors.response.use(
  (resp) => {
    const body = resp.data;
    // 后端统一返回 { code, data, message }
    if (body && typeof body === "object" && "code" in body) {
      if (body.code === 0 || body.code === 200) {
        return body.data;
      }
      const msg = body.message || `请求失败 code=${body.code}`;
      return Promise.reject(new Error(msg));
    }
    return body;
  },
  (error: AxiosError) => {
    const data = error.response?.data as { message?: string } | undefined;
    const msg = data?.message || error.message || "网络异常";
    return Promise.reject(new Error(msg));
  }
);

export default request;
