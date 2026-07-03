COMPOSE_FILE=infra/docker-compose.yml
BACKEND_DIR=services/medical-platform-service
BACKEND_PROFILE?=dev

.PHONY: up up-nc up-detached up-detached-nc down backend backend-dev backend-prod

up:
	docker compose -f $(COMPOSE_FILE) down -v
	docker compose -f $(COMPOSE_FILE) up --build

up-nc:
	docker compose -f $(COMPOSE_FILE) down -v
	docker compose -f $(COMPOSE_FILE) build --no-cache
	docker compose -f $(COMPOSE_FILE) up

up-detached:
	docker compose -f $(COMPOSE_FILE) down -v
	docker compose -f $(COMPOSE_FILE) up -d --build

up-detached-nc:
	docker compose -f $(COMPOSE_FILE) down -v
	docker compose -f $(COMPOSE_FILE) build --no-cache
	docker compose -f $(COMPOSE_FILE) up -d

down:
	docker compose -f $(COMPOSE_FILE) down -v

backend:
	cd $(BACKEND_DIR) && SPRING_PROFILES_ACTIVE=$(BACKEND_PROFILE) ./gradlew bootRun

backend-dev:
	cd $(BACKEND_DIR) && SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun

backend-prod:
	cd $(BACKEND_DIR) && SPRING_PROFILES_ACTIVE=prod ./gradlew bootRun