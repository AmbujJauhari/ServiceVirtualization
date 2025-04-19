import React from 'react';
import DarkModeToggle from '../common/DarkModeToggle';

interface MainLayoutProps {
  children: React.ReactNode;
  title?: string;
  subtitle?: string;
}

export const MainLayout: React.FC<MainLayoutProps> = ({ 
  children, 
  title, 
  subtitle 
}) => {
  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 flex flex-col text-gray-800 dark:text-gray-200 transition-colors duration-200">
      <div className="container mx-auto px-4 py-8 flex-grow">
        <div className="flex justify-end mb-4">
          <DarkModeToggle />
        </div>
        {(title || subtitle) && (
          <header className="mb-8">
            {title && <h1 className="text-3xl font-bold text-gray-800 dark:text-gray-100">{title}</h1>}
            {subtitle && <p className="text-gray-600 dark:text-gray-400 mt-2">{subtitle}</p>}
          </header>
        )}
        <main>
          {children}
        </main>
      </div>
      <footer className="bg-gray-800 dark:bg-gray-950 text-white py-4">
        <div className="container mx-auto px-4">
          <p className="text-center text-sm">
            Service Virtualization Platform © {new Date().getFullYear()} 
          </p>
        </div>
      </footer>
    </div>
  );
}; 