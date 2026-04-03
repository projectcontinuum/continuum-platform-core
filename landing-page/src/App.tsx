import { useState, useCallback, useEffect } from 'react';
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

function useMousePosition() {
  const [mousePosition, setMousePosition] = useState({ x: 0.5, y: 0.5 });

  useEffect(() => {
    const handleMouseMove = (e: MouseEvent) => {
      // Normalize mouse position to 0-1 range
      setMousePosition({
        x: e.clientX / window.innerWidth,
        y: e.clientY / window.innerHeight,
      });
    };

    window.addEventListener('mousemove', handleMouseMove);
    return () => window.removeEventListener('mousemove', handleMouseMove);
  }, []);

  return mousePosition;
}

export default function App() {
  const [authMode, setAuthMode] = useState<'signin' | 'signup'>('signin');
  const { isDark, toggle } = useTheme();
  const mousePosition = useMousePosition();

  // Calculate light position relative to center (for shadow effect)
  const lightOffsetX = (mousePosition.x - 0.5) * 100; // -50 to 50
  const lightOffsetY = (mousePosition.y - 0.5) * 100; // -50 to 50

  // Shadow offset (opposite direction of light) - increased multiplier for more visible effect
  const shadowX = -lightOffsetX * 0.5;
  const shadowY = -lightOffsetY * 0.5;

  // Glow position (same direction as light)
  const glowX = lightOffsetX * 0.6;
  const glowY = lightOffsetY * 0.6;

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
          {/* Static background ambient */}
          <div className="fixed inset-0 overflow-hidden pointer-events-none">
            <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-accent/5 rounded-full blur-3xl" />
            <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-purple/5 rounded-full blur-3xl" />
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

          {/* Sign-in card with dynamic lighting */}
          <motion.div
            className="relative z-10"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, delay: 0.1 }}
          >
            {/* Dynamic glow behind card based on light position */}
            <motion.div
              className="absolute -inset-4 rounded-3xl opacity-60"
              style={{
                background: `radial-gradient(circle at ${50 + glowX}% ${50 + glowY}%, rgb(var(--c-accent) / 0.3) 0%, rgb(var(--c-purple) / 0.15) 40%, transparent 70%)`,
                filter: 'blur(30px)',
              }}
              animate={{
                background: `radial-gradient(circle at ${50 + glowX}% ${50 + glowY}%, rgb(var(--c-accent) / 0.3) 0%, rgb(var(--c-purple) / 0.15) 40%, transparent 70%)`,
              }}
              transition={{
                type: 'tween',
                duration: 0.1,
                ease: 'linear',
              }}
            />

            {/* Card with dynamic shadow - multiple layers for depth, enhanced for light mode visibility */}
            <motion.div
              className="relative bg-card rounded-2xl border border-divider p-8"
              animate={{
                boxShadow: `
                  ${shadowX * 0.4}px ${shadowY * 0.4}px 10px -2px rgba(51, 56, 77, 0.12),
                  ${shadowX * 0.7}px ${shadowY * 0.7}px 25px -5px rgba(51, 56, 77, 0.18),
                  ${shadowX * 1.1}px ${shadowY * 1.1}px 50px -8px rgba(112, 86, 151, 0.4),
                  ${shadowX * 1.5}px ${shadowY * 1.5}px 80px -12px rgba(112, 86, 151, 0.25),
                  0px 30px 60px -15px rgba(51, 56, 77, 0.3)
                `,
              }}
              transition={{
                type: 'tween',
                duration: 0.15,
                ease: 'easeOut',
              }}
            >
              <AuthModal
                isOpen={true}
                onClose={() => {}}
                mode={authMode}
                onSwitchMode={setAuthMode}
                embedded={true}
              />
            </motion.div>
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
