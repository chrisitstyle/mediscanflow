FROM node:24-alpine3.23 AS deps

WORKDIR /app

ENV NEXT_TELEMETRY_DISABLED=1

COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./

RUN corepack enable && pnpm install --frozen-lockfile


FROM node:24-alpine3.23 AS builder

WORKDIR /app

ARG NEXT_PUBLIC_API_BASE_URL=/api/backend
ARG BACKEND_INTERNAL_URL=http://localhost:8080/api
ARG NEXT_PUBLIC_KEYCLOAK_URL=http://localhost:8081
ARG NEXT_PUBLIC_KEYCLOAK_REALM=mediscanflow
ARG NEXT_PUBLIC_KEYCLOAK_CLIENT_ID=mediscanflow-web

ENV NEXT_PUBLIC_API_BASE_URL=${NEXT_PUBLIC_API_BASE_URL}
ENV BACKEND_INTERNAL_URL=${BACKEND_INTERNAL_URL}
ENV NEXT_PUBLIC_KEYCLOAK_URL=${NEXT_PUBLIC_KEYCLOAK_URL}
ENV NEXT_PUBLIC_KEYCLOAK_REALM=${NEXT_PUBLIC_KEYCLOAK_REALM}
ENV NEXT_PUBLIC_KEYCLOAK_CLIENT_ID=${NEXT_PUBLIC_KEYCLOAK_CLIENT_ID}
ENV NEXT_TELEMETRY_DISABLED=1

COPY --from=deps /app/node_modules ./node_modules
COPY . .

RUN corepack enable && pnpm build


FROM node:24-alpine3.23 AS runner

WORKDIR /app

ARG NEXT_PUBLIC_API_BASE_URL=/api/backend
ARG BACKEND_INTERNAL_URL=http://localhost:8080/api
ARG NEXT_PUBLIC_KEYCLOAK_URL=http://localhost:8081
ARG NEXT_PUBLIC_KEYCLOAK_REALM=mediscanflow
ARG NEXT_PUBLIC_KEYCLOAK_CLIENT_ID=mediscanflow-web

ENV NODE_ENV=production
ENV PORT=3000
ENV HOSTNAME=0.0.0.0
ENV NEXT_TELEMETRY_DISABLED=1

ENV NEXT_PUBLIC_API_BASE_URL=${NEXT_PUBLIC_API_BASE_URL}
ENV BACKEND_INTERNAL_URL=${BACKEND_INTERNAL_URL}
ENV NEXT_PUBLIC_KEYCLOAK_URL=${NEXT_PUBLIC_KEYCLOAK_URL}
ENV NEXT_PUBLIC_KEYCLOAK_REALM=${NEXT_PUBLIC_KEYCLOAK_REALM}
ENV NEXT_PUBLIC_KEYCLOAK_CLIENT_ID=${NEXT_PUBLIC_KEYCLOAK_CLIENT_ID}

RUN addgroup -S nextjs-group -g 1001 \
    && adduser -S nextjs-user -u 1001 -G nextjs-group

COPY --from=builder --chown=nextjs-user:nextjs-group /app/public ./public
COPY --from=builder --chown=nextjs-user:nextjs-group /app/.next/standalone ./
COPY --from=builder --chown=nextjs-user:nextjs-group /app/.next/static ./.next/static

USER nextjs-user

EXPOSE 3000

CMD ["node", "server.js"]