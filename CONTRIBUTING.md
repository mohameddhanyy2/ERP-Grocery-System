# Contributing

## Branching

- `main` — protected, production-ready code only
- `dev` — integration branch
- `feature/<module>-<short-desc>` — e.g. `feature/inventory-add-product`
- `fix/<short-desc>` — bug fixes

## Workflow

1. Pull latest `dev`
2. Create your feature branch from `dev`
3. Commit small, focused changes
4. Push and open a PR into `dev`
5. At least 1 reviewer approves before merge

## Commit messages

Follow conventional commits:
- `feat: add product CRUD endpoints`
- `fix: correct stock calculation on sale`
- `docs: update README`
- `refactor: extract auth filter`
- `test: add product service tests`

## Code style

**Backend (Java):**
- Follow JavaBean conventions for entities and DTOs (no-arg constructor, getters/setters)
- Use constructor injection (not `@Autowired` on fields)
- Validate input with `@Valid` + Bean Validation annotations
- Service layer holds business logic, controllers stay thin

**Frontend (React):**
- Functional components + hooks
- Keep API calls in `services/`
- Reusable UI in `components/`, route screens in `pages/`
