# Continuum Platform Landing Page

A simple, focused sign-in page for the Continuum Platform with SSO authentication support.

## Features

- 🔐 **SSO Authentication** - Support for Google, GitHub, Microsoft, and LinkedIn OAuth
- 🌓 **Dark/Light Mode** - Automatic theme detection with manual toggle
- 📱 **Fully Responsive** - Optimized for all screen sizes
- ⚡ **Fast & Lightweight** - Minimal dependencies, quick load times
- 🎨 **Continuum Branding** - Consistent with the platform's visual identity

## Tech Stack

- **React 18** - UI library
- **TypeScript** - Type safety
- **Tailwind CSS** - Utility-first CSS framework
- **Framer Motion** - Smooth animations
- **Vite** - Build tool and dev server

## Getting Started

### Prerequisites

- Node.js 18+ 
- npm or yarn

### Installation

```bash
# Navigate to the landing-page directory
cd continuum-platform-core/landing-page

# Install dependencies
npm install

# Start development server
npm run dev
```

The development server will start at `http://localhost:3000`.

### Building for Production

```bash
# Create production build
npm run build

# Preview production build locally
npm run preview
```

## Project Structure

```
landing-page/
├── public/
│   └── Logo.png              # Continuum logo
├── src/
│   ├── components/
│   │   └── AuthModal.tsx     # SSO authentication component
│   ├── App.tsx               # Main app component
│   ├── index.css             # Global styles & theme
│   └── main.tsx              # Entry point
├── index.html                # HTML template
├── package.json
├── tailwind.config.js
├── tsconfig.json
└── vite.config.ts
```

## SSO Configuration

The landing page includes SSO buttons for:
- Google
- GitHub  
- Microsoft
- LinkedIn

### Backend Integration

To enable SSO in production, configure the OAuth providers in your backend. The landing page will redirect to:

```
/api/auth/google    - Google OAuth
/api/auth/github    - GitHub OAuth
/api/auth/microsoft - Microsoft OAuth
/api/auth/linkedin  - LinkedIn OAuth
```

Your backend should handle these routes and implement the OAuth flow.

## Theming

The landing page uses CSS custom properties for theming, matching the Continuum brand colors.

### Light Theme
```css
:root {
  --c-bg: 245 245 245;
  --c-accent: 112 86 151;
  /* ... */
}
```

### Dark Theme
```css
.dark {
  --c-bg: 41 45 62;
  --c-accent: 196 168 255;
  /* ... */
}
```

## Adding New SSO Providers

Edit `src/components/AuthModal.tsx` and add a new entry to `SSO_PROVIDERS`:

```typescript
{
  id: 'provider-id',
  name: 'Provider Name',
  icon: <YourIcon />,
  color: 'hover:bg-color-50 dark:hover:bg-color-900/10',
}
```

## License

Apache 2.0 - See the main repository LICENSE file.
