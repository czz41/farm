FROM swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/library/nginx:alpine
COPY dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
ENV TZ=Asia/Shanghai
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]