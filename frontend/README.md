# Spring Boot REST API Frontend

React TypeScript frontend application with OIDC authentication for the Spring Boot REST API.

## Features

- React 18 with TypeScript
- Vite for fast development and building
- OIDC authentication with PKCE flow
- Axios for HTTP client
- Path aliases for clean imports
- ESLint configuration

## Project Structure

```
src/
├── components/          # React components
│   ├── auth/           # Authentication components
│   └── common/         # Common/shared components
├── services/           # API and authentication services
├── types/              # TypeScript type definitions
├── utils/              # Utility functions
├── config/             # Configuration files
└── assets/             # Static assets
```

## Getting Started

1. Install dependencies:
   ```bash
   npm install
   ```

2. Copy environment variables:
   ```bash
   cp .env.example .env
   ```

3. Update environment variables in `.env` with your OIDC provider settings

4. Start development server:
   ```bash
   npm run dev
   ```

## Available Scripts

- `npm run dev` - Start development server
- `npm run build` - Build for production
- `npm run lint` - Run ESLint
- `npm run lint:fix` - Fix ESLint issues
- `npm run preview` - Preview production build
- `npm run type-check` - Run TypeScript type checking

## Environment Variables

- `VITE_API_BASE_URL` - Backend API base URL (default: http://localhost:8080)
- `VITE_OIDC_AUTHORITY` - OIDC provider authority URL
- `VITE_OIDC_CLIENT_ID` - OIDC client ID

## Development

The application uses path aliases for cleaner imports:

- `@/*` - src directory
- `@/components/*` - components directory
- `@/services/*` - services directory
- `@/types/*` - types directory
- `@/utils/*` - utils directory
- `@/config/*` - config directory