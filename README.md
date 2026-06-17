# CodeGuardian AI — Automated Pull Request Review

An AI-powered code review system that automatically analyzes PRs using multiple specialized AI agents.

## Features
- Multi-agent AI review (Security + Architecture)
- Groq LLM integration (llama-3.1-8b-instant)
- GitHub webhook integration
- Kafka async processing
- RAG-powered codebase context

## Tech Stack
- Java 21, Spring Boot 3
- PostgreSQL, Kafka, Qdrant, Redis
- Docker Compose orchestration
- React dashboard

## Quick Start
`ash
docker compose up --build
`

## API Endpoints
- POST /api/webhooks/github - GitHub webhook receiver
- GET /actuator/health - Health check