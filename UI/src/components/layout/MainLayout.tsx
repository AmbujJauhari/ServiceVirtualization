import React from 'react';

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
    <div className="min-h-screen bg-gray-50 flex flex-col">
      <div className="container mx-auto px-4 py-8 flex-grow">
        {(title || subtitle) && (
          <header className="mb-8">
            {title && <h1 className="text-3xl font-bold text-gray-800">{title}</h1>}
            {subtitle && <p className="text-gray-600 mt-2">{subtitle}</p>}
          </header>
        )}
        <main>
          {children}
        </main>
      </div>
      <footer className="bg-gray-800 text-white py-4">
        <div className="container mx-auto px-4">
          <p className="text-center text-sm">
            Service Virtualization Platform © {new Date().getFullYear()} 
          </p>
        </div>
      </footer>
    </div>
  );
}; 