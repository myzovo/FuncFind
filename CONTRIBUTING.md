# Contributing to FuncFind

Thanks for your interest in contributing! Here's how you can help.

## Getting Started

1. Fork the repository
2. Clone your fork: `git clone https://github.com/YOUR_USERNAME/FuncFind.git`
3. Create a branch: `git checkout -b feature/your-feature`

## Development Setup

### Prerequisites

- **Java 17+** and Maven (for backend)
- **Node.js 18+** (for crawler & demo)
- **Playwright** browsers: `npx playwright install chromium`

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

The API will be available at `http://localhost:8080`.

### Crawler

```bash
cd crawler
npm install
npm start -- --url https://example.com
```

The control panel will be available at `http://localhost:3456`.

### Demo Site

```bash
cd demo
node server.js
```

Then open `http://localhost:4567`.

## Code Style

- **Java**: Follow standard Spring Boot conventions
- **JavaScript**: Use ES modules, prefer `const` over `let`
- **CSS**: Use CSS custom properties for theming
- **Commits**: Write clear, concise commit messages in English or Chinese

## Pull Request Process

1. Ensure your code builds and runs without errors
2. Update documentation if needed
3. Keep PRs focused — one feature/fix per PR
4. Write a clear description of what & why

## Reporting Issues

Open an issue with:

- Expected behavior vs actual behavior
- Steps to reproduce
- Environment info (OS, Java/Node version)

## License

By contributing, you agree that your contributions will be licensed under the MIT License.
