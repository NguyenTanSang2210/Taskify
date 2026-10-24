FROM node:22-alpine AS builder
WORKDIR /app

COPY package.json package-lock.json ./
# npm bỏ qua IM LẶNG các gói optional tải lỗi (ví dụ native binding của rolldown/vite) nên `npm ci`
# vẫn "thành công", layer hỏng bị Docker cache lại và mọi lần build sau đều lỗi ở `npm run build`.
# Nạp thử binding ngay tại bước này để layer hỏng không bao giờ được cache như một thành công.
RUN npm ci && node --input-type=module -e "await import('rolldown'); console.log('rolldown native binding: OK')"

COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
