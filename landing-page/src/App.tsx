import { useState, useCallback } from 'react';
import { motion } from 'framer-motion';
import AuthModal from './components/AuthModal';

function useTheme() {
  const [isDark, setIsDark] = useState(
    () => document.documentElement.classList.contains('dark'),
  );

  const toggle = useCallback(() => {
    setIsDark((prev) => {
      const next = !prev;
      document.documentElement.classList.toggle('dark', next);
      localStorage.setItem('theme', next ? 'dark' : 'light');
      return next;
    });
  }, []);

  return { isDark, toggle };
}

export default function App() {
  const [authMode, setAuthMode] = useState<'signin' | 'signup'>('signin');
  const { isDark, toggle } = useTheme();

  return (
    <div className="min-h-screen flex flex-col">
      {/* Header */}
      <header className="absolute top-0 left-0 right-0 z-10">
        <nav className="mx-auto flex max-w-7xl items-center justify-between px-6 py-6">
          <a href="/" className="flex items-center gap-2 text-gradient text-xl font-bold">
            <img src="/Logo.png" alt="Continuum logo" className="h-8 w-8" />
            Continuum
          </a>

          {/* Theme toggle */}
          <button
            type="button"
            onClick={toggle}
            aria-label={isDark ? 'Switch to light mode' : 'Switch to dark mode'}
            className="flex h-10 w-10 items-center justify-center rounded-lg text-fg-muted transition-colors hover:text-fg hover:bg-overlay/10"
          >
            {isDark ? (
              <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <circle cx="12" cy="12" r="5" />
                <path strokeLinecap="round" d="M12 1v2M12 21v2M4.22 4.22l1.42 1.42M18.36 18.36l1.42 1.42M1 12h2M21 12h2M4.22 19.78l1.42-1.42M18.36 5.64l1.42-1.42" />
              </svg>
            ) : (
              <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
                <path strokeLinecap="round" strokeLinejoin="round" d="M21 12.79A9 9 0 1111.21 3 7 7 0 0021 12.79z" />
              </svg>
            )}
          </button>
        </nav>
      </header>

      {/* Main content - centered sign in */}
      <main className="flex-1 flex items-center justify-center px-4">
        <div className="w-full max-w-md">
          {/* Background decoration */}
          <div className="absolute inset-0 overflow-hidden pointer-events-none">
            <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-accent/10 rounded-full blur-3xl" />
            <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-purple/10 rounded-full blur-3xl" />
          </div>

          <motion.div
            className="relative z-10 text-center mb-8"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5 }}
          >
            <div className="mx-auto h-16 w-16 rounded-2xl bg-accent/10 flex items-center justify-center mb-6">
              <img src="/Logo.png" alt="Continuum" className="h-10 w-10" />
            </div>
            <h1 className="text-3xl font-bold mb-2">
              Welcome to <span className="text-gradient">Continuum</span>
            </h1>
            <p className="text-fg-muted">
              Sign in to access the visual workflow platform
            </p>
          </motion.div>

          <motion.div
            className="relative z-10 bg-card rounded-2xl border border-divider p-8 shadow-xl"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, delay: 0.1 }}
          >
            <AuthModal
              isOpen={true}
              onClose={() => {}}
              mode={authMode}
              onSwitchMode={setAuthMode}
              embedded={true}
            />
          </motion.div>

          <motion.p
            className="relative z-10 mt-6 text-center text-sm text-fg-muted"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ duration: 0.5, delay: 0.2 }}
          >
            By signing in, you agree to our{' '}
            <a href="#" className="text-accent hover:underline">Terms of Service</a>
            {' '}and{' '}
            <a href="#" className="text-accent hover:underline">Privacy Policy</a>
          </motion.p>
        </div>
      </main>

      {/* Footer */}
      <footer className="relative z-10 py-6 text-center text-sm text-fg-muted">
        <p>© {new Date().getFullYear()} Project Continuum · Apache 2.0 License</p>
      </footer>
    </div>
  );
}
